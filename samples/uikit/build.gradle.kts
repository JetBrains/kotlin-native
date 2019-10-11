import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

allprojects {
    repositories {
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

val sdkName: String? = System.getenv("SDK_NAME")

enum class Target(val simulator: Boolean, val key: String) {
    WATCHOS_X86(true, "watchos"), WATCHOS_ARM64(false, "watchos"),
    IOS_X64(true, "ios"), IOS_ARM64(false, "ios")
}

val target = sdkName.orEmpty().let {
    when {
        it.startsWith("iphoneos") -> Target.IOS_ARM64
        it.startsWith("iphonesimulator") -> Target.IOS_X64
        it.startsWith("watchos") -> Target.WATCHOS_ARM64
        it.startsWith("watchsimulator") -> Target.WATCHOS_X86
        else -> Target.WATCHOS_X86
    }
}

val buildType = System.getenv("CONFIGURATION")?.let {
    NativeBuildType.valueOf(it.toUpperCase())
} ?: NativeBuildType.DEBUG

kotlin {
    // Declare a target.
    // We declare only one target (either arm64 or x64)
    // to workaround lack of common platform libraries
    // for both device and simulator.
    val ios = if (!target.simulator ) {
        // Device.
        iosArm64("ios")
    } else {
        // Simulator.
        iosX64("ios")
    }

    val watchos = if (!target.simulator) {
        // Device.
        watchosArm64("watchos")
    } else {
        // Simulator.
        watchosX86("watchos")
    }

    // Declare the output program.
    ios.binaries.executable(listOf(buildType)) {
        baseName = "app"
        entryPoint = "sample.uikit.main"
    }

    watchos.binaries.executable(listOf(buildType)) {
        baseName = "watchapp"
    }

    // Configure dependencies.
    val appleMain by sourceSets.creating {
        dependsOn(sourceSets["commonMain"])
    }
    sourceSets["iosMain"].dependsOn(appleMain)
    sourceSets["watchosMain"].dependsOn(appleMain)
}

// Create Xcode integration tasks.
val targetBuildDir: String? = System.getenv("TARGET_BUILD_DIR")
val executablePath: String? = System.getenv("EXECUTABLE_PATH")

val currentTarget = kotlin.targets[target.key] as KotlinNativeTarget
val kotlinBinary = currentTarget.binaries.getExecutable(buildType)
val xcodeIntegrationGroup = "Xcode integration"

val packForXCode = if (sdkName == null || targetBuildDir == null || executablePath == null) {
    // The build is launched not by Xcode ->
    // We cannot create a copy task and just show a meaningful error message.
    tasks.create("packForXCode").doLast {
        throw IllegalStateException("Please run the task from Xcode")
    }
} else {
    // Otherwise copy the executable into the Xcode output directory.
    tasks.create("packForXCode", Copy::class.java) {
        dependsOn(kotlinBinary.linkTask)
        destinationDir = file(targetBuildDir)
        from(kotlinBinary.outputFile)
        rename { executablePath }
    }
}

packForXCode.apply {
    group = xcodeIntegrationGroup
    description = "Copies the Kotlin/Native executable into the Xcode output directory (executed by Xcode)."
}

val startSimulator by tasks.creating(Exec::class.java) {
    group = xcodeIntegrationGroup
    description = "Starts an iOS simulator."

    executable = "open"
    args("/Applications/Xcode.app/Contents/Developer/Applications/Simulator.app")
}

val shutdownSimulator by tasks.creating(Exec::class.java) {
    group = xcodeIntegrationGroup
    description = "Stops a running iOS simulator."

    executable = "sh"
    args("-c", "xcrun simctl shutdown booted")
}

val xcodeProject = file("konf-demo.xcodeproj")
val xcodeAppName = "konf-demo"
val xcodeBundleId = "org.jetbrains.kotlin.native-demo"
val xcodeDerivedDataPath = file("$buildDir/xcode-build")

val buildXcode by tasks.creating(Exec::class.java) {
    dependsOn(kotlinBinary.linkTask, startSimulator)
    group = xcodeIntegrationGroup
    description = "Builds the iOS application bundle using Xcode."

    workingDir = xcodeProject
    executable = "sh"
    args("-c", """
        xcrun xcodebuild \
            -scheme $xcodeAppName \
            -project . \
            -configuration Debug \
            -destination 'platform=iOS Simulator,name=iPhone X,OS=latest' \
            -derivedDataPath '$xcodeDerivedDataPath'
        """.trimIndent()
    )
}

val installSimulator by tasks.creating(Exec::class.java) {
    dependsOn(buildXcode)
    group = xcodeIntegrationGroup
    description = "Installs the application bundle on an iOS simulator."

    executable = "sh"
    val appFolder = xcodeDerivedDataPath.resolve("Build/Products/Debug-iphonesimulator/$xcodeAppName.app")
    args("-c", "xcrun simctl install booted '${appFolder.absolutePath}'")
}

val launchSimulator by tasks.creating(Exec::class.java) {
    dependsOn(installSimulator)
    group = xcodeIntegrationGroup
    description = "Launches the application on an iOS simulator."

    executable = "sh"
    args("-c", "xcrun simctl launch booted $xcodeBundleId")
}

