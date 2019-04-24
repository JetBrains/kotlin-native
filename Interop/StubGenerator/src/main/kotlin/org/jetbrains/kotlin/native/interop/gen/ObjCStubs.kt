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

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.native.interop.gen.jvm.StubGenerator
import org.jetbrains.kotlin.native.interop.indexer.*

private fun ObjCMethod.getKotlinParameterNames(forConstructorOrFactory: Boolean = false): List<String> {
    val selectorParts = this.selector.split(":")

    val result = mutableListOf<String>()

    fun String.mangled(): String {
        var mangled = this
        while (mangled in result) {
            mangled = "_$mangled"
        }
        return mangled
    }

    // The names of all parameters except first must depend only on the selector:
    this.parameters.forEachIndexed { index, _ ->
        if (index > 0) {
            val name = selectorParts[index].takeIf { it.isNotEmpty() } ?: "_$index"
            result.add(name.mangled())
        }
    }

    this.parameters.firstOrNull()?.let {
        val name = this.getFirstKotlinParameterNameCandidate(forConstructorOrFactory)
        result.add(0, name.mangled())
    }

    if (this.isVariadic) {
        result.add("args".mangled())
    }

    return result
}

private fun ObjCMethod.getFirstKotlinParameterNameCandidate(forConstructorOrFactory: Boolean): String {
    if (forConstructorOrFactory) {
        val selectorPart = this.selector.takeWhile { it != ':' }.trimStart('_')
        if (selectorPart.startsWith("init")) {
            selectorPart.removePrefix("init").removePrefix("With")
                    .takeIf { it.isNotEmpty() }?.let { return it.decapitalize() }
        }
    }

    return this.parameters.first().name?.takeIf { it.isNotEmpty() } ?: "_0"
}

private fun ObjCMethod.getKotlinParameters(
        stubGenerator: StubGenerator,
        forConstructorOrFactory: Boolean
): List<KotlinParameter> {
    val names = getKotlinParameterNames(forConstructorOrFactory) // TODO: consider refactoring.
    val result = mutableListOf<KotlinParameter>()

    this.parameters.mapIndexedTo(result) { index, it ->
        val kotlinType = stubGenerator.mirror(it.type).argType
        val name = names[index]
        val annotations = if (it.nsConsumed) listOf("@CCall.Consumed") else emptyList()
        KotlinParameter(name, kotlinType, isVararg = false, annotations = annotations)
    }

    if (this.isVariadic) {
        result += KotlinParameter(
                names.last(),
                KotlinTypes.any.makeNullable(),
                isVararg = true,
                annotations = emptyList()
        )
    }

    return result
}

