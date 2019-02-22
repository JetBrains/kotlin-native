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

package org.jetbrains.kotlin.konan.target

import org.jetbrains.kotlin.konan.file.File

class ClangArgs(private val configurables: Configurables) : Configurables by configurables {

    val targetArg = if (configurables is NonAppleConfigurables)
        configurables.targetArg
    else null

    val specificClangArgs: List<String>
        get() {
            val result = when (target) {
                KonanTarget.LINUX_X64 ->
                    listOf("--sysroot=$absoluteTargetSysRoot") +
                    if (target != host) listOf("-target", targetArg!!) else emptyList()

                KonanTarget.LINUX_ARM32_HFP ->
                    listOf("-target", targetArg!!,
                            "-mfpu=vfp", "-mfloat-abi=hard",
                            "--sysroot=$absoluteTargetSysRoot",
                            // TODO: those two are hacks.
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.8.3",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.8.3/arm-linux-gnueabihf")

                KonanTarget.LINUX_MIPS32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4/mips-unknown-linux-gnu")

                KonanTarget.LINUX_MIPSEL32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.4/mipsel-unknown-linux-gnu")

                KonanTarget.MINGW_X64, KonanTarget.MINGW_X86 ->
                    listOf("-target", targetArg!!, "--sysroot=$absoluteTargetSysRoot", "-Xclang", "-flto-visibility-public-std")

                KonanTarget.MACOS_X64 ->
                    listOf("--sysroot=$absoluteTargetSysRoot", "-mmacosx-version-min=10.11")

                KonanTarget.IOS_ARM32 ->
                    listOf("-stdlib=libc++", "-arch", "armv7", "-isysroot", absoluteTargetSysRoot, "-miphoneos-version-min=9.0.0")

                KonanTarget.IOS_ARM64 ->
                    listOf("-stdlib=libc++", "-arch", "arm64", "-isysroot", absoluteTargetSysRoot, "-miphoneos-version-min=9.0.0")

                KonanTarget.IOS_X64 ->
                    listOf("-stdlib=libc++", "-isysroot", absoluteTargetSysRoot, "-miphoneos-version-min=9.0.0")

                KonanTarget.ANDROID_ARM32 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            // HACKS!
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x/arm-linux-androideabi")

                KonanTarget.ANDROID_ARM64 ->
                    listOf("-target", targetArg!!,
                            "--sysroot=$absoluteTargetSysRoot",
                            // HACKS!
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x",
                            "-I$absoluteTargetSysRoot/usr/include/c++/4.9.x/aarch64-linux-android")

                // By default wasm target forces `hidden` visibility which causes
                // linkage problems.
                KonanTarget.WASM32 ->
                    listOf("-target", targetArg!!,
                            "-fno-rtti",
                            "-fno-exceptions",
                            "-fvisibility=default",
                            "-D_LIBCPP_ABI_VERSION=2",
                            "-D_LIBCPP_NO_EXCEPTIONS=1",
                            "-nostdinc",
                            "-Xclang", "-nobuiltininc",
                            "-Xclang", "-nostdsysteminc",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/include/libcxx",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/lib/libcxxabi/include",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/include/compat",
                            "-Xclang", "-isystem$absoluteTargetSysRoot/include/libc")

                is KonanTarget.ZEPHYR ->
                    listOf("-target", targetArg!!,
                        "-fno-rtti",
                        "-fno-exceptions",
                        "-fno-asynchronous-unwind-tables",
                        "-fno-pie",
                        "-fno-pic",
                        "-fshort-enums",
                        "-nostdinc",
                        // TODO: make it a libGcc property? 
                        // We need to get rid of wasm sysroot first.
                        "-isystem $targetToolchain/../lib/gcc/arm-none-eabi/7.2.1/include",
                        "-isystem $targetToolchain/../lib/gcc/arm-none-eabi/7.2.1/include-fixed",
                        "-isystem$absoluteTargetSysRoot/include/libcxx",
                        "-isystem$absoluteTargetSysRoot/include/libc"
                        ) +
                    (configurables as ZephyrConfigurables).boardSpecificClangFlags

            }
            return result
        }

