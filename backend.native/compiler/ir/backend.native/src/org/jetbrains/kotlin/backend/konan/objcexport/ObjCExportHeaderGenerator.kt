/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.common.serialization.findSourceFile
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns.isAny
import org.jetbrains.kotlin.builtins.getReceiverTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getReturnTypeFromFunctionType
import org.jetbrains.kotlin.builtins.getValueParameterTypesFromFunctionType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.deprecation.Deprecation
import org.jetbrains.kotlin.resolve.deprecation.DeprecationLevelValue
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.types.typeUtil.isInterface
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.addIfNotNull

interface ObjCExportTranslator {
    fun generateBaseDeclarations(): List<ObjCTopLevel<*>>
    fun getClassIfExtension(receiverType: KotlinType): ClassDescriptor?
    fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface
    fun translateClass(descriptor: ClassDescriptor): ObjCInterface
    fun translateInterface(descriptor: ClassDescriptor): ObjCProtocol
    fun translateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>): ObjCInterface
}

interface ObjCExportWarningCollector {
    fun reportWarning(text: String)
    fun reportWarning(method: FunctionDescriptor, text: String)

    object SILENT : ObjCExportWarningCollector {
        override fun reportWarning(text: String) {}
        override fun reportWarning(method: FunctionDescriptor, text: String) {}
    }
}