class ObjCMethodStub(private val stubGenerator: StubGenerator,
                     val method: ObjCMethod,
                     private val container: ObjCContainer,
                     private val isDesignatedInitializer: Boolean) : KotlinStub, NativeBacked {

    override fun generate(context: StubGenerationContext): Sequence<String> =
            if (context.nativeBridges.isSupported(this)) {
                val result = mutableListOf<String>()
                result.addAll(objCMethodAnnotations)
                result.add(header)

                if (method.isInit) {
                    // TODO: generate only constructor/factory in this case.
                    val kotlinScope = stubGenerator.kotlinFile

                    val parameters = method.getKotlinParameters(stubGenerator, forConstructorOrFactory = true)
                            .renderParameters(kotlinScope)

                    when (container) {
                        is ObjCClass -> {
                            result.add(0,
                                    deprecatedInit(
                                            container.kotlinClassName(method.isClass),
                                            kotlinMethodParameters.map { it.name },
                                            factory = false
                                    )
                            )

                            // TODO: consider generating non-designated initializers as factories.
                            val designated = isDesignatedInitializer ||
                                    stubGenerator.configuration.disableDesignatedInitializerChecks

                            result.add("")
                            result.add("@ObjCConstructor(${method.selector.quoteAsKotlinLiteral()}, $designated)")
                            result.add("constructor($parameters) {}")
                        }
                        is ObjCCategory -> {
                            assert(!method.isClass)

                            val className = stubGenerator.declarationMapper
                                    .getKotlinClassFor(container.clazz, isMeta = false).type
                                    .render(kotlinScope)

                            result.add(0,
                                    deprecatedInit(
                                            className,
                                            kotlinMethodParameters.map { it.name },
                                            factory = true
                                    )
                            )

                            // TODO: add support for type parameters to [KotlinType] etc.
                            val receiver = kotlinScope.reference(KotlinTypes.objCClassOf) + "<T>"

                            val originalReturnType = method.getReturnType(container.clazz)
                            val returnType = if (originalReturnType is ObjCPointer) {
                                if (originalReturnType.isNullable) "T?" else "T"
                            } else {
                                // This shouldn't happen actually.
                                this.kotlinReturnType
                            }

                            result.add("")
                            result.addAll(buildObjCMethodAnnotations("@ObjCFactory"))
                            result.add("external fun <T : $className> $receiver.create($parameters): $returnType")
                        }
                        is ObjCProtocol -> {} // Nothing to do.
                    }
                }

                result.asSequence()
            } else {
                sequenceOf(
                        annotationForUnableToImport,
                        header
                )
            }

    private val kotlinMethodParameters: List<KotlinParameter>
    private val kotlinReturnType: String
    private val header: String
    internal val objCMethodAnnotations: List<String>
    private val isStret: Boolean

    init {
        kotlinMethodParameters = method.getKotlinParameters(stubGenerator, forConstructorOrFactory = false)

        val returnType = method.getReturnType(container.classOrProtocol)

        isStret = returnType.isStret(stubGenerator.configuration.target)

        this.kotlinReturnType = if (returnType.unwrapTypedefs() is VoidType) {
            KotlinTypes.unit
        } else {
            stubGenerator.mirror(returnType).argType
        }.render(stubGenerator.kotlinFile)

        objCMethodAnnotations = buildObjCMethodAnnotations("@ObjCMethod")

        val joinedKotlinParameters = kotlinMethodParameters.renderParameters(stubGenerator.kotlinFile)

        this.header = buildString {
            if (container !is ObjCProtocol) append("external ")
            val modality = when (container) {
                is ObjCClassOrProtocol -> if (method.isOverride(container)) {
                    "override "
                } else when (container) {
                    is ObjCClass -> "open "
                    is ObjCProtocol -> ""
                }
                is ObjCCategory -> ""
            }
            append(modality)

            append("fun ")
            if (container is ObjCCategory) {
                val receiverType = stubGenerator.declarationMapper
                        .getKotlinClassFor(container.clazz, isMeta = method.isClass).type
                        .render(stubGenerator.kotlinFile)

                append(receiverType)
                append('.')
            }
            append("${method.kotlinName.asSimpleName()}($joinedKotlinParameters): $kotlinReturnType")

            if (container is ObjCProtocol && method.isOptional) append(" = optional()")
        }
    }

    private fun buildObjCMethodAnnotations(main: String) = listOfNotNull(
            buildObjCMethodAnnotation(main),
            "@CCall.ConsumesReceiver".takeIf { method.nsConsumesSelf },
            "@CCall.ReturnsRetained".takeIf { method.nsReturnsRetained }
    )

    private fun buildObjCMethodAnnotation(annotation: String) = buildString {
        append(annotation)
        append('(')
        append(method.selector.quoteAsKotlinLiteral())
        append(", ")
        append(method.encoding.quoteAsKotlinLiteral())
        if (isStret) {
            append(", ")
            append("true")
        }
        append(')')
    }
}

private fun deprecatedInit(className: String, initParameterNames: List<String>, factory: Boolean): String {
    val replacement = if (factory) "$className.create" else className
    val replacementKind = if (factory) "factory method" else "constructor"
    val replaceWith = "$replacement(${initParameterNames.joinToString { it.asSimpleName() }})"

    return deprecated("Use $replacementKind instead", replaceWith)
}

private fun deprecated(message: String, replaceWith: String): String =
        "@Deprecated(${message.quoteAsKotlinLiteral()}, " +
                "ReplaceWith(${replaceWith.quoteAsKotlinLiteral()}), " +
                "DeprecationLevel.ERROR)"

private val ObjCContainer.classOrProtocol: ObjCClassOrProtocol
    get() = when (this) {
        is ObjCClassOrProtocol -> this
        is ObjCCategory -> this.clazz
    }