    val clangArgsSpecificForKonanSources
        get() = when (target) {
            KonanTarget.LINUX_X64 ->
                listOf("-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=64",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")

            KonanTarget.LINUX_ARM32_HFP ->
                listOf("-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=32",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")

            KonanTarget.LINUX_MIPS32 ->
                listOf("-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=32",
                        // TODO: reconsider, once target MIPS can do proper 64-bit load/store/CAS.
                        "-DKONAN_NO_64BIT_ATOMIC=1",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")

            KonanTarget.LINUX_MIPSEL32 ->
                listOf("-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=32",
                        // TODO: reconsider, once target MIPS can do proper 64-bit load/store/CAS.
                        "-DKONAN_NO_64BIT_ATOMIC=1",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")

            KonanTarget.MINGW_X64, KonanTarget.MINGW_X86 ->
                listOf("-DUSE_GCC_UNWIND=1",
                        "-DUSE_PE_COFF_SYMBOLS=1",
                        "-DKONAN_WINDOWS=1",
                        "-DKONAN_NO_MEMMEM=1",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")

            KonanTarget.MACOS_X64 ->
                listOf("-DKONAN_OSX=1",
                        "-DKONAN_OBJC_INTEROP=1",
                        "-DKONAN_CORE_SYMBOLICATION=1",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")

            KonanTarget.IOS_ARM32 ->
                listOf("-DKONAN_OBJC_INTEROP=1",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1",
                        "-DKONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG=1",
                        "-DMACHSIZE=32",
                        // While not 100% correct here, using atomic ops on iOS armv7 requires 8 byte alignment,
                        // and general ABI requires 4-byte alignment on 64-bit long fields as mentioned in
                        // https://developer.apple.com/library/archive/documentation/Xcode/Conceptual/iPhoneOSABIReference/Articles/ARMv6FunctionCallingConventions.html#//apple_ref/doc/uid/TP40009021-SW1
                        // See https://github.com/ktorio/ktor/issues/941 for the context.
                        "-DKONAN_NO_64BIT_ATOMIC=1",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")

            KonanTarget.IOS_ARM64 ->
                listOf("-DKONAN_OBJC_INTEROP=1",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1",
                        "-DKONAN_REPORT_BACKTRACE_TO_IOS_CRASH_LOG=1",
                        "-DMACHSIZE=64")

            KonanTarget.IOS_X64 ->
                listOf("-DKONAN_OBJC_INTEROP=1",
                        "-DKONAN_CORE_SYMBOLICATION=1",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")

            KonanTarget.ANDROID_ARM32 ->
                listOf("-D__ANDROID__",
                        "-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=32",
                        "-DKONAN_ANDROID",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")

            KonanTarget.ANDROID_ARM64 ->
                listOf("-D__ANDROID__",
                        "-DUSE_GCC_UNWIND=1",
                        "-DUSE_ELF_SYMBOLS=1",
                        "-DELFSIZE=64",
                        "-DKONAN_ANDROID",
                        "-DKONAN_HAS_CXX11_EXCEPTION_FUNCTIONS=1")

            KonanTarget.WASM32 ->
                listOf("-DKONAN_WASM=1",
                        "-DKONAN_NO_FFI=1",
                        "-DKONAN_NO_THREADS=1",
                        "-DKONAN_NO_EXCEPTIONS=1",
                        "-DKONAN_INTERNAL_DLMALLOC=1",
                        "-DKONAN_INTERNAL_SNPRINTF=1",
                        "-DKONAN_INTERNAL_NOW=1",
                        "-DKONAN_NO_MEMMEM",
                        "-DKONAN_NO_CTORS_SECTION=1")

            is KonanTarget.ZEPHYR ->
                listOf( "-DKONAN_ZEPHYR=1",
                        "-DKONAN_NO_FFI=1",
                        "-DKONAN_NO_THREADS=1",
                        "-DKONAN_NO_EXCEPTIONS=1",
                        "-DKONAN_NO_MATH=1",
                        "-DKONAN_INTERNAL_SNPRINTF=1",
                        "-DKONAN_INTERNAL_NOW=1",
                        "-DKONAN_NO_MEMMEM=1",
                        "-DKONAN_NO_CTORS_SECTION=1",
                        "-DKONAN_NO_UNALIGNED_ACCESS=1")
        }

    private val host = HostManager.host

    private val binDir = when (host) {
        KonanTarget.LINUX_X64 -> "$absoluteTargetToolchain/bin"
        KonanTarget.MINGW_X64 -> "$absoluteTargetToolchain/bin"
        KonanTarget.MACOS_X64 -> "$absoluteTargetToolchain/usr/bin"
        else -> throw TargetSupportException("Unexpected host platform")
    }

    private val extraHostClangArgs =
            if (configurables is LinuxBasedConfigurables) {
                listOf("--gcc-toolchain=${configurables.absoluteGccToolchain}")
            } else {
                emptyList()
            }

    val commonClangArgs = listOf("-B$binDir", "-fno-stack-protector") + extraHostClangArgs

    val clangPaths = listOf("$absoluteLlvmHome/bin", binDir)

    private val jdkDir by lazy {
        val home = File.javaHome.absoluteFile
        if (home.child("include").exists)
            home.absolutePath
        else
            home.parentFile.absolutePath
    }

    val hostCompilerArgsForJni = listOf("", HostManager.jniHostPlatformIncludeDir).map { "-I$jdkDir/include/$it" }.toTypedArray()

    val clangArgs = (commonClangArgs + specificClangArgs).toTypedArray()

    val clangArgsForKonanSources =
            clangArgs + clangArgsSpecificForKonanSources

    val targetLibclangArgs: List<String> =
            // libclang works not exactly the same way as the clang binary and
            // (in particular) uses different default header search path.
            // See e.g. http://lists.llvm.org/pipermail/cfe-dev/2013-November/033680.html
            // We workaround the problem with -isystem flag below.
            listOf("-isystem", "$absoluteLlvmHome/lib/clang/$llvmVersion/include", *clangArgs)

    val targetClangCmd
            = listOf("${absoluteLlvmHome}/bin/clang") + clangArgs

    val targetClangXXCmd
            = listOf("${absoluteLlvmHome}/bin/clang++") + clangArgs

    fun clangC(vararg userArgs: String) = targetClangCmd + userArgs.asList()

    fun clangCXX(vararg userArgs: String) = targetClangXXCmd + userArgs.asList()

    companion object {
        @JvmStatic
        fun filterGradleNativeSoftwareFlags(args: MutableList<String>) {
            args.remove("/usr/include") // HACK: over gradle-4.4.
            args.remove("-nostdinc") // HACK: over gradle-5.1.
            when (HostManager.host) {
                KonanTarget.LINUX_X64 -> args.remove("/usr/include/x86_64-linux-gnu")  // HACK: over gradle-4.4.
                KonanTarget.MACOS_X64 -> {
                    val indexToRemove = args.indexOf(args.find { it.contains("MacOSX.platform")})  // HACK: over gradle-4.7.
                    if (indexToRemove != -1) {
                        args.removeAt(indexToRemove - 1) // drop -I.
                        args.removeAt(indexToRemove - 1) // drop /Application/Xcode.app/...
                    }
                }
            }
        }
    }
}

