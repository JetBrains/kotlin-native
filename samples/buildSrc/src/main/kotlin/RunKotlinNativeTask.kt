/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(
        private val myProject: Project,
        private val myTarget: KotlinTarget
): DefaultTask() {

    var buildType = "RELEASE"
    private var myArgs: List<String> = emptyList()
    private val myEnvironment: MutableMap<String, Any> = mutableMapOf()

    fun args(vararg args: Any) {
        myArgs = args.map { it.toString() }
    }

    fun environment(map: Map<String, Any>) {
        myEnvironment += map
    }

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        this.dependsOn += myTarget.compilations.main.linkTaskName("EXECUTABLE", buildType)
        return task
    }

    @TaskAction
    fun run() {
        myProject.exec {
            it.executable = myTarget.compilations.main.getBinary("EXECUTABLE", buildType).toString()
            it.args = myArgs
            it.environment = myEnvironment
        }
    }
}