/**
 * objc_msgSend*_stret functions must be used when return value is returned through memory
 * pointed by implicit argument, which is passed on the register that would otherwise be used for receiver.
 *
 * The entire implementation is just the real ABI approximation which is enough for practical cases.
 */
private fun Type.isStret(target: KonanTarget): Boolean {
    val unwrappedType = this.unwrapTypedefs()
    return when (target) {
        KonanTarget.IOS_ARM64 ->
            false // On aarch64 stret is never the case, since an implicit argument gets passed on x8.

        KonanTarget.IOS_X64, KonanTarget.MACOS_X64 -> when (unwrappedType) {
            is RecordType -> unwrappedType.decl.def!!.size > 16 || this.hasUnalignedMembers()
            else -> false
        }
        KonanTarget.IOS_ARM32 -> {
            when (unwrappedType) {
                is RecordType -> !this.isIntegerLikeType()
                else -> false
            }
        }

        else -> error(target)
    }
}

private fun Type.isIntegerLikeType(): Boolean = when (this) {
    is RecordType -> {
        val def = this.decl.def
        if (def == null) {
            false
        } else {
            def.size <= 4 &&
                    def.members.all {
                        when (it) {
                            is BitField -> it.type.isIntegerLikeType()
                            is Field -> it.offset == 0L && it.type.isIntegerLikeType()
                            is IncompleteField -> false
                        }
                    }
        }
    }
    is ObjCPointer, is PointerType, CharType, is BoolType -> true
    is IntegerType -> this.size <= 4
    is Typedef -> this.def.aliased.isIntegerLikeType()
    is EnumType -> this.def.baseType.isIntegerLikeType()

    else -> false
}

private fun Type.hasUnalignedMembers(): Boolean = when (this) {
    is Typedef -> this.def.aliased.hasUnalignedMembers()
    is RecordType -> this.decl.def!!.let { def ->
        def.fields.any {
            !it.isAligned ||
                    // Check members of fields too:
                    it.type.hasUnalignedMembers()
        }
    }
    is ArrayType -> this.elemType.hasUnalignedMembers()
    else -> false

// TODO: should the recursive checks be made in indexer when computing `hasUnalignedFields`?
}

private val ObjCMethod.kotlinName: String
    get() {
        val candidate = selector.split(":").first()
        val trimmed = candidate.trimEnd('_')
        return if (trimmed == "equals" && parameters.size == 1
                || (trimmed == "hashCode" || trimmed == "toString") && parameters.size == 0) {
            candidate + "_"
        } else {
            candidate
        }
    }

private val ObjCClassOrProtocol.protocolsWithSupers: Sequence<ObjCProtocol>
    get() = this.protocols.asSequence().flatMap { sequenceOf(it) + it.protocolsWithSupers }

private val ObjCClassOrProtocol.immediateSuperTypes: Sequence<ObjCClassOrProtocol>
    get() {
        val baseClass = (this as? ObjCClass)?.baseClass
        if (baseClass != null) {
            return sequenceOf(baseClass) + this.protocols.asSequence()
        }

        return this.protocols.asSequence()
    }

private val ObjCClassOrProtocol.selfAndSuperTypes: Sequence<ObjCClassOrProtocol>
    get() = sequenceOf(this) + this.superTypes

private val ObjCClassOrProtocol.superTypes: Sequence<ObjCClassOrProtocol>
    get() = this.immediateSuperTypes.flatMap { it.selfAndSuperTypes }.distinct()

private fun ObjCClassOrProtocol.declaredMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.methods.asSequence().filter { it.isClass == isClass }

@Suppress("UNUSED_PARAMETER")
private fun Sequence<ObjCMethod>.inheritedTo(container: ObjCClassOrProtocol, isMeta: Boolean): Sequence<ObjCMethod> =
        this // TODO: exclude methods that are marked as unavailable in [container].

private fun ObjCClassOrProtocol.inheritedMethods(isClass: Boolean): Sequence<ObjCMethod> =
        this.immediateSuperTypes.flatMap { it.methodsWithInherited(isClass) }
                .distinctBy { it.selector }
                .inheritedTo(this, isClass)

