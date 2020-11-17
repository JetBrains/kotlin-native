/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bitcode

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.jetbrains.kotlin.createCompilationDatabasesFromCompileToBitcodeTasks
import java.io.File
import javax.inject.Inject

/**
 * A plugin creating extensions to compile
 */
open class CompileToBitcodePlugin: Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        extensions.create(EXTENSION_NAME, CompileToBitcodeExtension::class.java, target)

        afterEvaluate {
            // TODO: Support providers (https://docs.gradle.org/current/userguide/lazy_configuration.html)
            //       in database tasks and create them along with corresponding compile tasks (not in afterEvaluate).
            createCompilationDatabasesFromCompileToBitcodeTasks(project, COMPILATION_DATABASE_TASK_NAME)
        }
    }

    companion object {
        const val EXTENSION_NAME = "bitcode"
        const val COMPILATION_DATABASE_TASK_NAME = "CompilationDatabase"
    }
}

open class CompileToBitcodeExtension @Inject constructor(val project: Project) {

    lateinit var targets: List<String>

    fun create(
            name: String,
            srcDir: File,
            outputGroup: String = "main",
            configurationBlock: CompileToBitcode.() -> Unit = {}
    ) {
        targets.forEach { targetName ->
            project.tasks.register(
                    "${targetName}${name}",
                    CompileToBitcode::class.java,
                    srcDir, name, targetName, outputGroup
            ).configure {
                it.group = BasePlugin.BUILD_GROUP
                it.description = "Compiles '$name' to bitcode for $targetName"
                it.configurationBlock()
            }
        }
    }
}
