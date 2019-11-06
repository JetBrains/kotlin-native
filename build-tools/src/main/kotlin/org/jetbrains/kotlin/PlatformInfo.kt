package org.jetbrains.kotlin

import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.konan.util.*
import org.gradle.api.Project

object PlatformInfo {
    @JvmStatic
    fun isMac() = HostManager.hostIsMac
    @JvmStatic
    fun isWindows() = HostManager.hostIsMingw
    @JvmStatic
    fun isLinux() = HostManager.hostIsLinux

    @JvmStatic
    fun isAppleTarget(project: Project): Boolean {
        val target = getTarget(project)
        return target.family.isAppleFamily
    }

    @JvmStatic
    fun isAppleTarget(target: KonanTarget): Boolean {
        return target.family.isAppleFamily
    }

    @JvmStatic
    fun isWindowsTarget(project: Project) = getTarget(project).family == Family.MINGW

    @JvmStatic
    fun isWasmTarget(project: Project) =
        getTarget(project).family == Family.WASM

    @JvmStatic
    fun getTarget(project: Project): KonanTarget {
        val platformManager = project.rootProject.platformManager()
        val targetName = project.project.testTarget().name
        return platformManager.targetManager(targetName).target
    }

    @JvmStatic
    fun checkXcodeVersion(project: Project) {
        val properties = PropertiesProvider(project)
        val requiredXcodeVersion = properties.xcodeVersion

        if (!DependencyProcessor.isInternalSeverAvailable
                && properties.checkXcodeVersion
                && requiredXcodeVersion != null
        ) {
            val currentXcodeVersion = Xcode.current.version
            if (currentXcodeVersion != requiredXcodeVersion) {
                throw IllegalStateException(
                        "Incorrect Xcode version: ${currentXcodeVersion}. Required Xcode version is ${requiredXcodeVersion}."
                )
            }
        }
    }

    fun unsupportedPlatformException() = TargetSupportException()
}