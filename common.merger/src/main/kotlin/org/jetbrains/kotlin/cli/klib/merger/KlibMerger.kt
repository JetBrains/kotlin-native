package org.jetbrains.kotlin.cli.klib.merger

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.backend.konan.KonanConfig
import org.jetbrains.kotlin.backend.konan.KonanConfigKeys
import org.jetbrains.kotlin.backend.konan.serialization.serializeModule
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.klib.Library
import org.jetbrains.kotlin.cli.klib.defaultRepository
import org.jetbrains.kotlin.cli.klib.libraryInRepo
import org.jetbrains.kotlin.cli.klib.libraryInRepoOrCurrentDir
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_ABI_VERSION
import org.jetbrains.kotlin.konan.library.KLIB_PROPERTY_COMPILER_VERSION
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.defaultResolver
import org.jetbrains.kotlin.konan.parseKonanAbiVersion
import org.jetbrains.kotlin.konan.parseKonanVersion
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.utils.KonanFactories
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import java.util.*


class KlibMerger(private val repository: File,
                 private val distribution: Distribution = Distribution()) {
    lateinit var konanConfig: KonanConfig

    private fun writeModuleDescriptorAndManifest(
            output: String,
            module: ModuleDescriptor,
            manifest: Properties
    ) {
        initKonanConfig()
        val multiTargetLibraryWriter = MultiTargetLibraryWriter(File(output), nopack = true)

        val linkData = serializeModule(module, konanConfig)
        multiTargetLibraryWriter.addLinkData(linkData)

        multiTargetLibraryWriter.addManifestAddend(manifest)
        multiTargetLibraryWriter.commit()
    }

    private fun initKonanConfig() {
        val configuration = CompilerConfiguration()
        configuration.put(KonanConfigKeys.PRODUCE, CompilerOutputKind.valueOf("program".toUpperCase()))
        configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS, LanguageVersionSettingsImpl.DEFAULT)

        val rootDisposable = Disposer.newDisposable()
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable,
                configuration, EnvironmentConfigFiles.NATIVE_CONFIG_FILES)
        val project = environment.project

        konanConfig = KonanConfig(project, configuration)
    }

    private fun mergeManifestProperties(properties: List<Properties>): Properties {
        return properties.first()
    }

    private fun writeSpecificParts(output: String, lib: KonanLibrary, hostManager: PlatformManager) {
        val targetVersion = lib.manifestProperties.getProperty(KLIB_PROPERTY_COMPILER_VERSION).parseKonanVersion()
        val abiVersion = lib.manifestProperties.getProperty(KLIB_PROPERTY_ABI_VERSION).parseKonanAbiVersion()

        for (target in lib.targetList.map { hostManager.targetByName(it) }) {
            val resolver = defaultResolver(
                    listOf(repository.absolutePath), emptyList(), target, Distribution(),
                    skipCurrentDir = true,
                    abiVersion = abiVersion,
                    compatibleCompilerVersions = listOf(targetVersion))
            val currentLib = resolver.resolve(lib.libraryName)


            val singleTargetLibraryWriterImpl = SingleTargetLibraryWriterImpl(File(output), target)
            currentLib.dataFlowGraph?.let { singleTargetLibraryWriterImpl.addDataFlowGraph(it) }

            currentLib.kotlinBitcodePaths.forEach {
                singleTargetLibraryWriterImpl.addKotlinBitcode(it)
            }

            currentLib.includedPaths.forEach {
                singleTargetLibraryWriterImpl.addIncludedBinary(it)
            }

            val kotlinIncludePaths = currentLib.kotlinBitcodePaths.toSet()
            currentLib.bitcodePaths.forEach {
                if (!kotlinIncludePaths.contains(it)) {
                    singleTargetLibraryWriterImpl.addNativeBitcode(it)
                }
            }
        }
    }

    private fun loadAndMergeDescriptors(libraries: List<KonanLibrary>): ModuleDescriptor {
        val versionSpec = LanguageVersionSettingsImpl(currentLanguageVersion, currentApiVersion)
        val storageManager = LockBasedStorageManager()

        // TODO find out is it required to load and set as dependency stdlib
//    val stdLibModule = loadStdlib(distribution, versionSpec, storageManager)

        val defaultBuiltins = DefaultBuiltIns.Instance
        val modules = mutableListOf<ModuleDescriptorImpl>()
        for (lib in libraries) {
            val konanLibrary = libraryInRepoOrCurrentDir(repository, lib.libraryName)
            val curModule = KonanFactories.DefaultDeserializedDescriptorFactory
                    .createDescriptor(konanLibrary, versionSpec, storageManager, defaultBuiltins)


            // TODO is it okey?
            listOf(curModule).let { allModules ->
                for (it in allModules) {
                    it.setDependencies(allModules)
                }
            }

            modules.add(curModule)
        }

        val descriptorMerger = DummyDescriptorMerger(storageManager, defaultBuiltins)
        val mergeModules = descriptorMerger.mergeModules(modules)

        listOf(mergeModules).let { allModules ->
            for (it in allModules) {
                it.setDependencies(allModules)
            }
        }

        return mergeModules
    }

    private fun loadStdlib(distribution: Distribution,
                           versionSpec: LanguageVersionSettings,
                           storageManager: StorageManager): ModuleDescriptorImpl {
        val stdlib = Library(distribution.stdlib, null, "host")
        val library = libraryInRepoOrCurrentDir(stdlib.repository, stdlib.name)
        return KonanFactories.DefaultDeserializedDescriptorFactory.createDescriptorAndNewBuiltIns(library, versionSpec, storageManager)
    }

    companion object {
        val currentLanguageVersion = LanguageVersion.LATEST_STABLE
        val currentApiVersion = ApiVersion.LATEST_STABLE
    }

    fun mergeKlibs(output: String, libs: List<KonanLibrary>) {
        val mergedModules = loadAndMergeDescriptors(libs)
        val mergedManifestProperties = mergeManifestProperties(libs.map { it.manifestProperties })
        writeModuleDescriptorAndManifest(output, mergedModules, mergedManifestProperties)

        val hostManager = PlatformManager(distribution)
        for (lib in libs) {
            writeSpecificParts(output, lib, hostManager)
        }

    }

}

fun main(args: Array<String>) {
    val distribution = Distribution()
    val whatToMerge = listOf(
            "linux"
//            "darwin", "AppKit", "ApplicationServices",
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
//                "/platform/ios_x64/",
//                "/platform/macos_x64/"
            "/platform/linux_x64/",
            "/platform/linux_mips32/"
    )

    val repository = defaultRepository
    val klibMerger = KlibMerger(repository, distribution)

    for (what in whatToMerge) {
        val librariesNames = platformsPaths.map { distribution.klib + it + what }

        val libsExist = librariesNames.map(::File).all { it.exists }
        if (!libsExist) {
            continue
        }

        val libraries = librariesNames.map {
            libraryInRepo(repository, it)
        }

        val output = "merged/$what"
        klibMerger.mergeKlibs(output, libraries)
    }

}
