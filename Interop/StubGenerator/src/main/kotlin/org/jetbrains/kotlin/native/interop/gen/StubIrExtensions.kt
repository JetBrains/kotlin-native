/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol

private val StubOrigin.ObjCMethod.isOptional: Boolean
    get() = container is ObjCProtocol && method.isOptional

fun FunctionStub.isOptionalObjCMethod(): Boolean = this.origin is StubOrigin.ObjCMethod &&
        this.origin.isOptional

val StubContainer.isInterface: Boolean
    get() = if (this is ClassStub.Simple) {
        modality == ClassStubModality.INTERFACE
    } else {
        false
    }

/**
 * Compute which names will be declared by [StubContainer] in the given [pkgName]
 */
fun StubContainer.computeNamesToBeDeclared(pkgName: String): List<String> {

    fun checkPackageCorrectness(classifier: Classifier) {
        assert(classifier.pkg == pkgName) {
            """Wrong classifier package. 
                |Expected: $pkgName
                |Got: ${classifier.pkg}
                |""".trimMargin()
        }
    }

    val classNames = classes.mapNotNull {
        when (it) {
            is ClassStub.Simple -> it.classifier
            is ClassStub.Companion -> null
            is ClassStub.Enum -> it.classifier
        }
    }.onEach { checkPackageCorrectness(it) }.map { it.topLevelName }

    val typealiasNames = typealiases
            .onEach { checkPackageCorrectness(it.alias) }
            .map { it.alias.topLevelName }

    val namesFromNestedContainers = simpleContainers
            .flatMap { it.computeNamesToBeDeclared(pkgName) }

    return classNames + typealiasNames + namesFromNestedContainers
}

val StubContainer.defaultMemberModifier: InheritanceModifier
    get() = when (this) {
        is SimpleStubContainer -> InheritanceModifier.FINAL
        is ClassStub.Simple -> if (this.modality == ClassStubModality.INTERFACE) {
            InheritanceModifier.ABSTRACT
        } else {
            InheritanceModifier.FINAL
        }
        is ClassStub.Companion -> InheritanceModifier.FINAL
        is ClassStub.Enum -> InheritanceModifier.FINAL
    }

/**
 * Returns constructor that should be rendered in class header.
 */
val ClassStub.explicitPrimaryConstructor: ConstructorStub?
    get() = functions.filterIsInstance<ConstructorStub>().firstOrNull(ConstructorStub::isPrimary)

fun ClassStub.nestedName(): String =
        classifier.relativeFqName.substringAfterLast('.')

fun IntegralConstantStub.determineEnumEntryClassifier(): Classifier = when (size) {
    1 -> if (isSigned) "Byte" else "UByte"
    2 -> if (isSigned) "Short" else "UShort"
    4 -> if (isSigned) "Int" else "UInt"
    8 -> if (isSigned) "Long" else "ULong"
    else -> error("Integral constant with unexpected size of ${size}.")
}.let { Classifier.topLevel(cinteropInternalPackage, "CEnumEntryValue").nested(it) }