internal class ObjCExportTranslatorImpl(
        private val generator: ObjCExportHeaderGenerator?,
        val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val warningCollector: ObjCExportWarningCollector,
        val objcGenerics: Boolean
) : ObjCExportTranslator {

    private val kotlinAnyName = namer.kotlinAnyName

    override fun generateBaseDeclarations(): List<ObjCTopLevel<*>> {
        val stubs = mutableListOf<ObjCTopLevel<*>>()

        stubs.add(objCInterface(namer.kotlinAnyName, superClass = "NSObject", members = buildMembers {
            +ObjCMethod(null, true, ObjCInstanceType, listOf("init"), emptyList(), listOf("unavailable"))
            +ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable"))
            +ObjCMethod(null, false, ObjCVoidType, listOf("initialize"), emptyList(), listOf("objc_requires_super"))
        }))

        // TODO: add comment to the header.
        stubs.add(ObjCInterfaceImpl(
                namer.kotlinAnyName.objCName,
                superProtocols = listOf("NSCopying"),
                categoryName = "${namer.kotlinAnyName.objCName}Copying"
        ))

        // TODO: only if appears
        stubs.add(objCInterface(
                namer.mutableSetName,
                generics = listOf("ObjectType"),
                superClass = "NSMutableSet<ObjectType>"
        ))

        // TODO: only if appears
        stubs.add(objCInterface(
                namer.mutableMapName,
                generics = listOf("KeyType", "ObjectType"),
                superClass = "NSMutableDictionary<KeyType, ObjectType>"
        ))

        stubs.add(ObjCInterfaceImpl("NSError", categoryName = "NSErrorKotlinException", members = buildMembers {
            +ObjCProperty("kotlinException", null, ObjCNullableReferenceType(ObjCIdType), listOf("readonly"))
        }))

        genKotlinNumbers(stubs)

        return stubs
    }

    private fun genKotlinNumbers(stubs: MutableList<ObjCTopLevel<*>>) {
        val members = buildMembers {
            NSNumberKind.values().forEach {
                +nsNumberFactory(it, listOf("unavailable"))
            }
            NSNumberKind.values().forEach {
                +nsNumberInit(it, listOf("unavailable"))
            }
        }
        stubs.add(objCInterface(
                namer.kotlinNumberName,
                superClass = "NSNumber",
                members = members
        ))

        NSNumberKind.values().forEach {
            if (it.mappedKotlinClassId != null) {
                stubs += genKotlinNumber(it.mappedKotlinClassId, it)
            }
        }
    }

    private fun genKotlinNumber(kotlinClassId: ClassId, kind: NSNumberKind): ObjCInterface {
        val name = namer.numberBoxName(kotlinClassId)

        val members = buildMembers {
            +nsNumberFactory(kind)
            +nsNumberInit(kind)
        }
        return objCInterface(
                name,
                superClass = namer.kotlinNumberName.objCName,
                members = members
        )
    }

    private fun nsNumberInit(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
        return ObjCMethod(
                null,
                false,
                ObjCInstanceType,
                listOf(kind.factorySelector),
                listOf(ObjCParameter("value", null, kind.objCType)),
                attributes
        )
    }

    private fun nsNumberFactory(kind: NSNumberKind, attributes: List<String> = emptyList()): ObjCMethod {
        return ObjCMethod(
                null,
                true,
                ObjCInstanceType,
                listOf(kind.initSelector),
                listOf(ObjCParameter("value", null, kind.objCType)),
                attributes
        )
    }

    override fun getClassIfExtension(receiverType: KotlinType): ClassDescriptor? =
            mapper.getClassIfCategory(receiverType)

    internal fun translateUnexposedClassAsUnavailableStub(descriptor: ClassDescriptor): ObjCInterface = objCInterface(
            namer.getClassOrProtocolName(descriptor),
            descriptor = descriptor,
            superClass = "NSObject",
            attributes = attributesForUnexposed(descriptor)
    )

    internal fun translateUnexposedInterfaceAsUnavailableStub(descriptor: ClassDescriptor): ObjCProtocol = objCProtocol(
            namer.getClassOrProtocolName(descriptor),
            descriptor = descriptor,
            superProtocols = emptyList(),
            members = emptyList(),
            attributes = attributesForUnexposed(descriptor)
    )

    private fun attributesForUnexposed(descriptor: ClassDescriptor): List<String> {
        val message = when {
            descriptor.isKotlinObjCClass() -> "Kotlin subclass of Objective-C class "
            else -> ""
        } + "can't be imported"
        return listOf("unavailable(\"$message\")")
    }

    private fun genericExportScope(classDescriptor: DeclarationDescriptor): ObjCExportScope {
        return if(objcGenerics && classDescriptor is ClassDescriptor && !classDescriptor.isInterface) {
            ObjCClassExportScope(classDescriptor, namer)
        } else {
            ObjCNoneExportScope
        }
    }

    private fun referenceClass(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        fun forwardDeclarationObjcClassName(objcGenerics: Boolean, descriptor: ClassDescriptor, namer:ObjCExportNamer): String {
            val className = translateClassOrInterfaceName(descriptor)
            val builder = StringBuilder(className.objCName)
            if (objcGenerics)
                formatGenerics(builder, descriptor.typeConstructor.parameters.map { typeParameterDescriptor ->
                    "${typeParameterDescriptor.variance.objcDeclaration()}${namer.getTypeParameterName(typeParameterDescriptor)}"
                })
            return builder.toString()
        }

        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        assert(!descriptor.isInterface)
        generator?.requireClassOrInterface(descriptor)

        return translateClassOrInterfaceName(descriptor).also {
            val objcName = forwardDeclarationObjcClassName(objcGenerics, descriptor, namer)
            generator?.referenceClass(objcName, descriptor)
        }
    }

    private fun referenceProtocol(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        assert(descriptor.isInterface)
        generator?.requireClassOrInterface(descriptor)

        return translateClassOrInterfaceName(descriptor).also {
            generator?.referenceProtocol(it.objCName, descriptor)
        }
    }

    private fun translateClassOrInterfaceName(descriptor: ClassDescriptor): ObjCExportNamer.ClassOrProtocolName {
        assert(mapper.shouldBeExposed(descriptor)) { "Shouldn't be exposed: $descriptor" }
        if (ErrorUtils.isError(descriptor)) {
            return ObjCExportNamer.ClassOrProtocolName("ERROR", "ERROR")
        }

        return namer.getClassOrProtocolName(descriptor)
    }

    override fun translateInterface(descriptor: ClassDescriptor): ObjCProtocol {
        require(descriptor.isInterface)
        if (!mapper.shouldBeExposed(descriptor)) {
            return translateUnexposedInterfaceAsUnavailableStub(descriptor)
        }

        val name = translateClassOrInterfaceName(descriptor)
        val members: List<Stub<*>> = buildMembers { translateInterfaceMembers(descriptor) }
        val superProtocols: List<String> = descriptor.superProtocols

        return objCProtocol(name, descriptor, superProtocols, members)
    }

    private val ClassDescriptor.superProtocols: List<String>
        get() =
            getSuperInterfaces()
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .map {
                        generator?.generateInterface(it)
                        referenceProtocol(it).objCName
                    }
                    .toList()

    override fun translateExtensions(
            classDescriptor: ClassDescriptor,
            declarations: List<CallableMemberDescriptor>
    ): ObjCInterface {
        generator?.generateClass(classDescriptor)

        val name = referenceClass(classDescriptor).objCName
        val members = buildMembers {
            translatePlainMembers(declarations, ObjCNoneExportScope)
        }
        return ObjCInterfaceImpl(name, categoryName = "Extensions", members = members)
    }

    override fun translateFile(file: SourceFile, declarations: List<CallableMemberDescriptor>): ObjCInterface {
        val name = namer.getFileClassName(file)

        // TODO: stop inheriting KotlinBase.
        val members = buildMembers {
            translatePlainMembers(declarations, ObjCNoneExportScope)
        }
        return objCInterface(
                name,
                superClass = namer.kotlinAnyName.objCName,
                members = members,
                attributes = listOf(OBJC_SUBCLASSING_RESTRICTED)
        )
    }

    override fun translateClass(descriptor: ClassDescriptor): ObjCInterface {
        require(!descriptor.isInterface)
        if (!mapper.shouldBeExposed(descriptor)) {
            return translateUnexposedClassAsUnavailableStub(descriptor)
        }

        val genericExportScope = genericExportScope(descriptor)

        fun superClassGenerics(genericExportScope: ObjCExportScope): List<ObjCNonNullReferenceType> {
            val parentType = computeSuperClassType(descriptor)
            return if(parentType != null) {
                parentType.arguments.map { typeProjection ->
                    mapReferenceTypeIgnoringNullability(typeProjection.type, genericExportScope)
                }
            } else {
                emptyList()
            }
        }

        val superClass = descriptor.getSuperClassNotAny()

        val superName = if (superClass == null) {
            kotlinAnyName
        } else {
            generator?.generateClass(superClass)
            referenceClass(superClass)
        }

        val superProtocols: List<String> = descriptor.superProtocols
        val members: List<Stub<*>> = buildMembers {
            val presentConstructors = mutableSetOf<String>()

            descriptor.constructors
                    .asSequence()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val selector = getSelector(it)
                        if (!descriptor.isArray) presentConstructors += selector

                        +buildMethod(it, it, genericExportScope)
                        exportThrown(it)
                        if (selector == "init") {
                            +ObjCMethod(it, false, ObjCInstanceType, listOf("new"), emptyList(),
                                    listOf("availability(swift, unavailable, message=\"use object initializers instead\")"))
                        }
                    }

            if (descriptor.isArray || descriptor.kind == ClassKind.OBJECT || descriptor.kind == ClassKind.ENUM_CLASS) {
                +ObjCMethod(null, false, ObjCInstanceType, listOf("alloc"), emptyList(), listOf("unavailable"))

                val parameter = ObjCParameter("zone", null, ObjCRawType("struct _NSZone *"))
                +ObjCMethod(descriptor, false, ObjCInstanceType, listOf("allocWithZone:"), listOf(parameter), listOf("unavailable"))
            }

            // TODO: consider adding exception-throwing impls for these.
            when (descriptor.kind) {
                ClassKind.OBJECT -> {
                    +ObjCMethod(
                            null, false, ObjCInstanceType,
                            listOf(namer.getObjectInstanceSelector(descriptor)), emptyList(),
                            listOf(swiftNameAttribute("init()"))
                    )
                }
                ClassKind.ENUM_CLASS -> {
                    val type = mapType(descriptor.defaultType, ReferenceBridge, ObjCNoneExportScope)

                    descriptor.enumEntries.forEach {
                        val entryName = namer.getEnumEntrySelector(it)
                        +ObjCProperty(entryName, null, type, listOf("class", "readonly"),
                                declarationAttributes = listOf(swiftNameAttribute(entryName)))
                    }
                }
                else -> {
                    // Nothing special.
                }
            }

            // Hide "unimplemented" super constructors:
            superClass?.constructors
                    ?.asSequence()
                    ?.filter { mapper.shouldBeExposed(it) }
                    ?.forEach {
                        val selector = getSelector(it)
                        if (selector !in presentConstructors) {
                            +buildMethod(it, it, ObjCNoneExportScope, unavailable = true)

                            if (selector == "init") {
                                +ObjCMethod(null, false, ObjCInstanceType, listOf("new"), emptyList(), listOf("unavailable"))
                            }

                            // TODO: consider adding exception-throwing impls for these.
                        }
                    }

            translateClassMembers(descriptor)
        }

        val attributes = if (descriptor.isFinalOrEnum) listOf(OBJC_SUBCLASSING_RESTRICTED) else emptyList()

        val name = translateClassOrInterfaceName(descriptor)

        val generics = if (objcGenerics) {
            descriptor.typeConstructor.parameters.map {
                "${it.variance.objcDeclaration()}${namer.getTypeParameterName(it)}"
            }
        } else {
            emptyList()
        }

        val superClassGenerics = if (objcGenerics) {
            superClassGenerics(genericExportScope)
        } else {
            emptyList()
        }

        return objCInterface(
                name,
                generics = generics,
                descriptor = descriptor,
                superClass = superName.objCName,
                superClassGenerics = superClassGenerics,
                superProtocols = superProtocols,
                members = members,
                attributes = attributes
        )
    }

    private fun ClassDescriptor.getExposedMembers(): List<CallableMemberDescriptor> =
            this.unsubstitutedMemberScope.getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .toList()

    private fun StubBuilder.translateClassMembers(descriptor: ClassDescriptor) {
        require(!descriptor.isInterface)
        translateClassMembers(descriptor.getExposedMembers())
    }

    private fun StubBuilder.translateInterfaceMembers(descriptor: ClassDescriptor) {
        require(descriptor.isInterface)
        translateBaseMembers(descriptor.getExposedMembers())
    }

    private class RenderedStub<T: Stub<*>>(val stub: T) {
        private val presentation: String by lazy(LazyThreadSafetyMode.NONE) {
            val listOfLines = StubRenderer.render(stub)
            assert(listOfLines.size == 1)
            listOfLines[0]
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            return other is RenderedStub<*> && presentation == other.presentation
        }

        override fun hashCode(): Int {
            return presentation.hashCode()
        }
    }

    private fun List<CallableMemberDescriptor>.toObjCMembers(
            methodsBuffer: MutableList<FunctionDescriptor>,
            propertiesBuffer: MutableList<PropertyDescriptor>
    ) = this.forEach {
        when (it) {
            is FunctionDescriptor -> methodsBuffer += it
            is PropertyDescriptor -> if (mapper.isObjCProperty(it)) {
                propertiesBuffer += it
            } else {
                methodsBuffer.addIfNotNull(it.getter)
                methodsBuffer.addIfNotNull(it.setter)
            }
            else -> error(it)
        }
    }

    private fun StubBuilder.translateClassMembers(members: List<CallableMemberDescriptor>) {
        // TODO: add some marks about modality.

        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        collectMethodsOrProperties(methods) { it -> buildAsDeclaredOrInheritedMethods(it.original) }
        collectMethodsOrProperties(properties) { it -> buildAsDeclaredOrInheritedProperties(it.original) }
    }

    private fun StubBuilder.translateBaseMembers(members: List<CallableMemberDescriptor>) {
        // TODO: add some marks about modality.

        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        methods.retainAll { mapper.isBaseMethod(it) }

        properties.retainAll {
            if (mapper.isBaseProperty(it)) {
                true
            } else {
                methods.addIfNotNull(it.setter?.takeIf(mapper::isBaseMethod))
                false
            }
        }

        translatePlainMembers(methods, properties, ObjCNoneExportScope)
    }

    private fun StubBuilder.translatePlainMembers(members: List<CallableMemberDescriptor>, objCExportScope: ObjCExportScope) {
        val methods = mutableListOf<FunctionDescriptor>()
        val properties = mutableListOf<PropertyDescriptor>()

        members.toObjCMembers(methods, properties)

        methods.forEach { exportThrown(it) }

        translatePlainMembers(methods, properties, objCExportScope)
    }

    private fun StubBuilder.translatePlainMembers(methods: List<FunctionDescriptor>, properties: List<PropertyDescriptor>, objCExportScope: ObjCExportScope) {
        methods.forEach { +buildMethod(it, it, objCExportScope) }
        properties.forEach { +buildProperty(it, it, objCExportScope) }
    }

    private fun <D : CallableMemberDescriptor, S : Stub<*>> StubBuilder.collectMethodsOrProperties(
            members: List<D>,
            converter: (D) -> Set<RenderedStub<S>>) {
        members.forEach { member ->
            val memberStubs = converter(member).asSequence()

            val filteredMemberStubs = if (member.kind.isReal) {
                memberStubs
            } else {
                val superMembers: Set<RenderedStub<S>> = (member.overriddenDescriptors as Collection<D>)
                        .asSequence()
                        .filter { mapper.shouldBeExposed(it) }
                        .flatMap { converter(it).asSequence() }
                        .toSet()

                memberStubs.filterNot { superMembers.contains(it) }
            }

            this += filteredMemberStubs
                    .map { rendered -> rendered.stub }
                    .toList()
        }
    }

    private fun buildAsDeclaredOrInheritedMethods(
            method: FunctionDescriptor
    ): Set<RenderedStub<ObjCMethod>> {
        val isInterface = (method.containingDeclaration as ClassDescriptor).isInterface

        return mapper.getBaseMethods(method)
                .asSequence()
                .distinctBy { namer.getSelector(it) }
                .map { base -> buildMethod((if (isInterface) base else method), base, genericExportScope(method.containingDeclaration)) }
                .map { RenderedStub(it) }
                .toSet()
    }

    private fun buildAsDeclaredOrInheritedProperties(
            property: PropertyDescriptor
    ): Set<RenderedStub<ObjCProperty>> {
        val isInterface = (property.containingDeclaration as ClassDescriptor).isInterface

        return mapper.getBaseProperties(property)
                .asSequence()
                .distinctBy { namer.getPropertyName(it) }
                .map { base -> buildProperty((if (isInterface) base else property), base, genericExportScope(property.containingDeclaration)) }
                .map { RenderedStub(it) }
                .toSet()
    }

    // TODO: consider checking that signatures for bases with same selector/name are equal.

    private fun getSelector(method: FunctionDescriptor): String {
        return namer.getSelector(method)
    }

    private fun buildProperty(property: PropertyDescriptor, baseProperty: PropertyDescriptor, objCExportScope: ObjCExportScope): ObjCProperty {
        assert(mapper.isBaseProperty(baseProperty))
        assert(mapper.isObjCProperty(baseProperty))

        val getterBridge = mapper.bridgeMethod(baseProperty.getter!!)
        val type = mapReturnType(getterBridge.returnBridge, property.getter!!, objCExportScope)
        val name = namer.getPropertyName(baseProperty)

        val attributes = mutableListOf<String>()

        if (!getterBridge.isInstance) {
            attributes += "class"
        }

        val setterName: String?
        val propertySetter = property.setter
        if (propertySetter != null && mapper.shouldBeExposed(propertySetter)) {
            val setterSelector = mapper.getBaseMethods(propertySetter).map { namer.getSelector(it) }.distinct().single()
            setterName = if (setterSelector != "set" + name.capitalize() + ":") setterSelector else null
        } else {
            attributes += "readonly"
            setterName = null
        }

        val getterSelector = getSelector(baseProperty.getter!!)
        val getterName: String? = if (getterSelector != name) getterSelector else null

        val declarationAttributes = mutableListOf(swiftNameAttribute(name))
        declarationAttributes.addIfNotNull(mapper.getDeprecation(property)?.toDeprecationAttribute())

        return ObjCProperty(name, property, type, attributes, setterName, getterName, declarationAttributes)
    }

    private fun buildMethod(
            method: FunctionDescriptor,
            baseMethod: FunctionDescriptor,
            objCExportScope: ObjCExportScope,
            unavailable: Boolean = false
    ): ObjCMethod {
        fun collectParameters(baseMethodBridge: MethodBridge, method: FunctionDescriptor): List<ObjCParameter> {
            fun unifyName(initialName: String, usedNames: Set<String>): String {
                var unique = initialName.toValidObjCSwiftIdentifier()
                while (unique in usedNames || unique in cKeywords) {
                    unique += "_"
                }
                return unique
            }

            val valueParametersAssociated = baseMethodBridge.valueParametersAssociated(method)

            val parameters = mutableListOf<ObjCParameter>()

            val usedNames = mutableSetOf<String>()

            valueParametersAssociated.forEach { (bridge: MethodBridgeValueParameter, p: ParameterDescriptor?) ->
                val candidateName: String = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> {
                        p!!
                        when {
                            p is ReceiverParameterDescriptor -> "receiver"
                            method is PropertySetterDescriptor -> "value"
                            else -> p.name.asString()
                        }
                    }
                    MethodBridgeValueParameter.ErrorOutParameter -> "error"
                    is MethodBridgeValueParameter.KotlinResultOutParameter -> "result"
                }

                val uniqueName = unifyName(candidateName, usedNames)
                usedNames += uniqueName

                val type = when (bridge) {
                    is MethodBridgeValueParameter.Mapped -> mapType(p!!.type, bridge.bridge, objCExportScope)
                    MethodBridgeValueParameter.ErrorOutParameter ->
                        ObjCPointerType(ObjCNullableReferenceType(ObjCClassType("NSError")), nullable = true)

                    is MethodBridgeValueParameter.KotlinResultOutParameter -> {
                        val resultType = mapType(method.returnType!!, bridge.bridge, objCExportScope)
                        // Note: non-nullable reference or pointer type is unusable here
                        // when passing reference to local variable from Swift because it
                        // would require a non-null initializer then.
                        val pointeeType = resultType.makeNullableIfReferenceOrPointer()
                        ObjCPointerType(pointeeType, nullable = true)
                    }
                }

                parameters += ObjCParameter(uniqueName, p, type)
            }
            return parameters
        }

        assert(mapper.isBaseMethod(baseMethod))

        val baseMethodBridge = mapper.bridgeMethod(baseMethod)

        val isInstanceMethod: Boolean = baseMethodBridge.isInstance
        val returnType: ObjCType = mapReturnType(baseMethodBridge.returnBridge, method, objCExportScope)
        val parameters = collectParameters(baseMethodBridge, method)
        val selector = getSelector(baseMethod)
        val selectorParts: List<String> = splitSelector(selector)
        val swiftName = namer.getSwiftName(baseMethod)
        val attributes = mutableListOf<String>()

        attributes += swiftNameAttribute(swiftName)

        if (method is ConstructorDescriptor && !method.constructedClass.isArray) { // TODO: check methodBridge instead.
            attributes += "objc_designated_initializer"
        }

        if (unavailable) {
            attributes += "unavailable"
        } else {
            attributes.addIfNotNull(mapper.getDeprecation(method)?.toDeprecationAttribute())
        }

        return ObjCMethod(method, isInstanceMethod, returnType, selectorParts, parameters, attributes)
    }

    private fun splitSelector(selector: String): List<String> {
        return if (!selector.endsWith(":")) {
            listOf(selector)
        } else {
            selector.trimEnd(':').split(':').map { "$it:" }
        }
    }

    private val uncheckedExceptionClasses =
            listOf("kotlin.Error", "kotlin.RuntimeException").map { ClassId.topLevel(FqName(it)) }

    private fun exportThrown(method: FunctionDescriptor) {
        if (!method.kind.isReal) return
        val throwsAnnotation = method.annotations.findAnnotation(KonanFqNames.throws) ?: return

        if (!mapper.doesThrow(method)) {
            warningCollector.reportWarning(method,
                    "@${KonanFqNames.throws.shortName()} annotation should also be added to a base method")
        }

        val arguments = (throwsAnnotation.allValueArguments.values.single() as ArrayValue).value
        for (argument in arguments) {
            val classDescriptor = TypeUtils.getClassDescriptor((argument as KClassValue).getArgumentType(method.module)) ?: continue

            classDescriptor.getAllSuperClassifiers().firstOrNull { it.classId in uncheckedExceptionClasses }?.let {
                warningCollector.reportWarning(method,
                        "Method is declared to throw ${classDescriptor.fqNameSafe}, " +
                                "but instances of ${it.fqNameSafe} and its subclasses aren't propagated " +
                                "from Kotlin to Objective-C/Swift")
            }

            generator?.requireClassOrInterface(classDescriptor)
        }
    }

    private fun mapReturnType(returnBridge: MethodBridge.ReturnValue, method: FunctionDescriptor, objCExportScope: ObjCExportScope): ObjCType = when (returnBridge) {
        MethodBridge.ReturnValue.Void -> ObjCVoidType
        MethodBridge.ReturnValue.HashCode -> ObjCPrimitiveType("NSUInteger")
        is MethodBridge.ReturnValue.Mapped -> mapType(method.returnType!!, returnBridge.bridge, objCExportScope)
        MethodBridge.ReturnValue.WithError.Success -> ObjCPrimitiveType("BOOL")
        is MethodBridge.ReturnValue.WithError.RefOrNull -> {
            val successReturnType = mapReturnType(returnBridge.successBridge, method, objCExportScope) as? ObjCNonNullReferenceType
                    ?: error("Function is expected to have non-null return type: $method")

            ObjCNullableReferenceType(successReturnType)
        }

        MethodBridge.ReturnValue.Instance.InitResult,
        MethodBridge.ReturnValue.Instance.FactoryResult -> ObjCInstanceType
    }

    internal fun mapReferenceType(kotlinType: KotlinType, objCExportScope: ObjCExportScope): ObjCReferenceType =
            mapReferenceTypeIgnoringNullability(kotlinType, objCExportScope).withNullabilityOf(kotlinType)

    private fun ObjCNonNullReferenceType.withNullabilityOf(kotlinType: KotlinType): ObjCReferenceType =
            if (kotlinType.binaryRepresentationIsNullable()) {
                ObjCNullableReferenceType(this)
            } else {
                this
            }

    internal fun mapReferenceTypeIgnoringNullability(kotlinType: KotlinType, objCExportScope: ObjCExportScope): ObjCNonNullReferenceType {
        class TypeMappingMatch(val type: KotlinType, val descriptor: ClassDescriptor, val mapper: CustomTypeMapper)

        val typeMappingMatches = (listOf(kotlinType) + kotlinType.supertypes()).mapNotNull { type ->
            (type.constructor.declarationDescriptor as? ClassDescriptor)?.let { descriptor ->
                mapper.getCustomTypeMapper(descriptor)?.let { mapper ->
                    TypeMappingMatch(type, descriptor, mapper)
                }
            }
        }

        val mostSpecificMatches = typeMappingMatches.filter { match ->
            typeMappingMatches.all { otherMatch ->
                otherMatch.descriptor == match.descriptor ||
                        !otherMatch.descriptor.isSubclassOf(match.descriptor)
            }
        }

        if (mostSpecificMatches.size > 1) {
            val types = mostSpecificMatches.map { it.type }
            val firstType = types[0]
            val secondType = types[1]

            warningCollector.reportWarning(
                    "Exposed type '$kotlinType' is '$firstType' and '$secondType' at the same time. " +
                            "This most likely wouldn't work as expected.")

            // TODO: the same warning for such classes.
        }

        mostSpecificMatches.firstOrNull()?.let {
            return it.mapper.mapType(it.type, this, objCExportScope)
        }

        if(objcGenerics && kotlinType.isTypeParameter()){
            val genericTypeDeclaration = objCExportScope.getGenericDeclaration(TypeUtils.getTypeParameterDescriptorOrNull(kotlinType))
            if(genericTypeDeclaration != null)
                return genericTypeDeclaration
        }

        val classDescriptor = kotlinType.getErasedTypeClass()

        // TODO: translate `where T : BaseClass, T : SomeInterface` to `BaseClass* <SomeInterface>`

        // TODO: expose custom inline class boxes properly.
        if (isAny(classDescriptor) || classDescriptor.classId in mapper.hiddenTypes || classDescriptor.isInlined()) {
            return ObjCIdType
        }

        if (classDescriptor.defaultType.isObjCObjectType()) {
            return mapObjCObjectReferenceTypeIgnoringNullability(classDescriptor)
        }

        if (!mapper.shouldBeExposed(classDescriptor)) {
            // There are number of tricky corner cases getting here.
            return ObjCIdType
        }

        return if (classDescriptor.isInterface) {
            ObjCProtocolType(referenceProtocol(classDescriptor).objCName)
        } else {
            val typeArgs = if (objcGenerics) {
                kotlinType.arguments.map { typeProjection ->
                    mapReferenceTypeIgnoringNullability(typeProjection.type, objCExportScope)
                }
            } else {
                emptyList()
            }
            ObjCClassType(referenceClass(classDescriptor).objCName, typeArgs)
        }
    }

    private tailrec fun mapObjCObjectReferenceTypeIgnoringNullability(descriptor: ClassDescriptor): ObjCNonNullReferenceType {
        // TODO: more precise types can be used.

        if (descriptor.isObjCMetaClass()) return ObjCMetaClassType
        if (descriptor.isObjCProtocolClass()) return foreignClassType("Protocol")

        if (descriptor.isExternalObjCClass() || descriptor.isObjCForwardDeclaration()) {
            return if (descriptor.isInterface) {
                val name = descriptor.name.asString().removeSuffix("Protocol")
                foreignProtocolType(name)
            } else {
                val name = descriptor.name.asString()
                foreignClassType(name)
            }
        }

        if (descriptor.isKotlinObjCClass()) {
            return mapObjCObjectReferenceTypeIgnoringNullability(descriptor.getSuperClassOrAny())
        }

        return ObjCIdType
    }

    private fun foreignProtocolType(name: String): ObjCProtocolType {
        generator?.referenceProtocol(name)
        return ObjCProtocolType(name)
    }

    private fun foreignClassType(name: String): ObjCClassType {
        generator?.referenceClass(name)
        return ObjCClassType(name)
    }

    internal fun mapFunctionTypeIgnoringNullability(
            functionType: KotlinType,
            objCExportScope: ObjCExportScope,
            returnsVoid: Boolean
    ): ObjCBlockPointerType {
        val parameterTypes = listOfNotNull(functionType.getReceiverTypeFromFunctionType()) +
                functionType.getValueParameterTypesFromFunctionType().map { it.type }

        return ObjCBlockPointerType(
                if (returnsVoid) {
                    ObjCVoidType
                } else {
                    mapReferenceType(functionType.getReturnTypeFromFunctionType(), objCExportScope)
                },
                parameterTypes.map { mapReferenceType(it, objCExportScope) }
        )
    }

    private fun mapFunctionType(
            kotlinType: KotlinType,
            objCExportScope: ObjCExportScope,
            typeBridge: BlockPointerBridge
    ): ObjCReferenceType {
        val expectedDescriptor = kotlinType.builtIns.getFunction(typeBridge.numberOfParameters)

        // Somewhat similar to mapType:
        val functionType = if (TypeUtils.getClassDescriptor(kotlinType) == expectedDescriptor) {
            kotlinType
        } else {
            kotlinType.supertypes().singleOrNull { TypeUtils.getClassDescriptor(it) == expectedDescriptor }
                    ?: expectedDescriptor.defaultType // Should not happen though.
        }

        return mapFunctionTypeIgnoringNullability(functionType, objCExportScope, typeBridge.returnsVoid)
                .withNullabilityOf(kotlinType)
    }

    private fun mapType(kotlinType: KotlinType, typeBridge: TypeBridge, objCExportScope: ObjCExportScope): ObjCType = when (typeBridge) {
        ReferenceBridge -> mapReferenceType(kotlinType, objCExportScope)
        is BlockPointerBridge -> mapFunctionType(kotlinType, objCExportScope, typeBridge)
        is ValueTypeBridge -> {
            when (typeBridge.objCValueType) {
                ObjCValueType.BOOL -> ObjCPrimitiveType("BOOL")
                ObjCValueType.UNICHAR -> ObjCPrimitiveType("unichar")
                ObjCValueType.CHAR -> ObjCPrimitiveType("int8_t")
                ObjCValueType.SHORT -> ObjCPrimitiveType("int16_t")
                ObjCValueType.INT -> ObjCPrimitiveType("int32_t")
                ObjCValueType.LONG_LONG -> ObjCPrimitiveType("int64_t")
                ObjCValueType.UNSIGNED_CHAR -> ObjCPrimitiveType("uint8_t")
                ObjCValueType.UNSIGNED_SHORT -> ObjCPrimitiveType("uint16_t")
                ObjCValueType.UNSIGNED_INT -> ObjCPrimitiveType("uint32_t")
                ObjCValueType.UNSIGNED_LONG_LONG -> ObjCPrimitiveType("uint64_t")
                ObjCValueType.FLOAT -> ObjCPrimitiveType("float")
                ObjCValueType.DOUBLE -> ObjCPrimitiveType("double")
                ObjCValueType.POINTER -> ObjCPointerType(ObjCVoidType, kotlinType.binaryRepresentationIsNullable())
            }
            // TODO: consider other namings.
        }
    }
}

