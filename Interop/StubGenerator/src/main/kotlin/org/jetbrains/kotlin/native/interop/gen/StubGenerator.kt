/*
 * Copyright 2010-2019 JetBrains s.r.o.
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

import org.jetbrains.kotlin.native.interop.gen.jvm.InteropConfiguration
import org.jetbrains.kotlin.native.interop.indexer.*
import java.util.*

interface KotlinStub

abstract class StubGenerator<T : KotlinStub>(
        protected val nativeIndex: NativeIndex,
        private val verbose: Boolean,
        val configuration: InteropConfiguration
) {
    private var theCounter = 0

    fun nextUniqueId() = theCounter++

    val pkgName: String
        get() = configuration.pkgName

    private val excludedFunctions: Set<String>
        get() = configuration.excludedFunctions

    private val excludedMacros: Set<String>
        get() = configuration.excludedMacros

    /**
     * Indicates whether this enum should be represented as Kotlin enum.
     */
    val EnumDef.isStrictEnum: Boolean
        // TODO: if an anonymous enum defines e.g. a function return value or struct field type,
        // then it probably should be represented as Kotlin enum
        get() {
            if (this.isAnonymous) {
                return false
            }

            val name = this.kotlinName

            if (name in configuration.strictEnums) {
                return true
            }

            if (name in configuration.nonStrictEnums) {
                return false
            }

            // Let the simple heuristic decide:
            return !this.constants.any { it.isExplicitlyDefined }
        }

    /**
     * The names that should not be used for struct classes to prevent name clashes
     */
    private val forbiddenStructNames = run {
        val typedefNames = nativeIndex.typedefs.map { it.name }
        typedefNames.toSet()
    }

    private val anonymousStructKotlinNames = mutableMapOf<StructDecl, String>()

    protected val noStringConversion: Set<String>
        get() = configuration.noStringConversion

    protected fun log(message: String) {
        if (verbose) {
            println(message)
        }
    }

    private val functionsToBind = nativeIndex.functions.filter { it.name !in excludedFunctions }

    protected abstract fun generateKotlinFragmentBy(block: () -> Unit): T

    protected abstract fun generateStubsForFunctions(functions: List<FunctionDecl>): Collection<T>

    protected abstract fun generateGlobalVariableStub(globalVariable: GlobalDecl): T

    protected abstract fun generateConstant(constant: ConstantDef)

    protected abstract fun generateStruct(struct: StructDecl)

    protected abstract fun generateEnum(enum: EnumDef)

    protected abstract fun generateTypedef(typedef: TypedefDef)

    protected abstract fun generateObjCProtocolStub(protocol: ObjCProtocol) : T

    protected abstract fun generateObjCClassStub(klass: ObjCClass): T

    protected abstract fun generateObjCCategory(category: ObjCCategory): T

    fun addManifestProperties(properties: Properties) {
        val exportForwardDeclarations = configuration.exportForwardDeclarations.toMutableList()

        nativeIndex.structs
                .filter { it.def == null }
                .mapTo(exportForwardDeclarations) {
                    "$cnamesStructsPackageName.${it.kotlinName}"
                }

        properties["exportForwardDeclarations"] = exportForwardDeclarations.joinToString(" ")

        // TODO: consider exporting Objective-C class and protocol forward refs.
    }

    fun generateStubs(): List<T> {
        val stubs = mutableListOf<T>()

        stubs.addAll(generateStubsForFunctions(functionsToBind))

        nativeIndex.objCProtocols.forEach {
            if (!it.isForwardDeclaration) {
                stubs.add(generateObjCProtocolStub(it))
            }
        }

        nativeIndex.objCClasses.forEach {
            if (!it.isForwardDeclaration && !it.isNSStringSubclass()) {
                stubs.add(generateObjCClassStub(it))
            }
        }

        nativeIndex.objCCategories.filter { !it.clazz.isNSStringSubclass() }.mapTo(stubs) {
            generateObjCCategory(it)
        }

        nativeIndex.macroConstants.filter { it.name !in excludedMacros }.forEach {
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateConstant(it) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for constant ${it.name}")
            }
        }

        nativeIndex.wrappedMacros.filter { it.name !in excludedMacros }.forEach {
            try {
                stubs.add(generateGlobalVariableStub(GlobalDecl(it.name, it.type, isConst = true)))
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for macro ${it.name}")
            }
        }

        nativeIndex.globals.filter { it.name !in excludedFunctions }.forEach {
            try {
                stubs.add(generateGlobalVariableStub(it))
            } catch (e: Throwable) {
                log("Warning: cannot generate stubs for global ${it.name}")
            }
        }

        nativeIndex.structs.forEach { s ->
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateStruct(s) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate definition for struct ${s.kotlinName}")
            }
        }

        nativeIndex.enums.forEach {
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateEnum(it) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate definition for enum ${it.spelling}")
            }
        }

        nativeIndex.typedefs.forEach { t ->
            try {
                stubs.add(
                        generateKotlinFragmentBy { generateTypedef(t) }
                )
            } catch (e: Throwable) {
                log("Warning: cannot generate typedef ${t.name}")
            }
        }

        return stubs
    }

    /**
     * The name to be used for this struct in Kotlin
     */
    val StructDecl.kotlinName: String
        get() {
            if (this.isAnonymous) {
                val names = anonymousStructKotlinNames
                return names.getOrPut(this) {
                    "anonymousStruct${names.size + 1}"
                }
            }

            val strippedCName = if (spelling.startsWith("struct ") || spelling.startsWith("union ")) {
                spelling.substringAfter(' ')
            } else {
                spelling
            }

            // TODO: don't mangle struct names because it wouldn't work if the struct
            // is imported into another interop library.
            return if (strippedCName !in forbiddenStructNames) strippedCName else (strippedCName + "Struct")
        }

    /**
     * The name to be used for this enum in Kotlin
     */
    val EnumDef.kotlinName: String
        get() = if (spelling.startsWith("enum ")) {
            spelling.substringAfter(' ')
        } else {
            assert (!isAnonymous)
            spelling
        }
}