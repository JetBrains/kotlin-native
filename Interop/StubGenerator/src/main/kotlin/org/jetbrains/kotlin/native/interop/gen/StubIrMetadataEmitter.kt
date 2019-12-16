/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import kotlinx.metadata.*
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.metadata.serialization.Interner
import org.jetbrains.kotlin.utils.addIfNotNull

class StubIrMetadataEmitter(
        private val context: StubIrContext,
        private val builderResult: StubIrBuilderResult,
        private val moduleName: String
) {
    fun emit(): KlibModuleMetadata {
        val annotations = emptyList<KmAnnotation>()
        val fragments = emitModuleFragments()
        return KlibModuleMetadata(moduleName, fragments, annotations)
    }

    private fun emitModuleFragments(): List<KmModuleFragment> =
            ModuleMetadataEmitter(context.configuration.pkgName, builderResult.stubs).emit().let(::listOf)
}

/**
 * Translates single [StubContainer] to [KmModuleFragment].
 */
internal class ModuleMetadataEmitter(
        private val packageFqName: String,
        private val module: SimpleStubContainer
) {

    fun emit(): KmModuleFragment {
        val uniqIdProvider = StubIrUniqIdProvider(ManglingContext.Module(packageFqName))
        val context = VisitingContext(uniqIds = uniqIdProvider)
        val elements = KmElements(visitor.visitSimpleStubContainer(module, context))
        return writeModule(elements)
    }

    private fun writeModule(elements: KmElements) = KmModuleFragment().also { km ->
        km.fqName = packageFqName
        km.classes += elements.classes.toList()
        km.className += elements.classes.map(KmClass::name)
        km.pkg = writePackage(elements)
    }

    private fun writePackage(elements: KmElements) = KmPackage().also { km ->
        km.fqName = packageFqName
        km.typeAliases += elements.typeAliases.toList()
        km.properties += elements.properties.toList()
        km.functions += elements.functions.toList()
    }

    /**
     * StubIr translation result. Since Km* classes don't have common hierarchy we need
     * to use list of Any.
     */
    private class KmElements(result: List<Any>) {
        val classes: List<KmClass> = result.filterIsInstance<List<KmClass>>().flatten()
        val properties: List<KmProperty> = result.filterIsInstance<KmProperty>()
        val typeAliases: List<KmTypeAlias> = result.filterIsInstance<KmTypeAlias>()
        val functions: List<KmFunction> = result.filterIsInstance<KmFunction>()
        val constructors: List<KmConstructor> = result.filterIsInstance<KmConstructor>()
    }

    /**
     * Used to pass data between parents and children when visiting StubIr elements.
     */
    private data class VisitingContext(
            val container: StubContainer? = null,
            val uniqIds: StubIrUniqIdProvider,
            val typeParametersInterner: Interner<TypeParameterStub> = Interner()
    )

    private val visitor = object : StubIrVisitor<VisitingContext, Any> {

        override fun visitClass(element: ClassStub, data: VisitingContext) {
            // TODO("not implemented")
        }

        override fun visitTypealias(element: TypealiasStub, data: VisitingContext): KmTypeAlias =
                with (MappingExtensions(data.typeParametersInterner)) {
                    KmTypeAlias(element.flags, element.alias.topLevelName).also { km ->
                        km.uniqId = data.uniqIds.uniqIdForTypeAlias(element)
                        km.underlyingType = element.aliasee.map(shouldExpandTypeAliases = false)
                        km.expandedType = element.aliasee.map()
                    }
                }

        override fun visitFunction(element: FunctionStub, data: VisitingContext) =
                with (MappingExtensions(data.typeParametersInterner)) {
                    KmFunction(element.flags, element.name).also { km ->
                        element.typeParameters.mapTo(km.typeParameters) { it.map() }
                        element.parameters.mapTo(km.valueParameters) { it.map() }
                        element.annotations.mapTo(km.annotations) { it.map() }
                        km.returnType = element.returnType.map()
                        km.uniqId = data.uniqIds.uniqIdForFunction(element)
                    }
                }

        override fun visitProperty(element: PropertyStub, data: VisitingContext) =
                with (MappingExtensions(data.typeParametersInterner)) {
                    KmProperty(element.flags, element.name, element.getterFlags, element.setterFlags).also { km ->
                        element.annotations.mapTo(km.annotations) { it.map() }
                        km.uniqId = data.uniqIds.uniqIdForProperty(element)
                        km.returnType = element.type.map()
                        if (element.kind is PropertyStub.Kind.Var) {
                            val setter = element.kind.setter
                            setter.annotations.mapTo(km.setterAnnotations) { it.map() }
                            // TODO: Maybe it's better to explicitly add setter parameter in stub.
                            km.setterParameter = FunctionParameterStub("value", element.type).map()
                        }
                        km.getterAnnotations += when (element.kind) {
                            is PropertyStub.Kind.Val -> element.kind.getter.annotations.map { it.map() }
                            is PropertyStub.Kind.Var -> element.kind.getter.annotations.map { it.map() }
                            is PropertyStub.Kind.Constant -> emptyList()
                        }
                        if (element.kind is PropertyStub.Kind.Constant) {
                            km.compileTimeValue = element.kind.constant.mapToAnnotationArgument()
                        }
                    }
                }

        override fun visitConstructor(constructorStub: ConstructorStub, data: VisitingContext) {
            // TODO("not implemented")
        }

        override fun visitPropertyAccessor(propertyAccessor: PropertyAccessor, data: VisitingContext) {
            // TODO("not implemented")
        }

        override fun visitSimpleStubContainer(simpleStubContainer: SimpleStubContainer, data: VisitingContext): List<Any> =
                simpleStubContainer.children.map { it.accept(this, data) } +
                        simpleStubContainer.simpleContainers.flatMap { visitSimpleStubContainer(it, data) }
    }
}