abstract class ObjCExportHeaderGenerator internal constructor(
        val moduleDescriptors: List<ModuleDescriptor>,
        internal val mapper: ObjCExportMapper,
        val namer: ObjCExportNamer,
        val objcGenerics:Boolean = false
) {

    constructor(
            moduleDescriptors: List<ModuleDescriptor>,
            builtIns: KotlinBuiltIns,
            topLevelNamePrefix: String
    ) : this(moduleDescriptors, builtIns, topLevelNamePrefix, ObjCExportMapper())

    private constructor(
            moduleDescriptors: List<ModuleDescriptor>,
            builtIns: KotlinBuiltIns,
            topLevelNamePrefix: String,
            mapper: ObjCExportMapper
    ) : this(
            moduleDescriptors,
            mapper,
            ObjCExportNamerImpl(moduleDescriptors.toSet(), builtIns, mapper, topLevelNamePrefix, local = false)
    )

    constructor(
            moduleDescriptor: ModuleDescriptor,
            builtIns: KotlinBuiltIns,
            topLevelNamePrefix: String = moduleDescriptor.namePrefix
    ) : this(moduleDescriptor, emptyList(), builtIns, topLevelNamePrefix)

    constructor(
            moduleDescriptor: ModuleDescriptor,
            exportedDependencies: List<ModuleDescriptor>,
            builtIns: KotlinBuiltIns,
            topLevelNamePrefix: String = moduleDescriptor.namePrefix
    ) : this(listOf(moduleDescriptor) + exportedDependencies, builtIns, topLevelNamePrefix)

    private val stubs = mutableListOf<Stub<*>>()

    private val classForwardDeclarations = linkedSetOf<String>()
    private val protocolForwardDeclarations = linkedSetOf<String>()
    private val extraClassesToTranslate = mutableSetOf<ClassDescriptor>()

    private val translator = ObjCExportTranslatorImpl(this, mapper, namer,
            object : ObjCExportWarningCollector {
                override fun reportWarning(text: String) =
                        this@ObjCExportHeaderGenerator.reportWarning(text)

                override fun reportWarning(method: FunctionDescriptor, text: String) =
                        this@ObjCExportHeaderGenerator.reportWarning(method, text)
            },
            objcGenerics)

    private val generatedClasses = mutableSetOf<ClassDescriptor>()
    private val extensions = mutableMapOf<ClassDescriptor, MutableList<CallableMemberDescriptor>>()
    private val topLevel = mutableMapOf<SourceFile, MutableList<CallableMemberDescriptor>>()

    fun build(): List<String> = mutableListOf<String>().apply {
        add("#import <Foundation/NSArray.h>")
        add("#import <Foundation/NSDictionary.h>")
        add("#import <Foundation/NSError.h>")
        add("#import <Foundation/NSObject.h>")
        add("#import <Foundation/NSSet.h>")
        add("#import <Foundation/NSString.h>")
        add("#import <Foundation/NSValue.h>")
        getAdditionalImports().forEach {
            add("#import <$it>")
        }
        add("")

        if (classForwardDeclarations.isNotEmpty()) {
            add("@class ${classForwardDeclarations.joinToString()};")
            add("")
        }

        if (protocolForwardDeclarations.isNotEmpty()) {
            add("@protocol ${protocolForwardDeclarations.joinToString()};")
            add("")
        }

        add("NS_ASSUME_NONNULL_BEGIN")
        add("#pragma clang diagnostic push")
        listOf(
                "-Wunknown-warning-option",
                "-Wnullability"
        ).forEach {
            add("#pragma clang diagnostic ignored \"$it\"")
        }
        add("")

        stubs.forEach {
            addAll(StubRenderer.render(it))
            add("")
        }

        add("#pragma clang diagnostic pop")
        add("NS_ASSUME_NONNULL_END")
    }

    internal fun buildInterface(): ObjCExportedInterface {
        val headerLines = build()
        return ObjCExportedInterface(generatedClasses, extensions, topLevel, headerLines, namer, mapper)
    }

    protected abstract fun reportWarning(text: String)

    protected abstract fun reportWarning(method: FunctionDescriptor, text: String)

    protected open fun getAdditionalImports(): List<String> = emptyList()


    fun translateModule(): List<Stub<*>> {
        // TODO: make the translation order stable
        // to stabilize name mangling.

        stubs += translator.generateBaseDeclarations()

        val packageFragments = moduleDescriptors.flatMap { it.getPackageFragments() }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<CallableMemberDescriptor>()
                    .filter { mapper.shouldBeExposed(it) }
                    .forEach {
                        val classDescriptor = mapper.getClassIfCategory(it)
                        if (classDescriptor != null) {
                            extensions.getOrPut(classDescriptor, { mutableListOf() }) += it
                        } else {
                            topLevel.getOrPut(it.findSourceFile(), { mutableListOf() }) += it
                        }
                    }

        }

        fun MemberScope.translateClasses() {
            getContributedDescriptors()
                    .asSequence()
                    .filterIsInstance<ClassDescriptor>()
                    .forEach {
                        if (mapper.shouldBeExposed(it)) {
                            if (it.isInterface) {
                                generateInterface(it)
                            } else {
                                generateClass(it)
                            }

                            it.unsubstitutedMemberScope.translateClasses()
                        } else if (mapper.shouldBeVisible(it)) {
                            stubs += if (it.isInterface) {
                                translator.translateUnexposedInterfaceAsUnavailableStub(it)
                            } else {
                                translator.translateUnexposedClassAsUnavailableStub(it)
                            }
                        }
                    }
        }

        packageFragments.forEach { packageFragment ->
            packageFragment.getMemberScope().translateClasses()
        }

        extensions.forEach { classDescriptor, declarations ->
            generateExtensions(classDescriptor, declarations)
        }

        topLevel.forEach { sourceFile, declarations ->
            generateFile(sourceFile, declarations)
        }

        while (extraClassesToTranslate.isNotEmpty()) {
            val descriptor = extraClassesToTranslate.first()
            extraClassesToTranslate -= descriptor
            if (descriptor.isInterface) {
                generateInterface(descriptor)
            } else {
                generateClass(descriptor)
            }
        }

        return stubs
    }

    private fun generateFile(sourceFile: SourceFile, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateFile(sourceFile, declarations))
    }

    private fun generateExtensions(classDescriptor: ClassDescriptor, declarations: List<CallableMemberDescriptor>) {
        stubs.add(translator.translateExtensions(classDescriptor, declarations))
    }

    internal fun generateClass(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        stubs.add(translator.translateClass(descriptor))
    }

    internal fun generateInterface(descriptor: ClassDescriptor) {
        if (!generatedClasses.add(descriptor)) return
        stubs.add(translator.translateInterface(descriptor))
    }

    internal fun requireClassOrInterface(descriptor: ClassDescriptor) {
        if (descriptor !in generatedClasses) {
            extraClassesToTranslate += descriptor
        }
    }

    internal fun referenceClass(objCName: String, descriptor: ClassDescriptor? = null) {
        classForwardDeclarations += objCName
    }

    internal fun referenceProtocol(objCName: String, descriptor: ClassDescriptor? = null) {
        protocolForwardDeclarations += objCName
    }
}

