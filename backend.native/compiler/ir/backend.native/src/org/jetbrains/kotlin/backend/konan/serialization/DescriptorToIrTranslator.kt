package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.descriptors.findTopLevelDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.withScope
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.psi2ir.generators.StandaloneDeclarationGenerator
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.cast

class DescriptorToIrTranslator(
        private val moduleFragment: IrModuleFragment,
        private val klib: KotlinLibrary,
        private val standaloneDeclarationGenerator: StandaloneDeclarationGenerator
) {
    private val irGenerator = IrGenerator(standaloneDeclarationGenerator)
    private val symbolTable = standaloneDeclarationGenerator.symbolTable
    private val processedTopLevelDescriptors = mutableMapOf<DeclarationDescriptor, IrDeclaration>()

    fun getDeclaration(
            declarationDescriptor: DeclarationDescriptor,
            irFile: IrFile,
            symbolKind: BinarySymbolData.SymbolKind
    ): IrSymbol {
        val topLevelDescriptor = declarationDescriptor.findTopLevelDescriptor()
        val topLevelIrDeclaration = processedTopLevelDescriptors.getOrPut(topLevelDescriptor) {
            generateSubtree(topLevelDescriptor, irFile).also {
                irFile.addChild(it)
                irFile.patchDeclarationParents()
            }
        }
        return when (symbolKind) {
            BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> symbolTable.referenceSimpleFunction(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> symbolTable.referenceConstructor(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> symbolTable.referenceEnumEntry(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.FIELD_SYMBOL -> symbolTable.referenceField(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.VALUE_PARAMETER_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.RETURNABLE_BLOCK_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.CLASS_SYMBOL -> symbolTable.referenceClass(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.TYPE_PARAMETER_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.VARIABLE_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.ANONYMOUS_INIT_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.STANDALONE_FIELD_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.RECEIVER_PARAMETER_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> symbolTable.referenceProperty(declarationDescriptor.cast())
            BinarySymbolData.SymbolKind.LOCAL_DELEGATED_PROPERTY_SYMBOL -> TODO()
            BinarySymbolData.SymbolKind.TYPEALIAS_SYMBOL -> symbolTable.referenceTypeAlias(declarationDescriptor.cast())
        }
    }

    private fun generateSubtree(declarationDescriptor: DeclarationDescriptor, parent: IrDeclarationContainer): IrDeclaration {
        return declarationDescriptor.accept(irGenerator, parent)
    }
}

private class IrGenerator(
        private val standaloneDeclarationGenerator: StandaloneDeclarationGenerator
) : DeclarationDescriptorVisitor<IrDeclaration, IrDeclarationContainer> {

    private val symbolTable = standaloneDeclarationGenerator.symbolTable
    private val offset = UNDEFINED_OFFSET
    private val origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    private fun <D : IrDeclaration> D.insertDeclaration(declarationContainer: IrDeclarationContainer): D {
        parent = declarationContainer
        declarationContainer.declarations.add(this)
        return this
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: IrDeclarationContainer): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            val origin = when (descriptor.kind) {
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> IrDeclarationOrigin.FAKE_OVERRIDE
                else -> origin
            }
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
                    .insertDeclaration(data)
        }
    }

    private fun collectDescriptors(descriptor: ClassDescriptor) = mutableListOf<DeclarationDescriptor>().apply {
        addAll(DescriptorUtils.getAllDescriptors(descriptor.unsubstitutedMemberScope))
        addAll(descriptor.constructors)
        addIfNotNull(descriptor.companionObjectDescriptor)
    }

    private fun ensureMemberScope(irClass: IrClass) {
        val declaredDescriptors = irClass.declarations.map { it.descriptor }
        val contributedDescriptors = collectDescriptors(irClass.descriptor)

        contributedDescriptors.removeAll(declaredDescriptors)

        symbolTable.withScope(irClass) {
            contributedDescriptors.forEach {
                it.accept(this@IrGenerator, irClass)
            }
        }
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, data: IrDeclarationContainer): IrDeclaration {
        return if (DescriptorUtils.isEnumEntry(descriptor)) {
            symbolTable.declareEnumEntryIfNotExists(descriptor) {
                standaloneDeclarationGenerator.generateEnumEntry(offset, offset, origin, descriptor, it)
                        .insertDeclaration(data)
            }
        } else symbolTable.declareClassIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateClass(offset, offset, origin, descriptor, it).also { irClass ->
                irClass.insertDeclaration(data)
                ensureMemberScope(irClass)
            }
        }
    }

    override fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, data: IrDeclarationContainer): IrDeclaration {
        return symbolTable.declareTypeAliasIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateTypeAlias(offset, offset, origin, descriptor, it).insertDeclaration(data)
        }
    }

    override fun visitConstructorDescriptor(constructorDescriptor: ConstructorDescriptor, data: IrDeclarationContainer): IrDeclaration {
        require(constructorDescriptor is ClassConstructorDescriptor)
        return symbolTable.declareConstructorIfNotExists(constructorDescriptor) {
            standaloneDeclarationGenerator.generateConstructor(offset, offset, origin, constructorDescriptor, it).insertDeclaration(data)
        }
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: IrDeclarationContainer): IrDeclaration {
        return symbolTable.declarePropertyIfNotExists(descriptor) {
            val origin = when (descriptor.kind) {
                CallableMemberDescriptor.Kind.FAKE_OVERRIDE -> IrDeclarationOrigin.FAKE_OVERRIDE
                else -> origin
            }
            standaloneDeclarationGenerator.generateProperty(offset, offset, origin, descriptor, it).also {
                it.getter = descriptor.getter?.accept(this, null) as? IrSimpleFunction
                it.setter = descriptor.setter?.accept(this, null) as? IrSimpleFunction
                it.insertDeclaration(data)
            }
        }
    }

    override fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: IrDeclarationContainer?): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: IrDeclarationContainer?): IrDeclaration {
        return symbolTable.declareSimpleFunctionIfNotExists(descriptor) {
            standaloneDeclarationGenerator.generateSimpleFunction(offset, offset, origin, descriptor, it)
        }
    }

    override fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitModuleDeclaration(descriptor: ModuleDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }

    override fun visitVariableDescriptor(descriptor: VariableDescriptor?, data: IrDeclarationContainer?): IrDeclaration {
        TODO("Not yet implemented")
    }
}