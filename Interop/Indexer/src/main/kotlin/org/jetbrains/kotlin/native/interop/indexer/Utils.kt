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

package org.jetbrains.kotlin.native.interop.indexer

import clang.*
import kotlinx.cinterop.*
import java.io.Closeable
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

internal val CValue<CXType>.kind: CXTypeKind get() = this.useContents { kind }

internal val CValue<CXCursor>.kind: CXCursorKind get() = this.useContents { kind }

internal fun CValue<CXString>.convertAndDispose(): String {
    try {
        return clang_getCString(this)!!.toKString()
    } finally {
        clang_disposeString(this)
    }
}

internal fun CPointer<CXStringSet>.convertAndDispose(): Set<String> = try {
    (0 until this.pointed.Count).mapTo(mutableSetOf()) {
        clang_getCString(this.pointed.Strings!![it].readValue())!!.toKString()
    }
} finally {
    clang_disposeStringSet(this)
}

internal fun getCursorSpelling(cursor: CValue<CXCursor>) =
        clang_getCursorSpelling(cursor).convertAndDispose()

internal fun CValue<CXType>.getSize(): Long {
    val size = clang_Type_getSizeOf(this)
    if (size < 0) {
        throw Error(size.toString())
    }
    return size
}

internal fun convertUnqualifiedPrimitiveType(type: CValue<CXType>): Type = when (type.kind) {
    CXTypeKind.CXType_Char_U, CXTypeKind.CXType_Char_S -> {
        assert(type.getSize() == 1L)
        CharType
    }

    CXTypeKind.CXType_UChar, CXTypeKind.CXType_UShort,
    CXTypeKind.CXType_UInt, CXTypeKind.CXType_ULong, CXTypeKind.CXType_ULongLong -> IntegerType(
            size = type.getSize().toInt(),
            isSigned = false,
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

    CXTypeKind.CXType_SChar, CXTypeKind.CXType_Short,
    CXTypeKind.CXType_Int, CXTypeKind.CXType_Long, CXTypeKind.CXType_LongLong -> IntegerType(
            size = type.getSize().toInt(),
            isSigned = true,
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

    CXTypeKind.CXType_Float, CXTypeKind.CXType_Double -> FloatingType(
            size = type.getSize().toInt(),
            spelling = clang_getTypeSpelling(type).convertAndDispose()
    )

    CXTypeKind.CXType_Bool -> BoolType

    else -> UnsupportedType
}

internal fun parseTranslationUnit(
        index: CXIndex,
        sourceFile: File,
        compilerArgs: List<String>,
        options: Int
): CXTranslationUnit {
    memScoped {
        val result = clang_parseTranslationUnit(
                index,
                sourceFile.absolutePath,
                compilerArgs.toNativeStringArray(memScope), compilerArgs.size,
                null, 0,
                options
        )!!

        return result
    }
}

internal fun NativeLibrary.parse(index: CXIndex, options: Int = 0): CXTranslationUnit =
        parseTranslationUnit(index, this.createTempSource(), this.compilerArgs, options)

internal data class Diagnostic(val severity: CXDiagnosticSeverity, val format: String,
                               val location: CValue<CXSourceLocation>)

internal fun CXTranslationUnit.getDiagnostics(): Sequence<Diagnostic> {
    val numDiagnostics = clang_getNumDiagnostics(this)
    return (0 until numDiagnostics).asSequence()
            .map { index ->
                val diagnostic = clang_getDiagnostic(this, index)
                try {
                    val severity = clang_getDiagnosticSeverity(diagnostic)

                    val format = clang_formatDiagnostic(diagnostic, clang_defaultDiagnosticDisplayOptions())
                            .convertAndDispose()

                    val location = clang_getDiagnosticLocation(diagnostic)

                    Diagnostic(severity, format, location)
                } finally {
                    clang_disposeDiagnostic(diagnostic)
                }
            }
}

internal fun CXTranslationUnit.getCompileErrors(): Sequence<String> =
        getDiagnostics().filter { it.isError() }.map { it.format }

private fun Diagnostic.isError() = (severity == CXDiagnosticSeverity.CXDiagnostic_Error) ||
        (severity == CXDiagnosticSeverity.CXDiagnostic_Fatal)

internal fun CXTranslationUnit.hasCompileErrors() = (this.getCompileErrors().firstOrNull() != null)

internal fun CXTranslationUnit.ensureNoCompileErrors(): CXTranslationUnit {
    val firstError = this.getCompileErrors().firstOrNull() ?: return this
    throw Error(firstError)
}

internal typealias CursorVisitor = (cursor: CValue<CXCursor>, parent: CValue<CXCursor>) -> CXChildVisitResult

internal fun visitChildren(parent: CValue<CXCursor>, visitor: CursorVisitor) {
    val visitorStableRef = StableRef.create(visitor)
    try {
        val clientData = visitorStableRef.asCPointer()
        clang_visitChildren(parent, staticCFunction { cursorIt, parentIt, clientDataIt ->
            val visitorIt = clientDataIt!!.asStableRef<CursorVisitor>().get()
            visitorIt(cursorIt, parentIt)
        }, clientData)
    } finally {
        visitorStableRef.dispose()
    }
}

internal fun visitChildren(translationUnit: CXTranslationUnit, visitor: CursorVisitor) =
        visitChildren(clang_getTranslationUnitCursor(translationUnit), visitor)

internal fun getFields(type: CValue<CXType>): List<CValue<CXCursor>> {
    val result = mutableListOf<CValue<CXCursor>>()
    val resultStableRef = StableRef.create(result)
    try {
        val clientData = resultStableRef.asCPointer()

        @Suppress("NAME_SHADOWING")
        clang_Type_visitFields(type, staticCFunction { cursor, clientData ->
            val result = clientData!!.asStableRef<MutableList<CValue<CXCursor>>>().get()
            result.add(cursor)
            CXVisitorResult.CXVisit_Continue
        }, clientData)

    } finally {
        resultStableRef.dispose()
    }

    return result
}

internal fun CValue<CXCursor>.isLeaf(): Boolean {
    var hasChildren = false

    visitChildren(this) { _, _ ->
        hasChildren = true
        CXChildVisitResult.CXChildVisit_Break
    }

    return !hasChildren
}

internal fun List<String>.toNativeStringArray(scope: AutofreeScope): CArrayPointer<CPointerVar<ByteVar>> {
    return scope.allocArray(this.size) { index ->
        this.value = this@toNativeStringArray[index].cstr.getPointer(scope)
    }
}

val NativeLibrary.preambleLines: List<String>
    get() = this.includes.map { "#include <$it>" } + this.additionalPreambleLines

internal fun Appendable.appendPreamble(library: NativeLibrary) = this.apply {
    library.preambleLines.forEach {
        this.appendln(it)
    }
}

/**
 * Creates temporary source file which includes the library.
 */
internal fun NativeLibrary.createTempSource(): File {
    val result = createTempFile(suffix = ".${language.sourceFileExtension}")
    result.deleteOnExit()

    result.bufferedWriter().use { writer ->
        writer.appendPreamble(this)
    }

    return result
}

/**
 * Precompiles the headers of this library.
 *
 * @return the library which includes the precompiled header instead of original ones.
 */
internal fun NativeLibrary.precompileHeaders(): NativeLibrary {
    val precompiledHeader = createTempFile(suffix = ".pch").apply { this.deleteOnExit() }

    val index = clang_createIndex(excludeDeclarationsFromPCH = 0, displayDiagnostics = 0)!!
    try {
        val options = CXTranslationUnit_ForSerialization
        val translationUnit = this.parse(index, options)
        try {
            translationUnit.ensureNoCompileErrors()
            clang_saveTranslationUnit(translationUnit, precompiledHeader.absolutePath, 0)
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }

    return this.copy(
            includes = emptyList(),
            additionalPreambleLines = emptyList(),
            compilerArgs = this.compilerArgs + listOf("-include-pch", precompiledHeader.absolutePath)
    )
}

internal fun NativeLibrary.includesDeclaration(cursor: CValue<CXCursor>): Boolean {
    return if (this.excludeSystemLibs) {
        clang_Location_isInSystemHeader(clang_getCursorLocation(cursor)) == 0
    } else {
        true
    }
}

private fun CXTranslationUnit.getErrorLineNumbers(): Sequence<Int> =
        getDiagnostics().filter {
            it.isError()
        }.map {
            memScoped {
                val lineNumberVar = alloc<IntVar>()
                clang_getFileLocation(it.location, null, lineNumberVar.ptr, null, null)
                lineNumberVar.value
            }
        }

/**
 * For each list of lines, checks if the code fragment composed from these lines is compilable against given library.
 */
fun List<List<String>>.mapFragmentIsCompilable(originalLibrary: NativeLibrary): List<Boolean> {
    val library = originalLibrary
            .copy(compilerArgs = originalLibrary.compilerArgs + "-ferror-limit=0")
            .precompileHeaders()

    val indicesOfNonCompilable = mutableSetOf<Int>()

    val fragmentsToCheck = this.withIndex().toMutableList()

    val index = clang_createIndex(excludeDeclarationsFromPCH = 1, displayDiagnostics = 0)!!
    try {
        val sourceFile = library.createTempSource()
        val translationUnit = parseTranslationUnit(index, sourceFile, library.compilerArgs, options = 0)
        try {
            translationUnit.ensureNoCompileErrors()
            while (fragmentsToCheck.isNotEmpty()) {
                // Combine all fragments to be checked in a single file:
                sourceFile.bufferedWriter().use { writer ->
                    writer.appendPreamble(library)
                    fragmentsToCheck.forEach {
                        it.value.forEach {
                            assert(!it.contains('\n'))
                            writer.appendln(it)
                        }
                    }
                }

                clang_reparseTranslationUnit(translationUnit, 0, null, 0)
                val errorLineNumbers = translationUnit.getErrorLineNumbers().toSet()

                // Retain only those fragments that contain compilation error locations:
                var lastLineNumber = library.preambleLines.size
                fragmentsToCheck.retainAll {
                    val firstLineNumber = lastLineNumber + 1
                    lastLineNumber += it.value.size
                    (firstLineNumber .. lastLineNumber).any { it in errorLineNumbers }
                }

                if (fragmentsToCheck.isNotEmpty()) {
                    // The first fragment is now known to be non-compilable.
                    val firstFragment = fragmentsToCheck.removeAt(0)
                    indicesOfNonCompilable.add(firstFragment.index)
                }

                // The remaining fragments was potentially influenced by the first one,
                // and thus require to be checked again.
            }
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }

    return this.indices.map { it !in indicesOfNonCompilable }
}

internal interface Indexer {
    /**
     * Called when entered main file.
     */
    fun enteredMainFile(file: CXFile) {}

    /**
     * Called when a file gets #included/#imported.
     */
    fun ppIncludedFile(info: CXIdxIncludedFileInfo) {}

    /**
     * Called to index a declaration.
     */
    fun indexDeclaration(info: CXIdxDeclInfo) {}
}

internal fun indexTranslationUnit(index: CXIndex, translationUnit: CXTranslationUnit, options: Int, indexer: Indexer) {
    val indexerStableRef = StableRef.create(indexer)
    try {
        val clientData = indexerStableRef.asCPointer()
        memScoped {
            val indexerCallbacks = alloc<IndexerCallbacks>().apply {
                abortQuery = null
                diagnostic = null
                enteredMainFile = staticCFunction { clientData, mainFile, _ ->
                    @Suppress("NAME_SHADOWING")
                    val indexer = clientData!!.asStableRef<Indexer>().get()
                    indexer.enteredMainFile(mainFile!!)
                    // We must ensure only interop types exist in function signature.
                    @Suppress("USELESS_CAST")
                    null as CXIdxClientFile?
                }
                ppIncludedFile = staticCFunction { clientData, info ->
                    @Suppress("NAME_SHADOWING")
                    val indexer = clientData!!.asStableRef<Indexer>().get()
                    indexer.ppIncludedFile(info!!.pointed)
                    // We must ensure only interop types exist in function signature.
                    @Suppress("USELESS_CAST")
                    null as CXIdxClientFile?
                }
                importedASTFile = null
                startedTranslationUnit = null
                indexDeclaration = staticCFunction { clientData, info ->
                    @Suppress("NAME_SHADOWING")
                    val nativeIndex = clientData!!.asStableRef<Indexer>().get()
                    nativeIndex.indexDeclaration(info!!.pointed)
                }
                indexEntityReference = null
            }

            val indexAction = clang_IndexAction_create(index)
            try {
                val result = clang_indexTranslationUnit(indexAction, clientData,
                        indexerCallbacks.ptr, IndexerCallbacks.size.toInt(), options, translationUnit)

                if (result != 0) {
                    throw Error("clang_indexTranslationUnit returned $result")
                }
            } finally {
                clang_IndexAction_dispose(indexAction)
            }
        }
    } finally {
        indexerStableRef.dispose()
    }
}

internal class ModulesMap(
        val library: NativeLibrary,
        val translationUnit: CXTranslationUnit
) : Closeable {

    private val index: CXIndex
    private val translationUnitWithModules: CXTranslationUnit

    init {
        index = clang_createIndex(0, 0)!!
        try {
            translationUnitWithModules =
                    library
                            .copy(compilerArgs = library.compilerArgs + "-fmodules")
                            .parse(index)

            try {
                translationUnitWithModules.ensureNoCompileErrors()
            } catch (e: Throwable) {
                clang_disposeTranslationUnit(translationUnitWithModules)
                throw e
            }

        } catch (e: Throwable) {
            clang_disposeIndex(index)
            throw e
        }
    }

    override fun close() {
        try {
            clang_disposeTranslationUnit(translationUnitWithModules)
        } finally {
            clang_disposeIndex(index)
        }
    }

    data class Module(private val cxModule: CXModule)

    fun getModule(file: CXFile): Module? {
        // `file` is bound to `translationUnit`, however `translationUnitWithModules` is used to access modules.
        // Find the corresponding file in `translationUnitWithModules`:
        val fileInTuWithModules =
                clang_getFile(translationUnitWithModules, clang_getFileName(file).convertAndDispose())!!

        return clang_getModuleForFile(translationUnitWithModules, fileInTuWithModules)?.let { Module(it) }
    }
}

fun HeaderInclusionPolicy.includeAll(headerName: String?, headerId: HeaderId): Boolean =
        !this.excludeUnused(headerName) && !this.excludeAll(headerId)

internal fun getHeaderId(library: NativeLibrary, header: CXFile?): HeaderId {
    if (header == null) {
        return HeaderId("builtins")
    }

    val filePath = clang_getFileName(header).convertAndDispose()
    return library.headerToIdMapper.getHeaderId(filePath)
}

internal fun getFilteredHeaders(
        nativeIndex: NativeIndexImpl,
        index: CXIndex,
        translationUnit: CXTranslationUnit
): Set<CXFile?> = getHeaders(nativeIndex.library, index, translationUnit).ownHeaders

class NativeLibraryHeaders<Header>(val ownHeaders: Set<Header>, val importedHeaders: Set<Header>)

internal fun getHeaders(
        library: NativeLibrary,
        index: CXIndex,
        translationUnit: CXTranslationUnit
): NativeLibraryHeaders<CXFile?> {
    val ownHeaders = mutableSetOf<CXFile?>()
    val allHeaders = mutableSetOf<CXFile?>(null)
    val topLevelFiles = mutableListOf<CXFile>()
    var mainFile: CXFile? = null

    indexTranslationUnit(index, translationUnit, 0, object : Indexer {
        val headerToName = mutableMapOf<CXFile, String>()
        // The *name* of the header here is the path relative to the include path element., e.g. `curl/curl.h`.

        override fun enteredMainFile(file: CXFile) {
            mainFile = file
            allHeaders += file
        }

        override fun ppIncludedFile(info: CXIdxIncludedFileInfo) {
            val includeLocation = clang_indexLoc_getCXSourceLocation(info.hashLoc.readValue())
            val file = info.file!!

            allHeaders += file

            if (clang_Location_isFromMainFile(includeLocation) != 0) {
                topLevelFiles.add(file)
            }

            val name = info.filename!!.toKString()
            val headerName = if (info.isAngled != 0) {
                // If the header is included with `#include <$name>`, then `name` is probably
                // the path relative to the include path element.
                name
            } else {
                // If it is included with `#include "$name"`, then `name` can also be the path relative to the includer.
                val includerFile = includeLocation.getContainingFile()!!
                val includerName = headerToName[includerFile] ?: ""
                val includerPath = clang_getFileName(includerFile).convertAndDispose()

                if (clang_getFile(translationUnit, Paths.get(includerPath).resolveSibling(name).toString()) == file) {
                    // included file is accessible from the includer by `name` used as relative path, so
                    // `name` seems to be relative to the includer:
                    Paths.get(includerName).resolveSibling(name).normalize().toString()
                } else {
                    name
                }
            }

            headerToName[file] = headerName
            if (library.headerInclusionPolicy.includeAll(headerName, getHeaderId(library, file))) {
                ownHeaders.add(file)
            }
        }
    })

    if (library.excludeDepdendentModules) {
        ModulesMap(library, translationUnit).use { modulesMap ->
            val topLevelModules = topLevelFiles.map { modulesMap.getModule(it) }.toSet()
            ownHeaders.removeAll {
                val module = modulesMap.getModule(it!!)
                module !in topLevelModules
            }
            // Note: if some of the top-level headers don't belong to modules,
            // then all non-modular headers are included.
        }
    } else {
        if (library.headerInclusionPolicy.includeAll(headerName = null, headerId = getHeaderId(library, null))) {
            // Builtins.
            ownHeaders.add(null)
        }
    }

    ownHeaders.add(mainFile!!)

    return NativeLibraryHeaders(ownHeaders, allHeaders - ownHeaders)
}

fun NativeLibrary.getHeaderPaths(): NativeLibraryHeaders<String> {
    val index = clang_createIndex(0, 0)!!
    try {
        val translationUnit =
                this.parse(index, options = CXTranslationUnit_DetailedPreprocessingRecord).ensureNoCompileErrors()
        try {

            fun getPath(file: CXFile?) = if (file == null) "<builtins>" else clang_getFileName(file).convertAndDispose()

            val headers = getHeaders(this, index, translationUnit)
            return NativeLibraryHeaders(
                    headers.ownHeaders.map(::getPath).toSet(),
                    headers.importedHeaders.map(::getPath).toSet()
            )
        } finally {
            clang_disposeTranslationUnit(translationUnit)
        }
    } finally {
        clang_disposeIndex(index)
    }
}

fun ObjCMethod.replaces(other: ObjCMethod): Boolean =
        this.isClass == other.isClass && this.selector == other.selector

fun ObjCProperty.replaces(other: ObjCProperty): Boolean =
        this.getter.replaces(other.getter)

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    DigestInputStream(this.inputStream(), digest).use { dis ->
        val buffer = ByteArray(8192)
        // Read all bytes:
        while (dis.read(buffer, 0, buffer.size) != -1) {}
    }
    // Convert to hex:
    return digest.digest().joinToString("") {
        Integer.toHexString((it.toInt() and 0xff) + 0x100).substring(1)
    }
}

fun headerContentsHash(filePath: String) = File(filePath).sha256()

internal fun CValue<CXSourceLocation>.getContainingFile(): CXFile? = memScoped {
    val fileVar = alloc<CXFileVar>()
    clang_getFileLocation(this@getContainingFile, fileVar.ptr, null, null, null)
    fileVar.value
}

@JvmName("getFileContainingCursor")
internal fun getContainingFile(cursor: CValue<CXCursor>): CXFile? {
    return clang_getCursorLocation(cursor).getContainingFile()
}

private fun createVfsOverlayFileContents(virtualPathToReal: Map<Path, Path>): ByteArray {
    val overlay = clang_VirtualFileOverlay_create(0)

    try {
        fun addFileMapping(realPath: Path, virtualPath: Path) {
            clang_VirtualFileOverlay_addFileMapping(
                    overlay,
                    virtualPath = virtualPath.toAbsolutePath().toString(),
                    realPath = realPath.toAbsolutePath().toString()
            )
        }

        virtualPathToReal.forEach { virtualPath, realPath ->
            if (Files.isDirectory(realPath)) {
                realPath.toFile().walkTopDown().forEach {
                    if (!it.isDirectory) {
                        addFileMapping(
                                realPath = it.toPath(),
                                virtualPath = virtualPath.resolve(realPath.relativize(it.toPath()))
                        )
                    }
                }
            } else {
                addFileMapping(realPath = realPath, virtualPath = virtualPath)
            }
        }

        memScoped {
            val bufferVar = alloc<CPointerVar<ByteVar>>().apply { value = null }
            val bufferSizeVar = alloc<IntVar>()

            val res = clang_VirtualFileOverlay_writeToBuffer(overlay, 0, bufferVar.ptr, bufferSizeVar.ptr)
            if (res != CXErrorCode.CXError_Success) {
                // TODO: shall we free the buffer in this case?
                error(res)
            }

            return bufferVar.value!!.readBytes(bufferSizeVar.value)
        }
    } finally {
        clang_VirtualFileOverlay_dispose(overlay)
    }
}

fun createVfsOverlayFile(virtualPathToReal: Map<Path, Path>): Path {
    val bytes = createVfsOverlayFileContents(virtualPathToReal)

    return createTempFile(prefix = "konan", suffix = ".vfsoverlay").apply {
        bufferedWriter().use {
            writeBytes(bytes)
        }
        deleteOnExit()
    }.toPath()
}

tailrec fun Type.unwrapTypedefs(): Type = if (this is Typedef) {
    this.def.aliased.unwrapTypedefs()
} else {
    this
}

fun Type.canonicalIsPointerToChar(): Boolean {
    val unwrappedType = this.unwrapTypedefs()
    return unwrappedType is PointerType && unwrappedType.pointeeType.unwrapTypedefs() == CharType
}