private fun objCInterface(
        name: ObjCExportNamer.ClassOrProtocolName,
        generics: List<String> = emptyList(),
        descriptor: ClassDescriptor? = null,
        superClass: String? = null,
        superClassGenerics: List<ObjCNonNullReferenceType> = emptyList(),
        superProtocols: List<String> = emptyList(),
        members: List<Stub<*>> = emptyList(),
        attributes: List<String> = emptyList()
): ObjCInterface = ObjCInterfaceImpl(
        name.objCName,
        generics,
        descriptor,
        superClass,
        superClassGenerics,
        superProtocols,
        null,
        members,
        attributes + name.toNameAttributes()
)

private fun objCProtocol(
        name: ObjCExportNamer.ClassOrProtocolName,
        descriptor: ClassDescriptor,
        superProtocols: List<String>,
        members: List<Stub<*>>,
        attributes: List<String> = emptyList()
): ObjCProtocol = ObjCProtocolImpl(
        name.objCName,
        descriptor,
        superProtocols,
        members,
        attributes + name.toNameAttributes()
)

internal fun ObjCExportNamer.ClassOrProtocolName.toNameAttributes(): List<String> = listOfNotNull(
        binaryName.takeIf { it != objCName }?.let { objcRuntimeNameAttribute(it) },
        swiftName.takeIf { it != objCName }?.let { swiftNameAttribute(it) }
)

