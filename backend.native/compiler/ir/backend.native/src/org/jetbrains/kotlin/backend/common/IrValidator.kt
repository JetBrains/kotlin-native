package org.jetbrains.kotlin.backend.common

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun validateIrFile(context: BackendContext, irFile: IrFile) {
    val visitor = IrValidator(context)
    irFile.acceptVoid(visitor)
}

fun validateIrModule(context: BackendContext, irModule: IrModuleFragment) {
    val visitor = IrValidator(context)
    irModule.acceptVoid(visitor)

    val moduleDeclarations = visitor.foundDeclarations

    irModule.acceptVoid(object : IrElementVisitorVoid {
        lateinit var currentFile: IrFile

        override fun visitFile(declaration: IrFile) {
            currentFile = declaration
            declaration.acceptChildrenVoid(this)
        }

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitProperty(declaration: IrProperty) {
            if (declaration.descriptor.modality == Modality.ABSTRACT) {
                return // Workaround TODO
            }

            declaration.acceptChildrenVoid(this)
        }

        override fun visitDeclarationReference(expression: IrDeclarationReference) {
            expression.acceptChildrenVoid(this)

            val declarationDescriptor = expression.irDeclarationDescriptor ?: return

            val declarationModule = declarationDescriptor.module

            if (declarationModule == irModule.descriptor && !moduleDeclarations.hasTarget(expression)) {
                context.reportIrValidationError(
                        "declaration $declarationDescriptor is missing;\n" +
                                "references from:\n" +
                                expression.render(),

                        currentFile, expression)
            }
        }
    })
}

private fun BackendContext.reportIrValidationError(message: String, irFile: IrFile, irElement: IrElement) {
    this.reportWarning("[IR VALIDATION] $message", irFile, irElement)
    // TODO: throw an exception after fixing bugs leading to invalid IR.
}

/**
 * Descriptor to be found in IR element declaring target of this reference,
 * or `null` if it is not supported yet.
 */
private val IrDeclarationReference.irDeclarationDescriptor: DeclarationDescriptor?
    get() {
        when (this) {
            is IrFieldAccessExpression -> return this.descriptor.original

            is IrMemberAccessExpression -> {
                val original = this.descriptor.original
                if (original is CallableMemberDescriptor && !original.kind.isReal) {
                    return null
                }

                if (original is TypeAliasConstructorDescriptor) {
                    return original.underlyingConstructorDescriptor
                }

                return original
            }

            is IrGetValue -> {
                if (this.descriptor is ParameterDescriptor) {
                    return null
                }

                return this.descriptor
            }

            else -> return this.descriptor
        }
    }

private val IrDeclarationReference.declarationKind: IrDeclarationKind
    get() = when (this) {
        is IrClassReference, is IrGetObjectValue -> IrDeclarationKind.CLASS
        is IrFieldAccessExpression -> IrDeclarationKind.FIELD
        is IrGetEnumValue -> IrDeclarationKind.ENUM_ENTRY
        is IrValueAccessExpression -> IrDeclarationKind.VARIABLE
        is IrMemberAccessExpression -> when (this.descriptor) {
            is ConstructorDescriptor -> IrDeclarationKind.CONSTRUCTOR
            is PropertyDescriptor -> IrDeclarationKind.PROPERTY
            is VariableDescriptorWithAccessors -> IrDeclarationKind.LOCAL_PROPERTY
            else -> IrDeclarationKind.FUNCTION
        }
        else -> TODO()
    }

/**
 * The collection of IR declarations.
 */
private class Declarations {
    private data class DeclarationKey(val descriptor: DeclarationDescriptor, val kind: IrDeclarationKind)

    private val declarations = mutableSetOf<DeclarationKey>()

    private fun createKey(declaration: IrDeclaration) =
            DeclarationKey(declaration.descriptor, declaration.declarationKind)

    fun add(declaration: IrDeclaration) {
        declarations.add(createKey(declaration))
    }

    fun addParameterOf(aCatch: IrCatch) {
        val kind = IrDeclarationKind.VARIABLE
        val key = DeclarationKey(aCatch.parameter, kind)
        declarations.add(key)
    }

    fun isAlreadyDeclared(declaration: IrDeclaration): Boolean {
        return createKey(declaration) in declarations
    }

    fun hasTarget(reference: IrDeclarationReference): Boolean {
        val descriptor = reference.irDeclarationDescriptor ?: return false
        val kind = reference.declarationKind

        return DeclarationKey(descriptor, kind) in declarations
    }
}

private class IrValidator(val context: BackendContext) : IrElementVisitorVoid {

    val foundDeclarations = Declarations()

    val builtIns = context.builtIns
    lateinit var currentFile: IrFile

    override fun visitFile(declaration: IrFile) {
        currentFile = declaration
        super.visitFile(declaration)
    }

    private fun error(element: IrElement, message: String) {
        // TODO: render all element's parents.
        context.reportIrValidationError(
                "$message\n" +
                        element.render(),
                currentFile, element)
    }

    private val elementChecker = CheckIrElementVisitor(builtIns, this::error)

    override fun visitElement(element: IrElement) {
        element.acceptVoid(elementChecker)
        element.acceptChildrenVoid(this)
    }

    private fun recordDeclaration(declaration: IrDeclaration) {
        if (foundDeclarations.isAlreadyDeclared(declaration)) {
            error(declaration, "redeclaration")
        } else {
            foundDeclarations.add(declaration)
        }
    }

    override fun visitDeclaration(declaration: IrDeclaration) {
        recordDeclaration(declaration)
        super.visitDeclaration(declaration)
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer) {
        // Do not treat anonymous initializers as declarations, because they are not unique.
        super.visitDeclaration(declaration)
    }

    override fun visitCatch(aCatch: IrCatch) {
        foundDeclarations.addParameterOf(aCatch)
        super.visitCatch(aCatch)
    }
}
