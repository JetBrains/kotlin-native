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

package  org.jetbrains.kotlin.native.interop.tool

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.visibleName
import org.jetbrains.kotlin.native.interop.gen.jvm.KotlinPlatform

class ToolConfig(userProvidedTargetName: String?, flavor: KotlinPlatform) {

    private val konanHome = System.getProperty("konan.home")
    private val distribution = customerDistribution(konanHome)
    private val platformManager = PlatformManager(distribution)
    private val targetManager = platformManager.targetManager(userProvidedTargetName)
    private val host = HostManager.host
    private val target = targetManager.target

    private val platform = platformManager.platform(target)

    val substitutions = mapOf<String, String>(
            "target" to target.detailedName,
            "arch" to target.architecture.visibleName)

    fun downloadDependencies() = platform.downloadDependencies()

    val defaultCompilerOpts =
            platform.clang.targetLibclangArgs.toList()

    val platformCompilerOpts = if (flavor == KotlinPlatform.JVM)
            platform.clang.hostCompilerArgsForJni.toList() else emptyList()

    val llvmHome = platform.absoluteLlvmHome
    val sysRoot = platform.absoluteTargetSysRoot

    val libclang = when (host) {
        KonanTarget.MINGW_X64 -> "$llvmHome/bin/libclang.dll"
        else -> "$llvmHome/lib/${System.mapLibraryName("clang")}"
    }
}