private fun swiftNameAttribute(swiftName: String) = "swift_name(\"$swiftName\")"
private fun objcRuntimeNameAttribute(name: String) = "objc_runtime_name(\"$name\")"

interface ObjCExportScope{
    fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration?
}

internal class ObjCClassExportScope constructor(container:DeclarationDescriptor, val namer: ObjCExportNamer): ObjCExportScope {
    private val typeNames = if(container is ClassDescriptor && !container.isInterface) {
        container.typeConstructor.parameters
    } else {
        emptyList<TypeParameterDescriptor>()
    }

    override fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration? {
        val localTypeParam = typeNames.firstOrNull {
            typeParameterDescriptor != null &&
                    (it == typeParameterDescriptor || (it.isCapturedFromOuterDeclaration && it.original == typeParameterDescriptor))
        }

        return if(localTypeParam == null) {
            null
        } else {
            ObjCGenericTypeDeclaration(localTypeParam, namer)
        }
    }
}

internal object ObjCNoneExportScope: ObjCExportScope{
    override fun getGenericDeclaration(typeParameterDescriptor: TypeParameterDescriptor?): ObjCGenericTypeDeclaration? = null
}

internal fun Variance.objcDeclaration():String = when(this){
    Variance.OUT_VARIANCE -> "__covariant "
    Variance.IN_VARIANCE -> "__contravariant "
    else -> ""
}

private fun computeSuperClassType(descriptor: ClassDescriptor): KotlinType? = descriptor.typeConstructor.supertypes.filter { !it.isInterface() }.firstOrNull()

internal const val OBJC_SUBCLASSING_RESTRICTED = "objc_subclassing_restricted"

private fun Deprecation.toDeprecationAttribute(): String {
    val attribute = when (deprecationLevel) {
        DeprecationLevelValue.WARNING -> "deprecated"
        DeprecationLevelValue.ERROR, DeprecationLevelValue.HIDDEN -> "unavailable"
    }

    // TODO: consider avoiding code generation for unavailable.

    val message = this.message.orEmpty()

    return "$attribute(\"$message\")"
}
