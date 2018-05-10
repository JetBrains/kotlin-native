package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.backend.konan.library.impl.LibraryReaderImpl
import org.jetbrains.kotlin.konan.KonanVersion
import org.jetbrains.kotlin.konan.KonanVersion.*
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.util.*

interface SearchPathResolver {
    val target: KonanTarget?
    val knownAbiVersions: List<Int>?
    val knownCompilerVersions: List<KonanVersion>?
    val searchRoots: List<File>
    fun resolutionList(givenPath: String): List<File>
    fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<KonanLibraryReader>
    val logger: ((String) -> Unit)
    fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean = false): LibraryReaderImpl
    fun resolve(path: String): LibraryReaderImpl
}

fun dummyLogger(msg: String) {}

fun defaultResolver(repositories: List<String>, target: KonanTarget, logger: (String) -> Unit = ::dummyLogger): KonanLibrarySearchPathResolver =
        defaultResolver(repositories, target, Distribution(), logger)

fun defaultResolver(repositories: List<String>, target: KonanTarget, distribution: Distribution, logger: (String) -> Unit = ::dummyLogger): KonanLibrarySearchPathResolver =
        KonanLibrarySearchPathResolver(
                repositories,
                target,
                distribution.klib,
                distribution.localKonanDir.absolutePath,
                false,
                listOf(KonanAbiVersion.CURRENT.version),
                listOf(KonanVersion.CURRENT),
                logger
        )

class KonanLibrarySearchPathResolver(
        repositories: List<String>,
        override val target: KonanTarget?,
        val distributionKlib: String?,
        val localKonanDir: String?,
        val skipCurrentDir: Boolean = false,
        override val knownAbiVersions: List<Int>,
        override val knownCompilerVersions: List<KonanVersion>? = null,
        override val logger: ((String) -> Unit) = ::dummyLogger
): SearchPathResolver {

    val localHead: File?
        get() = localKonanDir?.File()?.klib

    val distHead: File?
        get() = distributionKlib?.File()?.child("common")

    val distPlatformHead: File?
        get() = target?.let { distributionKlib?.File()?.child("platform")?.child(target.visibleName) }

    val currentDirHead: File?
        get() = if (!skipCurrentDir) File.userDir else null

    private val repoRoots: List<File> by lazy {
        repositories.map{File(it)}
    }

    // This is the place where we specify the order of library search.
    override val searchRoots: List<File> by lazy {
        (listOf(currentDirHead) + repoRoots + listOf(localHead, distHead, distPlatformHead)).filterNotNull()
    }

    private fun found(candidate: File): File? {
        fun check(file: File): Boolean =
                file.exists && (file.isFile || File(file, "manifest").exists)

        val noSuffix = File(candidate.path.removeSuffixIfPresent(".klib"))
        val withSuffix = File(candidate.path.suffixIfNot(".klib"))
        return when {
            check(withSuffix) -> withSuffix
            check(noSuffix) -> noSuffix
            else -> null
        }
    }

    override fun resolutionList(givenPath: String): List<File> {
        val given = File(givenPath)
        val list = if (given.isAbsolute) {
            listOf(found(given))
        } else {
            // Do we need a Sequence here, to be a little more lazy?
            searchRoots.map{
                found(File(it, givenPath))
            }
        }.filterNotNull()
        return list
    }

    override fun resolve(unresolved: UnresolvedLibrary, isDefaultLink: Boolean): LibraryReaderImpl {
        val givenPath = unresolved.path
        val files = resolutionList(givenPath)
        val matching = files.map { LibraryReaderImpl(it, target, isDefaultLink) }
                .map { it.takeIf {libraryMatch(it, unresolved)} }
                .filterNotNull()

        if (matching.isEmpty()) error("Could not find \"$givenPath\" in ${searchRoots.map{it.absolutePath}}.")
        return matching.first()
    }

    override fun resolve(givenPath: String) = resolve(UnresolvedLibrary(givenPath, null), false)

    private val File.klib
        get() = File(this, "klib")

    // The libraries from the default root are linked automatically.
    val defaultRoots: List<File>
        get() = listOf(distHead, distPlatformHead)
                .filterNotNull()
                .filter{ it.exists }

    override fun defaultLinks(nostdlib: Boolean, noDefaultLibs: Boolean): List<LibraryReaderImpl> {
        val defaultLibs = defaultRoots.flatMap{ it.listFiles }
                .filterNot { it.name.removeSuffixIfPresent(".klib") == "stdlib" }
                .map { UnresolvedLibrary(it.absolutePath, null) }
                .map { resolve(it, isDefaultLink = true) }
        val result = mutableListOf<LibraryReaderImpl>()
        if (!nostdlib) result.add(resolve(UnresolvedLibrary("stdlib", null)))
        if (!noDefaultLibs) result.addAll(defaultLibs)
        return result
    }
}


fun SearchPathResolver.libraryMatch(candidate: LibraryReaderImpl, unresolved: UnresolvedLibrary): Boolean {
    val resolverTarget = this.target
    val candidatePath = candidate.libraryFile.absolutePath

    if (resolverTarget != null && !candidate.targetList.contains(resolverTarget.visibleName)) {
        logger("skipping $candidatePath as it doesn't support the needed hardware target. Expected '$resolverTarget', found ${candidate.targetList}")
        return false
    }

    if (knownCompilerVersions != null &&
            !knownCompilerVersions!!.contains(candidate.compilerVersion)) {
        logger("skipping $candidatePath. The compiler versions don't match. Expected '${knownCompilerVersions}', found '${candidate.compilerVersion}'")
        return false
    }

    if (knownAbiVersions != null &&
            !knownAbiVersions!!.contains(candidate.abiVersion)) {
        logger("skipping $candidatePath. The abi versions don't match. Expected '${knownAbiVersions}', found ${candidate.abiVersion}")
        return false
    }

    if (candidate.libraryVersion != unresolved.libraryVersion &&
            candidate.libraryVersion != null &&
            unresolved.libraryVersion != null) {

        logger("skipping $candidatePath. The library versions don't match. Expected `${unresolved.libraryVersion}`, found ${candidate.libraryVersion}")
        return false
    }

    return true
}