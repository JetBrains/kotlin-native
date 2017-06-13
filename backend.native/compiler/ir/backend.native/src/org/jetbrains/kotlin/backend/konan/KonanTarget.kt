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

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.CompilerConfiguration

enum class KonanTarget(val suffix: String, var enabled: Boolean = false) {
    ANDROID_ARM32("android_arm32"),
    ANDROID_ARM64("android_arm64"),
    IPHONE("ios"),
    IPHONE_SIM("ios_sim"),
    LINUX("linux"),
    MINGW("mingw"),
    MACBOOK("osx"),
    RASPBERRYPI("raspberrypi")
}

class TargetManager(val userRequest: String? = null) {
    constructor(config: CompilerConfiguration) : this(config.get(KonanConfigKeys.TARGET))
    val targets = KonanTarget.values().associate{ it.name.toLowerCase() to it }
    val target = determineCurrent()
    val targetName
        get() = target.name.toLowerCase()

    init {
        when (host) {
            KonanTarget.LINUX   -> {
                KonanTarget.LINUX.enabled = true
                KonanTarget.RASPBERRYPI.enabled = true
                KonanTarget.ANDROID_ARM32.enabled = true
                KonanTarget.ANDROID_ARM64.enabled = true
            }
            KonanTarget.MINGW -> {
                KonanTarget.MINGW.enabled = true
            }
            KonanTarget.MACBOOK -> {
                KonanTarget.MACBOOK.enabled = true
                KonanTarget.IPHONE.enabled = true
                KonanTarget.IPHONE_SIM.enabled = true
                KonanTarget.ANDROID_ARM32.enabled = true
                KonanTarget.ANDROID_ARM64.enabled = true
            }
            else ->
                error("Unknown host platform: $host")
        }

        if (!target.enabled) {
            error("Target $target is not available on the target host")
        }
    }

    fun known(name: String): String {
        if (targets[name] == null) {
            error("Unknown target: $name. Use -list_targets to see the list of available targets")
        }
        return name
    }

    fun list() {
        targets.forEach { key, it -> 
            if (it.enabled) {
                val isDefault = if (it == target) "(default)" else ""
                println(String.format("%1$-30s%2$-10s", "$key:", "$isDefault"))
            }
        }
    }

    fun determineCurrent(): KonanTarget {
        return if (userRequest == null || userRequest == "host") {
            host
        } else {
            targets[known(userRequest)]!!
        }
    }

    val hostSuffix get() = host.suffix
    val hostTargetSuffix get() =
        if (target == host) host.suffix else "${host.suffix}-${target.suffix}"
    val targetSuffix get() = target.suffix

    companion object {
        fun host_os(): String {
            val javaOsName = System.getProperty("os.name")
            return when {
                javaOsName == "Mac OS X" -> "osx"
                javaOsName == "Linux" -> "linux"
                javaOsName.startsWith("Windows") -> "windows"
                else -> error("Unknown operating system: ${javaOsName}")
            }
        }

        fun host_arch(): String { 
            val javaArch = System.getProperty("os.arch")
            return when (javaArch) {
                "x86_64" -> "x86_64"
                "amd64"  -> "x86_64"
                "arm64"  -> "arm64"
                else -> error("Unknown hardware platform: ${javaArch}")
            }
        }

        val host: KonanTarget = when (host_os()) {
            "osx"   -> KonanTarget.MACBOOK
            "linux" -> KonanTarget.LINUX
            "windows" -> KonanTarget.MINGW
            else -> error("Unknown host target: ${host_os()} ${host_arch()}")
        }
    }
}

