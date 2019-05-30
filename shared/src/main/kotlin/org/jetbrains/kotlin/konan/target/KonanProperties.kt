/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed -> in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.util.DependencyProcessor

interface TargetableExternalStorage {
    fun targetString(key: String): String? 
    fun targetList(key: String): List<String>
    fun hostString(key: String): String? 
    fun hostList(key: String): List<String> 
    fun hostTargetString(key: String): String? 
    fun hostTargetList(key: String): List<String> 
    fun absolute(value: String?): String
    fun downloadDependencies()
}

abstract class KonanPropertiesLoader(override val target: KonanTarget,
                                     val properties: Properties,
                                     val baseDir: String? = null) : Configurables {
    open val dependencies get() = hostTargetList("dependencies")

    override fun downloadDependencies() {
        dependencyProcessor!!.run()
    }

    override fun targetString(key: String): String? 
        = properties.targetString(key, target)
    override fun targetList(key: String): List<String>
        = properties.targetList(key, target)
    override fun hostString(key: String): String? 
        = properties.hostString(key)
    override fun hostList(key: String): List<String> 
        = properties.hostList(key)
    override fun hostTargetString(key: String): String? 
        = properties.hostTargetString(key, target)
    override fun hostTargetList(key: String): List<String> 
        = properties.hostTargetList(key, target)

    override fun absolute(value: String?): String =
            dependencyProcessor!!.resolveRelative(value!!).absolutePath
    private val dependencyProcessor  by lazy {
        baseDir?.let { DependencyProcessor(java.io.File(it), this) }
    }
}

fun Properties.keepOnlyDefaultProfiles() {
    val DEPENDENCY_PROFILES_KEY = "dependencyProfiles"
    val dependencyProfiles = this.getProperty(DEPENDENCY_PROFILES_KEY)
    if (dependencyProfiles != "default alt")
        error("unexpected $DEPENDENCY_PROFILES_KEY value: expected 'default alt', got '$dependencyProfiles'")

    // Force build to use only 'default' profile:
    this.setProperty(DEPENDENCY_PROFILES_KEY, "default")
    // Force build to use fixed Xcode version:
    this.setProperty("useFixedXcodeVersion", "10.1")
    // TODO: it actually affects only resolution made in :dependencies,
    // that's why we assume that 'default' profile comes first (and check this above).
}
