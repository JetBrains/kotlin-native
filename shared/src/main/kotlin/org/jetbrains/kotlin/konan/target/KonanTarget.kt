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

package org.jetbrains.kotlin.konan.target

enum class Family(name:String, val exeSuffix:String) {
    OSX("osx", "kexe"),
    LINUX("linux", "kexe"),
    WINDOWS("windows", "exe"),
    ANDROID("android", "so"),
    WASM("wasm", "wasm")
}

enum class Bitness {
    BITSNESS_32,
    BITNESS_64
}

enum class Architecture(val bitness: Int) {
    X86_64(64),
    ARM64(64),
    ARM32(32),
    RASPBERRYPI(32),
    MIPS32(32),
    MIPSEL32(32),
    WASM32(32);

    val userName: String
        get() {
            return if (this == X86_64) 
                "x86-64" // Dash instead of underscore.
            else 
                this.name.toLowerCase()
        }
}

enum class KonanTarget(val family: Family, val architecture: Architecture, val detailedName: String, var enabled: Boolean = false) {
    ANDROID_ARM32(  Family.ANDROID,     Architecture.ARM32,     "android_arm32"),
    ANDROID_ARM64(  Family.ANDROID,     Architecture.ARM64,     "android_arm64"),
    IPHONE(         Family.OSX,         Architecture.ARM32,     "ios"),
    IPHONE_SIM(     Family.OSX,         Architecture.X86_64,    "ios_sim"),
    LINUX(          Family.LINUX,       Architecture.X86_64,    "linux"),
    MINGW(          Family.WINDOWS,     Architecture.X86_64,    "mingw"),
    MACBOOK(        Family.OSX,         Architecture.X86_64,    "osx"),
    RASPBERRYPI(    Family.LINUX,       Architecture.RASPBERRYPI, "raspberrypi"),
    LINUX_MIPS32(   Family.LINUX,       Architecture.MIPS32,    "linux_mips32"),
    LINUX_MIPSEL32( Family.LINUX,       Architecture.MIPSEL32,  "linux_mipsel32"),
    WASM32(         Family.WASM,        Architecture.WASM32,    "wasm32");

    val userName get() = name.toLowerCase()
}

fun hostTargetSuffix(host: KonanTarget, target: KonanTarget) =
    if (target == host) host.detailedName else "${host.detailedName}-${target.detailedName}"

enum class CompilerOutputKind {
    PROGRAM {
        override fun suffix(target: KonanTarget?) = ".${target!!.family.exeSuffix}"
    },
    LIBRARY {
        override fun suffix(target: KonanTarget?) = ".klib"
    } ,
    BITCODE {
        override fun suffix(target: KonanTarget?) = ".bc"
    };

    abstract fun suffix(target: KonanTarget? = null): String
}

class TargetManager(val userRequest: String? = null) {
    val targets = KonanTarget.values().associate{ it.userName to it }
    val target = determineCurrent()
    val targetName
        get() = target.name.toLowerCase()


    fun known(name: String): String {
        if (targets[name] == null) {
            throw TargetSupportException("Unknown target: $name. Use -list_targets to see the list of available targets")
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

    val hostTargetSuffix get() = hostTargetSuffix(host, target)
    val targetSuffix get() = target.detailedName

    companion object {

        fun host_os(): String {
            val javaOsName = System.getProperty("os.name")
            return when {
                javaOsName == "Mac OS X" -> "osx"
                javaOsName == "Linux" -> "linux"
                javaOsName.startsWith("Windows") -> "windows"
                else -> throw TargetSupportException("Unknown operating system: ${javaOsName}")
            }
        }

        @JvmStatic
        fun simpleOsName(): String {
            val hostOs = host_os()
            return if (hostOs == "osx") "macos" else hostOs
        }

        @JvmStatic
        fun longerSystemName(): String = when (host) {
            KonanTarget.MACBOOK ->  "darwin-macos"
            KonanTarget.LINUX ->  "linux-x86-64"
            KonanTarget.MINGW -> "windows-x86-64"
            else -> throw TargetSupportException("Unknown host: $host")
        }

        val jniHostPlatformIncludeDir: String 
            get() = when(host) {
                KonanTarget.MACBOOK -> "darwin"
                KonanTarget.LINUX -> "linux"
                KonanTarget.MINGW ->"win32"
                else -> throw TargetSupportException("Unknown host: $host.")
            }

        fun host_arch(): String {
            val javaArch = System.getProperty("os.arch")
            return when (javaArch) {
                "x86_64" -> "x86_64"
                "amd64"  -> "x86_64"
                "arm64"  -> "arm64"
                else -> throw TargetSupportException("Unknown hardware platform: ${javaArch}")
            }
        }

        val host: KonanTarget = when (host_os()) {
            "osx"   -> KonanTarget.MACBOOK
            "linux" -> KonanTarget.LINUX
            "windows" -> KonanTarget.MINGW
            else -> throw TargetSupportException("Unknown host target: ${host_os()} ${host_arch()}")
        }

        val hostSuffix get() = host.detailedName
        @JvmStatic
        val hostName get() = host.name.toLowerCase()

        init {
            when (host) {
                KonanTarget.LINUX   -> {
                    KonanTarget.LINUX.enabled = true
                    KonanTarget.RASPBERRYPI.enabled = true
                    KonanTarget.LINUX_MIPS32.enabled = true
                    KonanTarget.LINUX_MIPSEL32.enabled = true
                    KonanTarget.ANDROID_ARM32.enabled = true
                    KonanTarget.ANDROID_ARM64.enabled = true
                }
                KonanTarget.MINGW -> {
                    KonanTarget.MINGW.enabled = true
                }
                KonanTarget.MACBOOK -> {
                    KonanTarget.MACBOOK.enabled = true
                    KonanTarget.IPHONE.enabled = true
                    //KonanTarget.IPHONE_SIM.enabled = true
                    KonanTarget.ANDROID_ARM32.enabled = true
                    KonanTarget.ANDROID_ARM64.enabled = true
                    KonanTarget.WASM32.enabled = true
                }
                else ->
                    throw TargetSupportException("Unknown host platform: $host")
            }
        }

        @JvmStatic
        val enabled: List<KonanTarget> 
            get() = KonanTarget.values().asList().filter { it.enabled }
    }
}

class TargetSupportException (message: String = "", cause: Throwable? = null) : Exception(message, cause)