/**
 * Collection of extension functions that simplify translation of
 * StubIr elements to Kotlin Metadata.
 */
private class MappingExtensions(
        private val typeParametersInterner: Interner<TypeParameterStub>
) {

    private fun flagsOfNotNull(vararg flags: Flag?): Flags =
            flagsOf(*listOfNotNull(*flags).toTypedArray())

    private fun <K, V> mapOfNotNull(vararg entries: Pair<K, V>?): Map<K, V> =
            listOfNotNull(*entries).toMap()

    private val VisibilityModifier.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC.takeIf { this == VisibilityModifier.PUBLIC },
                Flag.IS_PROTECTED.takeIf { this == VisibilityModifier.PROTECTED },
                Flag.IS_INTERNAL.takeIf { this == VisibilityModifier.INTERNAL },
                Flag.IS_PRIVATE.takeIf { this == VisibilityModifier.PRIVATE }
        )

    private val MemberStubModality.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_FINAL.takeIf { this == MemberStubModality.FINAL },
                Flag.IS_OPEN.takeIf { this == MemberStubModality.OPEN },
                Flag.IS_ABSTRACT.takeIf { this == MemberStubModality.ABSTRACT }
        )

    val FunctionStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC,
                Flag.Function.IS_EXTERNAL,
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() }
        ) or modality.flags

    val Classifier.fqNameSerialized: String
        get() = buildString {
            if (pkg.isNotEmpty()) {
                append(pkg.replace('.', '/'))
                append('/')
            }
            // Nested classes should dot-separated.
            append(relativeFqName)
        }

    val PropertyStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC,
                Flag.Property.IS_DECLARATION,
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                when (kind) {
                    is PropertyStub.Kind.Val -> null
                    is PropertyStub.Kind.Var -> Flag.Property.IS_VAR
                    is PropertyStub.Kind.Constant -> Flag.Property.IS_CONST
                },
                when (kind) {
                    is PropertyStub.Kind.Constant -> null
                    is PropertyStub.Kind.Val,
                    is PropertyStub.Kind.Var -> Flag.Property.HAS_GETTER
                },
                when (kind) {
                    is PropertyStub.Kind.Constant -> null
                    is PropertyStub.Kind.Val -> null
                    is PropertyStub.Kind.Var -> Flag.Property.HAS_SETTER
                }
        ) or modality.flags

    val PropertyStub.getterFlags: Flags
        get() = when (kind) {
            is PropertyStub.Kind.Val -> kind.getter.flags
            is PropertyStub.Kind.Var -> kind.getter.flags
            is PropertyStub.Kind.Constant -> flagsOf()
        }

    private val PropertyAccessor.Getter.flags: Flags
        get() = flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.IS_PUBLIC,
                Flag.IS_FINAL,
                Flag.PropertyAccessor.IS_EXTERNAL.takeIf { this is PropertyAccessor.Getter.ExternalGetter }
        )

    val PropertyStub.setterFlags: Flags
        get() = if (kind !is PropertyStub.Kind.Var) flagsOf()
        else kind.setter.flags

    val PropertyAccessor.Setter.flags: Flags
        get() = flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() },
                Flag.IS_PUBLIC,
                Flag.IS_FINAL,
                Flag.PropertyAccessor.IS_EXTERNAL.takeIf { this is PropertyAccessor.Setter.ExternalSetter }
        )

    val StubType.flags: Flags
        get() = flagsOfNotNull(
                Flag.Type.IS_NULLABLE.takeIf { nullable }
        )

    val TypealiasStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.IS_PUBLIC
        )

    val FunctionParameterStub.flags: Flags
        get() = flagsOfNotNull(
                Flag.HAS_ANNOTATIONS.takeIf { annotations.isNotEmpty() }
        )

    fun AnnotationStub.map(): KmAnnotation {
        fun Pair<String, String>.asAnnotationArgument() =
                (first to KmAnnotationArgument.StringValue(second)).takeIf { second.isNotEmpty() }

        fun replaceWith(replaceWith: String) = KmAnnotationArgument.AnnotationValue(KmAnnotation(
                Classifier.topLevel("kotlin", "ReplaceWith").fqNameSerialized,
                mapOfNotNull(
                        "imports" to KmAnnotationArgument.ArrayValue(emptyList()),
                        ("expression" to replaceWith).asAnnotationArgument()
                )
        ))

        fun deprecationLevel(level: DeprecationLevel) = KmAnnotationArgument.EnumValue(
                Classifier.topLevel("kotlin", "DeprecationLevel").fqNameSerialized,
                level.name
        )

        val args = when (this) {
            AnnotationStub.ObjC.ConsumesReceiver -> emptyMap()
            AnnotationStub.ObjC.ReturnsRetained -> emptyMap()
            is AnnotationStub.ObjC.Method -> mapOfNotNull(
                    ("selector" to selector).asAnnotationArgument(),
                    ("encoding" to encoding).asAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            is AnnotationStub.ObjC.Factory -> mapOfNotNull(
                    ("selector" to selector).asAnnotationArgument(),
                    ("encoding" to encoding).asAnnotationArgument(),
                    ("isStret" to KmAnnotationArgument.BooleanValue(isStret))
            )
            AnnotationStub.ObjC.Consumed -> emptyMap()
            is AnnotationStub.ObjC.Constructor -> mapOfNotNull(
                    ("designated" to KmAnnotationArgument.BooleanValue(designated)),
                    ("initSelector" to selector).asAnnotationArgument()
            )
            is AnnotationStub.ObjC.ExternalClass -> mapOfNotNull(
                    ("protocolGetter" to protocolGetter).asAnnotationArgument(),
                    ("binaryName" to binaryName).asAnnotationArgument()
            )
            AnnotationStub.CCall.CString -> emptyMap()
            AnnotationStub.CCall.WCString -> emptyMap()
            is AnnotationStub.CCall.Symbol -> mapOfNotNull(
                    ("id" to symbolName).asAnnotationArgument()
            )
            is AnnotationStub.CStruct -> mapOfNotNull(
                    ("spelling" to struct).asAnnotationArgument()
            )
            is AnnotationStub.CNaturalStruct ->
                error("@CNaturalStruct should not be used for Kotlin/Native interop")
            is AnnotationStub.CLength -> mapOfNotNull(
                    "value" to KmAnnotationArgument.LongValue(length)
            )
            is AnnotationStub.Deprecated -> mapOfNotNull(
                    ("message" to message).asAnnotationArgument(),
                    ("replaceWith" to replaceWith(replaceWith)),
                    ("level" to deprecationLevel(DeprecationLevel.ERROR))
            )
        }
        return KmAnnotation(classifier.fqNameSerialized, args)
    }

    /**
     * @param shouldExpandTypeAliases describes how should we write type aliases.
     * If [shouldExpandTypeAliases] is true then type alias-based types are written as
     * ```
     * Type {
     *  abbreviatedType = AbbreviatedType.abbreviatedClassifier
     *  classifier = AbbreviatedType.underlyingType
     *  arguments = AbbreviatedType.underlyingType.typeArguments
     * }
     * ```
     * So we basically replacing type alias with underlying class.
     * Otherwise:
     * ```
     * Type {
     *  classifier = AbbreviatedType.abbreviatedClassifier
     * }
     * ```
     * As of 25 Nov 2019, the latter form is used only for KmTypeAlias.underlyingType.
     */
    // TODO: Add caching if needed.
    fun StubType.map(shouldExpandTypeAliases: Boolean = true): KmType = when (this) {
        is AbbreviatedType -> {
            val typeAliasClassifier = KmClassifier.TypeAlias(abbreviatedClassifier.fqNameSerialized)
            if (shouldExpandTypeAliases) {
                // Abbreviated and expanded types have the same nullability.
                KmType(flags).also { km ->
                    km.abbreviatedType = KmType(flags).also { it.classifier = typeAliasClassifier }
                    val kmUnderlyingType = underlyingType.map(true)
                    km.arguments += kmUnderlyingType.arguments
                    km.classifier = kmUnderlyingType.classifier
                }
            } else {
                KmType(flags).also { km -> km.classifier = typeAliasClassifier }
            }
        }
        is ClassifierStubType -> KmType(flags).also { km ->
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is FunctionalType -> KmType(flags).also { km ->
            typeArguments.mapTo(km.arguments) { it.map(shouldExpandTypeAliases) }
            km.classifier = KmClassifier.Class(classifier.fqNameSerialized)
        }
        is TypeParameterType -> KmType(flags).also { km ->
            km.classifier = KmClassifier.TypeParameter(id)
        }
    }

    fun FunctionParameterStub.map(): KmValueParameter =
            KmValueParameter(flags, name).also { km ->
                val kmType = type.map()
                if (isVararg) {
                    km.varargElementType = kmType
                } else {
                    km.type = kmType
                }
                annotations.mapTo(km.annotations, { it.map() })
            }

    fun TypeParameterStub.map(): KmTypeParameter =
            KmTypeParameter(flagsOf(), name, id, KmVariance.INVARIANT).also { km ->
                km.upperBounds.addIfNotNull(upperBound?.map())
            }

    private fun TypeArgument.map(expanded: Boolean = true): KmTypeProjection = when (this) {
        TypeArgument.StarProjection -> KmTypeProjection.STAR
        is TypeArgumentStub -> KmTypeProjection(variance.map(), type.map(expanded))
        else -> error("Unexpected TypeArgument: $this")
    }

    private fun TypeArgument.Variance.map(): KmVariance = when (this) {
        TypeArgument.Variance.INVARIANT -> KmVariance.INVARIANT
        TypeArgument.Variance.IN -> KmVariance.IN
        TypeArgument.Variance.OUT -> KmVariance.OUT
    }

    fun ConstantStub.mapToAnnotationArgument(): KmAnnotationArgument<*> = when (this) {
        is StringConstantStub -> KmAnnotationArgument.StringValue(value)
        is IntegralConstantStub -> when (size) {
            1 -> if (isSigned) {
                KmAnnotationArgument.ByteValue(value.toByte())
            } else {
                KmAnnotationArgument.UByteValue(value.toByte())
            }
            2 -> if (isSigned) {
                KmAnnotationArgument.ShortValue(value.toShort())
            } else {
                KmAnnotationArgument.UShortValue(value.toShort())
            }
            4 -> if (isSigned) {
                KmAnnotationArgument.IntValue(value.toInt())
            } else {
                KmAnnotationArgument.UIntValue(value.toInt())
            }
            8 -> if (isSigned) {
                KmAnnotationArgument.LongValue(value)
            } else {
                KmAnnotationArgument.ULongValue(value)
            }

            else -> error("Integral constant of value $value with unexpected size of $size.")
        }
        is DoubleConstantStub -> when (size) {
            4 -> KmAnnotationArgument.FloatValue(value.toFloat())
            8 -> KmAnnotationArgument.DoubleValue(value)
            else -> error("Floating-point constant of value $value with unexpected size of $size.")
        }
    }

    private val TypeParameterType.id: Int
        get() = typeParameterDeclaration.id

    private val TypeParameterStub.id: Int
        get() = typeParametersInterner.intern(this)
}