private fun ObjCClassOrProtocol.methodsWithInherited(isClass: Boolean): Sequence<ObjCMethod> =
        (this.declaredMethods(isClass) + this.inheritedMethods(isClass)).distinctBy { it.selector }

private fun ObjCClass.getDesignatedInitializerSelectors(result: MutableSet<String>): Set<String> {
    // Note: Objective-C initializers act as usual methods and thus are inherited by subclasses.
    // Swift considers all super initializers to be available (unless otherwise specified explicitly),
    // but seems to consider them as non-designated if class declares its own ones explicitly.
    // Simulate the similar behaviour:
    val explicitlyDesignatedInitializers = this.methods.filter { it.isExplicitlyDesignatedInitializer && !it.isClass }

    if (explicitlyDesignatedInitializers.isNotEmpty()) {
        explicitlyDesignatedInitializers.mapTo(result) { it.selector }
    } else {
        this.declaredMethods(isClass = false).filter { it.isInit }.mapTo(result) { it.selector }
        this.baseClass?.getDesignatedInitializerSelectors(result)
    }

    this.superTypes.filterIsInstance<ObjCProtocol>()
            .flatMap { it.declaredMethods(isClass = false) }.filter { it.isInit }
            .mapTo(result) { it.selector }

    return result
}

private fun ObjCMethod.isOverride(container: ObjCClassOrProtocol): Boolean =
        container.superTypes.any { superType -> superType.methods.any(this::replaces) }

abstract class ObjCContainerStub(stubGenerator: StubGenerator,
                                 private val container: ObjCClassOrProtocol,
                                 protected val metaContainerStub: ObjCContainerStub?
) : KotlinStub {

    private val isMeta: Boolean get() = metaContainerStub == null

    private val methods: List<ObjCMethod>
    private val properties: List<ObjCProperty>

    private val protocolGetter: String?

    init {
        val superMethods = container.inheritedMethods(isMeta)

        // Add all methods declared in the class or protocol:
        var methods = container.declaredMethods(isMeta)

        // Exclude those which are identically declared in super types:
        methods -= superMethods

        // Add some special methods from super types:
        methods += superMethods.filter { it.returnsInstancetype() || it.isInit }

        // Add methods from adopted protocols that must be implemented according to Kotlin rules:
        if (container is ObjCClass) {
            methods += container.protocolsWithSupers.flatMap { it.declaredMethods(isMeta) }.filter { !it.isOptional }
        }

        // Add methods inherited from multiple supertypes that must be defined according to Kotlin rules:
        methods += container.immediateSuperTypes
                .flatMap { superType ->
                    val methodsWithInherited = superType.methodsWithInherited(isMeta).inheritedTo(container, isMeta)
                    // Select only those which are represented as non-abstract in Kotlin:
                    when (superType) {
                        is ObjCClass -> methodsWithInherited
                        is ObjCProtocol -> methodsWithInherited.filter { it.isOptional }
                    }
                }
                .groupBy { it.selector }
                .mapNotNull { (_, inheritedMethods) -> if (inheritedMethods.size > 1) inheritedMethods.first() else null }

        this.methods = methods.distinctBy { it.selector }.toList()

        this.properties = container.properties.filter { property ->
            property.getter.isClass == isMeta &&
                    // Select only properties that don't override anything:
                    superMethods.none { property.getter.replaces(it) || property.setter?.replaces(it) ?: false }
        }
    }

    private val designatedInitializerSelectors = if (container is ObjCClass && !isMeta) {
        container.getDesignatedInitializerSelectors(mutableSetOf())
    } else {
        emptySet()
    }

    private val methodToStub = methods.map {
        it to ObjCMethodStub(
                stubGenerator, it, container,
                isDesignatedInitializer = it.selector in designatedInitializerSelectors
        )
    }.toMap()

    private val methodStubs get() = methodToStub.values

    val propertyStubs = properties.mapNotNull {
        createObjCPropertyStub(stubGenerator, it, container, this.methodToStub)
    }

    private val classHeader: String

    init {
        val supers = mutableListOf<KotlinType>()

        if (container is ObjCClass) {
            val baseClass = container.baseClass
            val baseClassifier = if (baseClass != null) {
                stubGenerator.declarationMapper.getKotlinClassFor(baseClass, isMeta)
            } else {
                if (isMeta) KotlinTypes.objCObjectBaseMeta else KotlinTypes.objCObjectBase
            }

            supers.add(baseClassifier.type)
        }
        container.protocols.forEach {
            supers.add(stubGenerator.declarationMapper.getKotlinClassFor(it, isMeta).type)
        }

        if (supers.isEmpty()) {
            assert(container is ObjCProtocol)
            val classifier = if (isMeta) KotlinTypes.objCObjectMeta else KotlinTypes.objCObject
            supers.add(classifier.type)
        }

        val keywords = when (container) {
            is ObjCClass -> "open class"
            is ObjCProtocol -> "interface"
        }

        val supersString = supers.joinToString { it.render(stubGenerator.kotlinFile) }
        val classifier = stubGenerator.declarationMapper.getKotlinClassFor(container, isMeta)
        val name = stubGenerator.kotlinFile.declare(classifier)

        val externalObjCClassAnnotationName = "@ExternalObjCClass"

        val externalObjCClassAnnotation: String = when (container) {
            is ObjCProtocol -> {
                protocolGetter = if (metaContainerStub != null) {
                    metaContainerStub.protocolGetter!!
                } else {
                    val nativeBacked = object : NativeBacked {}
                    // TODO: handle the case when protocol getter stub can't be compiled.
                    genProtocolGetter(stubGenerator, nativeBacked, container)
                }

                externalObjCClassAnnotationName.applyToStrings(protocolGetter)
            }
            is ObjCClass -> {
                protocolGetter = null
                val binaryName = container.binaryName
                if (binaryName != null) {
                    externalObjCClassAnnotationName.applyToStrings("", binaryName)
                } else {
                    externalObjCClassAnnotationName
                }
            }
        }

        this.classHeader = "$externalObjCClassAnnotation $keywords $name : $supersString"
    }

    open fun generateBody(context: StubGenerationContext): Sequence<String> {
        var result = (propertyStubs.asSequence() + methodStubs.asSequence())
                .flatMap { sequenceOf("") + it.generate(context) }

        if (container is ObjCClass && methodStubs.none {
            it.method.isInit && it.method.parameters.isEmpty() && context.nativeBridges.isSupported(it)
        }) {
            // Always generate default constructor.
            // If it is not produced for an init method, then include it manually:
            result += sequenceOf("", "protected constructor() {}")
        }

        return result
    }

    override fun generate(context: StubGenerationContext): Sequence<String> = block(classHeader, generateBody(context))
}

