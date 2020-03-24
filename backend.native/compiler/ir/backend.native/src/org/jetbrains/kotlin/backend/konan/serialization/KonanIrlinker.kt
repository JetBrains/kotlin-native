/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan .serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.isKonanStdlib
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class KonanIrLinker(
        private val currentModule: ModuleDescriptor,
        override val functionalInteraceFactory: IrAbstractFunctionFactory,
        logger: LoggingContext,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        private val forwardModuleDescriptor: ModuleDescriptor?,
        private val stubGenerator: DeclarationStubGenerator,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        exportedDependencies: List<ModuleDescriptor>
) : KotlinIrLinker(logger, builtIns, symbolTable, exportedDependencies, forwardModuleDescriptor) {

    override fun reader(moduleDescriptor: ModuleDescriptor, fileIndex: Int, idSigIndex: Int) =
            moduleDescriptor.konanLibrary!!.irDeclaration(idSigIndex, fileIndex)

    override fun readType(moduleDescriptor: ModuleDescriptor, fileIndex: Int, typeIndex: Int) =
            moduleDescriptor.konanLibrary!!.type(typeIndex, fileIndex)

    override fun readSignature(moduleDescriptor: ModuleDescriptor, fileIndex: Int, signatureIndex: Int) =
            moduleDescriptor.konanLibrary!!.signature(signatureIndex, fileIndex)

    override fun readString(moduleDescriptor: ModuleDescriptor, fileIndex: Int, stringIndex: Int) =
            moduleDescriptor.konanLibrary!!.string(stringIndex, fileIndex)

    override fun readBody(moduleDescriptor: ModuleDescriptor, fileIndex: Int, bodyIndex: Int) =
            moduleDescriptor.konanLibrary!!.body(bodyIndex, fileIndex)

    override fun readFile(moduleDescriptor: ModuleDescriptor, fileIndex: Int) =
            moduleDescriptor.konanLibrary!!.file(fileIndex)

    override fun readFileCount(moduleDescriptor: ModuleDescriptor) =
            moduleDescriptor.run { if (this === forwardModuleDescriptor || moduleDescriptor.isFromInteropLibrary()) 0 else konanLibrary!!.fileCount() }

    companion object {
        private val C_NAMES_NAME = Name.identifier("cnames")
        private val OBJC_NAMES_NAME = Name.identifier("objcnames")

        val FORWARD_DECLARATION_ORIGIN = object : IrDeclarationOriginImpl("FORWARD_DECLARATION_ORIGIN") {}

        const val offset = -2
    }

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isKonanStdlib()

    private val forwardDeclarationDeserializer = forwardModuleDescriptor?.let { KonanForwardDeclarationModuleDeserialier(it) }

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy): IrModuleDeserializer {
        if (moduleDescriptor === forwardModuleDescriptor) {
            return forwardDeclarationDeserializer ?: error("forward declaration deserializer expected")
        }

        if (moduleDescriptor.kotlinLibrary.isInteropLibrary()) {
            return KonanInteropModuleDeserializer(moduleDescriptor)
        }

        return KonanModuleDeserializer(moduleDescriptor, strategy)
    }

    private inner class KonanModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy):
        KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, strategy)

    private inner class KonanInteropModuleDeserializer(moduleDescriptor: ModuleDescriptor) : IrModuleDeserializer(moduleDescriptor) {
        init {
            assert(moduleDescriptor.kotlinLibrary.isInteropLibrary())
        }

        private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinder(moduleDescriptor)
        private fun IdSignature.isInteropSignature(): Boolean = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

        override fun contains(idSig: IdSignature): Boolean {

            if (idSig.isPublic) {
                if (idSig.isInteropSignature()) {
                    // TODO: add descriptor cache??
                    return descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null
                }
            }

            return false
        }

        private fun DeclarationDescriptor.isCEnumsOrCStruct(): Boolean = cenumsProvider.isCEnumOrCStruct(this)

        private val fileMap = mutableMapOf<PackageFragmentDescriptor, IrFile>()

        private fun getIrFile(packageFragment: PackageFragmentDescriptor): IrFile = fileMap.getOrPut(packageFragment) {
            IrFileImpl(NaiveSourceBasedFileEntryImpl(IrProviderForCEnumAndCStructStubs.cTypeDefinitionsFileName), packageFragment).also {
                moduleFragment.files.add(it)
            }
        }

        private fun resolveCEnumsOrStruct(descriptor: DeclarationDescriptor, idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            val file = getIrFile(descriptor.findPackage())
            return cenumsProvider.getDeclaration(descriptor, idSig, file, symbolKind).symbol
        }

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: error("Expecting descriptor for $idSig")

            if (descriptor.isCEnumsOrCStruct()) return resolveCEnumsOrStruct(descriptor, idSig, symbolKind)

            val symbolOwner = stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner

            return symbolOwner.symbol
        }

        override fun addModuleReachableTopLevel(idSig: IdSignature) {
            error("Is not allowed for Interop library reader (idSig = $idSig)")
        }

        override fun deserializeReachableDeclarations() {
            error("Is not allowed for Interop library reader")
        }

        override fun postProcess() {}

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns)
        override val moduleDependencies: Collection<IrModuleDeserializer> = listOfNotNull(forwardDeclarationDeserializer)
    }

    private inner class KonanForwardDeclarationModuleDeserialier(moduleDescriptor: ModuleDescriptor) : IrModuleDeserializer(moduleDescriptor) {
        init {
            assert(moduleDescriptor.isForwardDeclarationModule)
        }

        private val declaredDeclaration = mutableMapOf<IdSignature, IrClass>()
        private val packageToFileMap = mutableMapOf<FqName, IrFile>()

        private fun IdSignature.isForwardDeclarationSignature(): Boolean {
            if (isPublic) {
                return packageFqName().run {
                    startsWith(C_NAMES_NAME) || startsWith(OBJC_NAMES_NAME)
                }
            }

            return false
        }

        override fun contains(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

        private fun resolveDescriptor(idSig: IdSignature): ClassDescriptor =
            with(idSig as IdSignature.PublicSignature) {
                val classId = ClassId(packageFqn, declarationFqn, false)
                moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: error("No declaration found with $idSig")
            }

        private fun getIrFile(packageFragment: PackageFragmentDescriptor): IrFile {
            val fqn = packageFragment.fqName
            return packageToFileMap.getOrPut(packageFragment.fqName) {
                val fileSymbol = IrFileSymbolImpl(packageFragment)
                IrFileImpl(NaiveSourceBasedFileEntryImpl("forward declarations for $fqn"), fileSymbol).also {
                    moduleFragment.files.add(it)
                }
            }
        }

        private fun buildForwardDeclarationStub(idSig: IdSignature, descriptor: ClassDescriptor): IrClass {
            val packageDescriptor = descriptor.containingDeclaration as PackageFragmentDescriptor
            val irFile = getIrFile(packageDescriptor)

            val klass = symbolTable.declareClassFromLinker(descriptor, idSig) { s ->
                IrClassImpl(offset, offset, FORWARD_DECLARATION_ORIGIN, s)
            }

            klass.parent = irFile
            irFile.declarations.add(klass)

            return klass
        }

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            assert(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) { "Only class could be a Forward declaration $idSig (kind $symbolKind)" }
            val descriptor = resolveDescriptor(idSig)
            val actualModule = descriptor.module
            if (actualModule !== moduleDescriptor) {
                val moduleDeserializer = deserializersForModules[actualModule] ?: error("No module deserializer for $actualModule")
                moduleDeserializer.addModuleReachableTopLevel(idSig)
                return symbolTable.referenceClassFromLinker(descriptor, idSig)
            }

            return declaredDeclaration.getOrPut(idSig) { buildForwardDeclarationStub(idSig, descriptor) }.symbol
        }

        override fun addModuleReachableTopLevel(idSig: IdSignature) { error("Is not allowed for FWD reader (idSig = $idSig)") }

        override fun deserializeReachableDeclarations() { error("Is not allowed for FWD reader") }

        override fun postProcess() {}

        override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, builtIns)
        override val moduleDependencies: Collection<IrModuleDeserializer> = emptyList()
    }

    val modules: Map<String, IrModuleFragment>
        get() = mutableMapOf<String, IrModuleFragment>().apply {
            deserializersForModules
                    .filter { !it.key.isForwardDeclarationModule && it.value.moduleDescriptor !== currentModule }
                    .forEach { this.put(it.key.konanLibrary!!.libraryName, it.value.moduleFragment) }
        }
}