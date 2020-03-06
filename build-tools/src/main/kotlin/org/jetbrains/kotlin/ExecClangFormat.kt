/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin

import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream
import org.gradle.api.GradleException
import org.gradle.api.Project

class ExecClangFormat(private val project: Project) {
    private fun executable(): String {
        val llvmDir = project.findProperty("llvmDir")
        return "$llvmDir/bin/clang-format"
    }

    fun fix(target: String) {
        project.exec {
            it.executable = executable()
            it.args = listOf("-i", target)
        }
    }

    fun check(target: String) {
        val outputStream = ByteArrayOutputStream()
        project.exec {
            it.executable = executable()
            it.args = listOf(target)
            it.standardOutput = outputStream
        }.assertNormalExitValue()
        val diffResult = project.exec {
            it.commandLine = listOf("diff", "-u", target, "-")
            it.standardInput = ByteArrayInputStream(outputStream.toByteArray())
            it.isIgnoreExitValue = true
        }
        if (diffResult.exitValue == 0) {
            return
        }
        throw GradleException("$target is not formatted")
    }
}
