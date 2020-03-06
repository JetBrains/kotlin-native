/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*
import java.io.File

import javax.inject.Inject

enum class ClangFormatMode {
    CHECK,
    FIX,
}

open class ClangFormatSingleTask @Inject constructor(@Input val mode: ClangFormatMode,
                                                     @InputFile val file: File) : DefaultTask() {
    @TaskAction
    fun run() {
        val plugin = project.convention.getPlugin(ExecClangFormat::class.java)
        when (mode) {
            ClangFormatMode.CHECK -> plugin.check(file.absolutePath)
            ClangFormatMode.FIX -> plugin.fix(file.absolutePath)
        }
    }
}

open class ClangFormatTask @Inject constructor(@Input val mode: ClangFormatMode) : DefaultTask() {
    @InputFiles
    @SkipWhenEmpty
    var files: Iterable<File> = ArrayList()

    override fun configure(closure: Closure<Any>): Task {
        super.configure(closure)
        val root = project.file(".")
        for (file in files) {
            val subTaskName = "clangFormat_${mode}_" + file.toRelativeString(root).replace('/', '_')
            project.tasks.create(subTaskName, ClangFormatSingleTask::class.java, mode, file)
            dependsOn(subTaskName)
        }
        return this
    }
}

open class ClangFormatCheckTask : ClangFormatTask(ClangFormatMode.CHECK) {}

open class ClangFormatFixTask : ClangFormatTask(ClangFormatMode.FIX) {}
