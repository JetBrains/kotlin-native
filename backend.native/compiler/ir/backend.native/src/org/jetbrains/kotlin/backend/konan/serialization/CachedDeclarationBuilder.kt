package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.descriptors.findTopLevelDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.psi2ir.generators.StandaloneDeclarationGenerator

class CachedDeclarationBuilder(
        private val moduleFragment: IrModuleFragment,
        private val klib: KotlinLibrary,
        private val standaloneDeclarationGenerator: StandaloneDeclarationGenerator
) {
    private val irGenerator = IrGenerator(standaloneDeclarationGenerator)

    private val processedTopLevelDescriptors = mutableMapOf<DeclarationDescriptor, IrDeclaration>()

    fun getDeclaration(
            declarationDescriptor: DeclarationDescriptor,
            irFile: IrFile,
            symbolKind: BinarySymbolData.SymbolKind
    ): IrDeclaration {
        val topLevelDescriptor = declarationDescriptor.findTopLevelDescriptor()
        return processedTopLevelDescriptors.getOrPut(topLevelDescriptor) {
            generateSubtree(topLevelDescriptor, irFile).also {
                it.patchDeclarationParents(irFile)
                irFile.addChild(it)
            }
        }
    }

    private fun generateSubtree(declarationDescriptor: DeclarationDescriptor, parent: IrDeclarationParent): IrDeclaration {
        return declarationDescriptor.accept(irGenerator, parent)
    }
}

private class IrGenerator(
        private val standaloneDeclarationGenerator: StandaloneDeclarationGenerator
) : DeclarationDescriptorVisitor<IrDeclaration, IrDeclarationParent> {

    private val symbolTable = standaloneDeclarationGenerator.symbolTable
    private val offset = UNDEFINED_OFFSET
    private val origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: IrDeclarationParent): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: IrDeclarationParent): IrDeclaration {
        return symbolTable.declareClassIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateClass(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: IrDeclarationParent): IrDeclaration {
        return symbolTable.declareTypeAliasIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateTypeAlias(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: IrDeclarationParent): IrDeclaration {
        require(constructorDescriptor is ClassConstructorDescriptor)
        return symbolTable.declareConstructorIfNotExists(constructorDescriptor) {
            standaloneDeclarationGenerator.generateConstructor(offset, offset, origin, constructorDescriptor, it)
        }
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: IrDeclarationParent): IrDeclaration {
        return symbolTable.declarePropertyIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateProperty(offset, offset, origin, descriptor, it).also {
                it.getter = descriptor.getter?.accept(this, null) as IrSimpleFunction
                it.setter = descriptor.setter?.accept(this, null) as IrSimpleFunction
            }
        }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: IrDeclarationParent?): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: IrDeclarationParent?): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: IrDeclarationParent?): IrDeclaration {
        TODO("Not yet implemented")
    }
}