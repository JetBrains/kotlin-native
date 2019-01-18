package org.jetbrains.kotlin.cli.klib.merger

import org.jetbrains.kotlin.cli.klib.KlibPrinter
import org.jetbrains.kotlin.cli.klib.defaultRepository
import org.jetbrains.kotlin.cli.klib.libraryInRepo
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager

fun mergeLib(repository: File, platformsPaths: List<String>, whatToMerge: String) {
    val distribution = Distribution()
    val platformManager = PlatformManager(distribution)

    val librariesNames = platformsPaths.map { distribution.klib + it + whatToMerge }
    val libsExist = librariesNames.map(::File).all { it.exists }
    if (!libsExist) {
        return // TODO
    }

    val output = File("merged/$whatToMerge")
    val klibBuilder = MultiTargetKlibWriter(output, repository, platformManager)

    val libraries = librariesNames.map {
        libraryInRepo(repository, it)
    }

    for (lib in libraries) {
        klibBuilder.addKonanLibrary(lib)
    }

    val klibMerger = KlibMergerFacade(repository, platformManager)
    val linkData = klibMerger.merge(libraries)
    val properties = klibMerger.mergeProperties(libraries)

    klibBuilder.addLinkData(linkData)
    klibBuilder.addManifestAddend(properties)
    klibBuilder.commit()

}

fun merge(repository: File, platformsPaths: List<String>, whatToMerge: List<String>) {
    for (what in whatToMerge) {
        mergeLib(repository, platformsPaths, what)
    }
}


fun diff(repository: File, platformsPaths: List<String>, whatToMerge: List<String>) {
    val distribution = Distribution()
    val platformManager = PlatformManager(distribution)

    val klibMerger = KlibMergerFacade(repository, platformManager)
    java.io.File("diffed_lin").mkdir()


    for (what in whatToMerge) {
        val librariesNames = platformsPaths.map { distribution.klib + it + what }


        val libsExist = librariesNames.map(::File).all { it.exists }
        if (!libsExist) {
            continue // TODO
        }

        val libraries = librariesNames.map {
            libraryInRepo(repository, it)
        }

        val output = "diffed_lin/$what"
        val a = klibMerger.diff(libraries)

        for ((l, targets) in a) {
            l.setDependencies(l)

            val file = java.io.File(output + targets.first())
            file.createNewFile()
            file.printWriter().use {
                KlibPrinter(it).print(l)
            }
        }
    }

}

fun main(args: Array<String>) {
    val whatToMerge = listOf(
//            "linux"
            "posix"
//            "darwin", "Accelerate", "AppKit", "ApplicationServices",
//            "CFNetwork", "CommonCrypto", "CoreData",
//            "CoreFoundation", "CoreGraphics", "CoreImage",
//            "CoreML", "CoreServices", "CoreText",
//            "CoreVideo", "darwin", "DiskArbitration",
//            "Foundation", "GLUT", "iconv", "ImageIO",
//            "IOKit", "IOSurface", "libkern", "Metal",
//            "objc", "OpenGL", "OpenGL3", "OpenGLCommon",
//            "osx", "posix", "QuartzCore", "Security", "zlib"
    )

    val platformsPaths = listOf(
//            "/platform/ios_x64/",
//            "/platform/macos_x64/"
            "/platform/linux_x64/",
//            "/platform/linux_x64/" //,
            "/platform/linux_mips32/"
//            "/platform/android_arm64/"
//            "/platform/linux_arm32_hfpStdlib"
    )

    val repository = defaultRepository
    merge(repository, platformsPaths, whatToMerge)
    diff(repository, platformsPaths, whatToMerge)
}
