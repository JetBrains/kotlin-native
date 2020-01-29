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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.descriptors.isFromInteropLibrary
import org.jetbrains.kotlin.backend.konan.descriptors.konanLibrary
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrFileSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

class KonanIrLinker(
    private val currentModule: ModuleDescriptor,
    logger: LoggingContext,
    builtIns: IrBuiltIns,
    symbolTable: SymbolTable,
    private val forwardModuleDescriptor: ModuleDescriptor?,
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

    override fun handleNoModuleDeserializerFound(isSignature: IdSignature): DeserializationState<*> {
        return globalDeserializationState
    }

    companion object {
        private val C_NAMES_NAME = Name.identifier("cnames")
        private val OBJC_NAMES_NAME = Name.identifier("objcnames")
    }

    override fun isSpecialPlatformSignature(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

    private var forwardDeclarationDeserializer: KonanForwardDeclarationsModuleDeserializer? = null
    private val interopModuleDeserializers = mutableSetOf<KonanInteropModuleDeserializer>()

    override fun resolvePlatformDescriptor(idSig: IdSignature): DeclarationDescriptor? {
        if (!idSig.isForwardDeclarationSignature()) return null

        val fwdModule = forwardModuleDescriptor ?: error("Forward declaration module should not be null")

        return with(idSig as IdSignature.PublicSignature) {
            val classId = ClassId(packageFqn, classFqn, false)
            fwdModule.findClassAcrossModuleDependencies(classId)
        }
    }

    private fun IdSignature.isInteropSignature(): Boolean = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

    private fun IdSignature.isForwardDeclarationSignature(): Boolean {
        if (isPublic) {
            return packageFqName().run {
                startsWith(C_NAMES_NAME) || startsWith(OBJC_NAMES_NAME)
            }
        }

        return false
    }

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy): IrModuleDeserializer {
//        if (moduleDescriptor === forwardModuleDescriptor) {
//            assert(moduleDescriptor.isForwardDeclarationModule)
//            assert(forwardDeclarationDeserializer == null)
//            return KonanForwardDeclarationsModuleDeserializer(moduleDescriptor).also {
//                forwardDeclarationDeserializer = it
//            }
//        }

//        if (moduleDescriptor.isFromInteropLibrary()) {
//            return KonanInteropModuleDeserializer((moduleDescriptor)).also {
//                interopModuleDeserializers += it
//            }
//        }

        return KonanModuleDeserializer(moduleDescriptor, strategy)
    }

    private inner class KonanModuleDeserializer(moduleDescriptor: ModuleDescriptor, strategy: DeserializationStrategy):
        IrModuleDeserializer(moduleDescriptor, strategy)

    private inner class KonanForwardDeclarationsModuleDeserializer(moduleDescriptor: ModuleDescriptor) :
            IrModuleDeserializer(moduleDescriptor, DeserializationStrategy.ONLY_DECLARATION_HEADERS) {
        init {
            assert(moduleDescriptor === forwardModuleDescriptor)
        }

        private val forwardDeclarations = mutableSetOf<DeclarationDescriptor>()

        override fun containsIdSignature(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

        fun declareForwardDeclarations() {
            val packageFragments = forwardDeclarations.map { it.findPackage() }.distinct()

            // We don't bother making a real IR module here, as we have no need in it any later.
            // All we need is just to declare forward declarations in the symbol table
            // In case you need a full fledged module, turn the forEach into a map and collect
            // produced files into an IrModuleFragment.

            packageFragments.forEach { packageFragment ->
                val symbol = IrFileSymbolImpl(packageFragment)
                val file = IrFileImpl(NaiveSourceBasedFileEntryImpl("forward declarations pseudo-file"), symbol)
                val descriptors = forwardDeclarations.filter { it.findPackage() == packageFragment }
                val declarations = descriptors.map {
                    val classDescriptor = it as ClassDescriptor
                    val declaration = symbolTable.declareClass(
                            UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin,
                            classDescriptor,
                            classDescriptor.modality,
                            classDescriptor.visibility
                    ) { symbol: IrClassSymbol -> IrClassImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irrelevantOrigin, symbol) }
                            .also { d ->
                                d.parent = file
                            }
                    declaration

                }
                file.declarations.addAll(declarations)
            }
        }
    }

    private inner class KonanInteropModuleDeserializer(moduleDescriptor: ModuleDescriptor) :
        IrModuleDeserializer(moduleDescriptor, DeserializationStrategy.ONLY_DECLARATION_HEADERS) {

        init {
            assert(moduleDescriptor.isFromInteropLibrary())
        }

        override fun containsIdSignature(idSig: IdSignature): Boolean {
            return idSig.isInteropSignature() && TODO("Distinguish interop modules somehow")
        }
    }

    /**
     * If declaration is from interop library then IR for it is generated by IrProviderForInteropStubs.
     */
    override fun IdSignature.shouldBeDeserialized(): Boolean = !isInteropSignature() && !isForwardDeclarationSignature()

    val modules: Map<String, IrModuleFragment> get() = mutableMapOf<String, IrModuleFragment>().apply {
        deserializersForModules.filter { !it.key.isForwardDeclarationModule }.forEach {
            this.put(it.key.konanLibrary!!.libraryName, it.value.module)
        }
    }
}