open class ObjCClassOrProtocolStub(
        stubGenerator: StubGenerator,
        private val container: ObjCClassOrProtocol
) : ObjCContainerStub(
        stubGenerator,
        container,
        metaContainerStub = object : ObjCContainerStub(stubGenerator, container, metaContainerStub = null) {}
) {
    override fun generate(context: StubGenerationContext) =
            metaContainerStub!!.generate(context) + "" + super.generate(context)
}

class ObjCProtocolStub(stubGenerator: StubGenerator, protocol: ObjCProtocol) :
        ObjCClassOrProtocolStub(stubGenerator, protocol)

class ObjCClassStub(private val stubGenerator: StubGenerator, private val clazz: ObjCClass) :
        ObjCClassOrProtocolStub(stubGenerator, clazz) {

    override fun generateBody(context: StubGenerationContext): Sequence<String> {
        val companionSuper = stubGenerator.declarationMapper
                .getKotlinClassFor(clazz, isMeta = true).type
                .render(stubGenerator.kotlinFile)

        val objCClassType = KotlinTypes.objCClassOf.typeWith(
                stubGenerator.declarationMapper.getKotlinClassFor(clazz, isMeta = false).type
        ).render(stubGenerator.kotlinFile)

        return sequenceOf( "companion object : $companionSuper(), $objCClassType {}") +
                super.generateBody(context)
    }
}

class GeneratedObjCCategoriesMembers {
    private val propertyNames = mutableSetOf<String>()
    private val instanceMethodSelectors = mutableSetOf<String>()
    private val classMethodSelectors = mutableSetOf<String>()

