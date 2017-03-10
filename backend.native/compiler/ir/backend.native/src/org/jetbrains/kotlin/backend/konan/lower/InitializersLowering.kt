package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.descriptors.synthesizedName
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrInstanceInitializerCall
import org.jetbrains.kotlin.ir.expressions.IrStatementOriginImpl
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.descriptorUtils.hasPrimaryConstructor

internal class InitializersLowering(val context: Context) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        InitializersTransformer(irClass).lowerInitializers()
    }

    private inner class InitializersTransformer(val irClass: IrClass) {
        val initializers = mutableListOf<IrStatement>()

        fun lowerInitializers() {
            collectAndRemoveInitializers()
            val initializerMethodDescriptor = createInitializerMethod()
            lowerConstructors(initializerMethodDescriptor)
        }

        object STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER :
                IrStatementOriginImpl("ANONYMOUS_INITIALIZER")

        object DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER :
                IrDeclarationOriginImpl("ANONYMOUS_INITIALIZER")

        private fun collectAndRemoveInitializers() {
            // Do with one traversal in order to preserve initializers order.
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
                    initializers.add(IrBlockImpl(declaration.startOffset, declaration.endOffset,
                            context.builtIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER, declaration.body.statements))
                    return declaration
                }

                override fun visitField(declaration: IrField): IrStatement {
                    val initializer = declaration.initializer ?: return declaration
                    val propertyDescriptor = declaration.descriptor
                    val startOffset = initializer.startOffset
                    val endOffset = initializer.endOffset
                    initializers.add(IrBlockImpl(startOffset, endOffset, context.builtIns.unitType, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER,
                            listOf(
                                    IrSetFieldImpl(startOffset, endOffset, propertyDescriptor,
                                            IrGetValueImpl(startOffset, endOffset, irClass.descriptor.thisAsReceiverParameter),
                                            initializer.expression, STATEMENT_ORIGIN_ANONYMOUS_INITIALIZER))))
                    declaration.initializer = null
                    return declaration
                }
            })

            irClass.declarations.transformFlat {
                if (it !is IrAnonymousInitializer)
                    null
                else listOf()
            }
        }

        private fun createInitializerMethod(): FunctionDescriptor {
            val initializerMethodDescriptor = SimpleFunctionDescriptorImpl.create(
                    irClass.descriptor,
                    Annotations.EMPTY,
                    "INITIALIZER".synthesizedName,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE).apply {
                val parameters = irClass.descriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: listOf()
                initialize(
                        null,
                        irClass.descriptor.thisAsReceiverParameter,
                        listOf(),
                        parameters,
                        context.builtIns.unitType,
                        Modality.FINAL,
                        Visibilities.PRIVATE)
            }
            val startOffset = irClass.startOffset
            val endOffset = irClass.endOffset
            val initializer = IrFunctionImpl(startOffset, endOffset, DECLARATION_ORIGIN_ANONYMOUS_INITIALIZER,
                    initializerMethodDescriptor, IrBlockBodyImpl(startOffset, endOffset, initializers))
            irClass.declarations.add(initializer)

            return initializerMethodDescriptor
        }

        private fun lowerConstructors(initializerMethodDescriptor: FunctionDescriptor) {
            val parameters = irClass.descriptor.unsubstitutedPrimaryConstructor?.valueParameters ?: listOf()
            irClass.transformChildrenVoid(object : IrElementTransformerVoid() {

                override fun visitClass(declaration: IrClass): IrStatement {
                    // Skip nested.
                    return declaration
                }

                override fun visitConstructor(declaration: IrConstructor): IrStatement {
                    val blockBody = declaration.body as? IrBlockBody ?: throw AssertionError("Unexpected constructor body: ${declaration.body}")

                    blockBody.statements.transformFlat {
                        when {
                            it is IrInstanceInitializerCall -> {
                                val startOffset = it.startOffset
                                val endOffset = it.endOffset
                                val constructorParameters = declaration.descriptor.valueParameters
                                listOf(IrCallImpl(startOffset, endOffset, initializerMethodDescriptor).apply {
                                    dispatchReceiver = IrGetValueImpl(startOffset, endOffset, irClass.descriptor.thisAsReceiverParameter)
                                    parameters.forEach { // Here we are sure in the primary constructor - take his parameters.
                                        putValueArgument(it.index, IrGetValueImpl(startOffset, endOffset, constructorParameters[it.index]))
                                    }
                                })
                            }
                        /**
                         * IR for kotlin.Any is:
                         * BLOCK_BODY
                         *   DELEGATING_CONSTRUCTOR_CALL 'constructor Any()'
                         *   INSTANCE_INITIALIZER_CALL classDescriptor='Any'
                         *
                         *   to avoid possible recursion we manually reject body generation for Any.
                         */
                            it is IrDelegatingConstructorCall && irClass.descriptor == context.builtIns.any
                                    && it.descriptor == declaration.descriptor -> listOf()
                            else -> null
                        }
                    }

                    return declaration
                }
            })
        }
    }
}