    fun register(method: ObjCMethod): Boolean =
            (if (method.isClass) classMethodSelectors else instanceMethodSelectors).add(method.selector)

    fun register(property: ObjCProperty): Boolean = propertyNames.add(property.name)

}

class ObjCCategoryStub(
        private val stubGenerator: StubGenerator, private val category: ObjCCategory
) : KotlinStub {

    private val generatedMembers = stubGenerator.generatedObjCCategoriesMembers
            .getOrPut(category.clazz, { GeneratedObjCCategoriesMembers() })

    // TODO: consider removing members that are also present in the class or its supertypes.

    private val methodToStub = category.methods.filter { generatedMembers.register(it) }.map {
        it to ObjCMethodStub(stubGenerator, it, category, isDesignatedInitializer = false)
    }.toMap()

    private val methodStubs get() = methodToStub.values

    private val propertyStubs = category.properties.filter { generatedMembers.register(it) }.mapNotNull {
        createObjCPropertyStub(stubGenerator, it, category, methodToStub)
    }

    override fun generate(context: StubGenerationContext): Sequence<String> {
        val description = "${category.clazz.name} (${category.name})"
        return sequenceOf("// @interface $description") +
                propertyStubs.asSequence().flatMap { sequenceOf("") + it.generate(context) } +
                methodStubs.asSequence().flatMap { sequenceOf("") + it.generate(context) } +
                sequenceOf("// @end; // $description")
    }
}

private fun createObjCPropertyStub(
        stubGenerator: StubGenerator,
        property: ObjCProperty,
        container: ObjCContainer,
        methodToStub: Map<ObjCMethod, ObjCMethodStub>
): ObjCPropertyStub? {
    // Note: the code below assumes that if the property is generated,
    // then its accessors are also generated as explicit methods.
    val getterStub = methodToStub[property.getter] ?: return null
    val setterStub = property.setter?.let { methodToStub[it] ?: return null }
    return ObjCPropertyStub(stubGenerator, property, container, getterStub, setterStub)
}

class ObjCPropertyStub(
        val stubGenerator: StubGenerator, val property: ObjCProperty, val container: ObjCContainer,
        val getterStub: ObjCMethodStub, val setterStub: ObjCMethodStub?
) : KotlinStub {

    override fun generate(context: StubGenerationContext): Sequence<String> {
        val type = property.getType(container.classOrProtocol)

        val kotlinType = stubGenerator.mirror(type).argType.render(stubGenerator.kotlinFile)

        val kind = if (property.setter == null) "val" else "var"
        val modifiers = if (container is ObjCProtocol) "final " else ""
        val receiver = when (container) {
            is ObjCClassOrProtocol -> ""
            is ObjCCategory -> stubGenerator.declarationMapper
                    .getKotlinClassFor(container.clazz, isMeta = property.getter.isClass).type
                    .render(stubGenerator.kotlinFile) + "."
        }
        val result = mutableListOf(
                "$modifiers$kind $receiver${property.name.asSimpleName()}: $kotlinType",
                "    ${getterStub.objCMethodAnnotations.joinToString(" ")} external get"
        )

        property.setter?.let {
            result.add("    ${setterStub!!.objCMethodAnnotations.joinToString(" ")} external set")
        }

        return result.asSequence()
    }

}

fun ObjCClassOrProtocol.kotlinClassName(isMeta: Boolean): String {
    val baseClassName = when (this) {
        is ObjCClass -> this.name
        is ObjCProtocol -> "${this.name}Protocol"
    }

    return if (isMeta) "${baseClassName}Meta" else baseClassName
}

private fun genProtocolGetter(
        stubGenerator: StubGenerator,
        nativeBacked: NativeBacked,
        protocol: ObjCProtocol
): String {
    val functionName = "kniprot_" + stubGenerator.pkgName.replace('.', '_') + stubGenerator.nextUniqueId()

    val builder = NativeCodeBuilder(stubGenerator.simpleBridgeGenerator.topLevelNativeScope)

    with(builder) {
        out("Protocol* $functionName() {")
        out("    return @protocol(${protocol.name});")
        out("}")
    }

    stubGenerator.simpleBridgeGenerator.insertNativeBridge(nativeBacked, emptyList(), builder.lines)

    return functionName
}
