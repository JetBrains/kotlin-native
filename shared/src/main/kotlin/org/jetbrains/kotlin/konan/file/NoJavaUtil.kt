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

package org.jetbrains.kotlin.konan.file

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URI
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

data class File constructor(internal val javaPath: Path) {
    constructor(parent: Path, child: String): this(parent.resolve(child))
    constructor(parent: File, child: String): this(parent.javaPath.resolve(child))
    constructor(path: String): this(Paths.get(path))
    constructor(parent: String, child: String): this(Paths.get(parent, child))

    val path: String
        get() = javaPath.toString()
    val absolutePath: String
        get() = javaPath.toAbsolutePath().toString()
    val absoluteFile: File
        get() = File(absolutePath)
    val name: String
        get() = javaPath.fileName.toString()
    val parent: String
        get() = javaPath.parent.toString()

    val exists 
        get() = Files.exists(javaPath)
    val isDirectory 
        get() = Files.isDirectory(javaPath)
    val isFile 
        get() = Files.isRegularFile(javaPath)
    val isAbsolute 
        get() = javaPath.isAbsolute()
    val listFiles: List<File>
        get() = Files.newDirectoryStream(javaPath).use { stream -> stream.map { File(it) } }

    fun child(name: String) = File(this, name)

    fun copyTo(destination: File) {
        Files.copy(javaPath, destination.javaPath, StandardCopyOption.REPLACE_EXISTING) 
    }

    fun recursiveCopyTo(destination: File) {
        val sourcePath = javaPath
        val destPath = destination.javaPath
        sourcePath.recursiveCopyTo(destPath)
    }

    fun mkdirs() = Files.createDirectories(javaPath)
    fun delete() = Files.deleteIfExists(javaPath)
    fun deleteRecursively() = postorder{Files.delete(it)}
    fun deleteOnExitRecursively() = preorder{File(it).deleteOnExit()}

    fun preorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object: SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            task(file!!)
            return FileVisitResult.CONTINUE
        }

        override fun preVisitDirectory(dir: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            task(dir!!)
            return FileVisitResult.CONTINUE
        }
        })

    }

    fun postorder(task: (Path) -> Unit) {
        if (!this.exists) return

        Files.walkFileTree(javaPath, object: SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path?, attrs: BasicFileAttributes?): FileVisitResult {
            task(file!!)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path?, exc: java.io.IOException?): FileVisitResult {
            task(dir!!)
            return FileVisitResult.CONTINUE
        }
        })

    }
    fun deleteOnExit() {
        // Works only on the default file system, 
        // but that's okay for now.
        javaPath.toFile().deleteOnExit()
    }
    fun readBytes() = Files.readAllBytes(javaPath)
    fun writeBytes(bytes: ByteArray) = Files.write(javaPath, bytes)
    fun appendBytes(bytes: ByteArray)
        = Files.write(javaPath, bytes, StandardOpenOption.APPEND)

    fun writeLines(lines: Iterable<String>) {
        Files.write(javaPath, lines)
    }

    fun forEachLine(action: (String) -> Unit) {
        Files.lines(javaPath).use { lines ->
            lines.forEach { action(it) }
        }
    }

    override fun toString() = path

    // TODO: Consider removeing these after konanazing java.util.Properties.
    fun bufferedReader() = Files.newBufferedReader(javaPath)
    fun outputStream() = Files.newOutputStream(javaPath)

    companion object {
        val userDir
            get() = File(System.getProperty("user.dir"))

        val userHome
            get() = File(System.getProperty("user.home"))

        val jdkHome
            get() = File(System.getProperty("java.home"))

    }
}

fun String.File(): File = File(this)
fun Path.File(): File = File(this)

fun createTempFile(name: String, suffix: String? = null) 
    = Files.createTempFile(name, suffix).File()
fun createTempDir(name: String): File 
    = Files.createTempDirectory(name).File()

private val File.zipUri: URI
        get() = URI.create("jar:${this.toPath().toUri()}")

fun File.zipFileSystem(mutable: Boolean = false): FileSystem {
    val zipUri = this.zipUri
    val attributes = hashMapOf("create" to mutable.toString())
    return try {
        FileSystems.newFileSystem(zipUri, attributes, null)
    } catch (e: FileSystemAlreadyExistsException) {
        FileSystems.getFileSystem(zipUri)
    }
}

fun File.mutableZipFileSystem() = this.zipFileSystem(mutable = true)

fun File.zipPath(path: String): Path
    = this.zipFileSystem().getPath(path)

val File.asZipRoot: File
    get() = File(this.zipPath("/"))

val File.asWritableZipRoot: File
    get() = File(this.mutableZipFileSystem().getPath("/"))

private fun File.toPath() = Paths.get(this.path)

fun File.zipDirAs(unixFile: File) {
    val zipRoot = unixFile.asWritableZipRoot
    this.recursiveCopyTo(zipRoot)
    zipRoot.javaPath.fileSystem.close()
}

fun Path.recursiveCopyTo(destPath: Path) {
    val sourcePath = this
    Files.walk(sourcePath).forEach next@ { oldPath ->

        val relative = sourcePath.relativize(oldPath)
        val destFs = destPath.getFileSystem()
        // We are copying files between file systems, 
        // so pass the relative path through the String.
        val newPath = destFs.getPath(destPath.toString(), relative.toString())

        // File systems don't allow replacing an existing root.
        if (newPath == newPath.getRoot()) return@next
        if (Files.isDirectory(newPath)) {
            Files.createDirectories(newPath)
        } else {
            Files.copy(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

fun bufferedReader(errorStream: InputStream?) = BufferedReader(InputStreamReader(errorStream))

// stdlib `use` function adapted for AutoCloseable.
inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this?.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this?.close()
        }
    }
}

fun Path.unzipTo(directory: Path) {
    val zipUri = URI.create("jar:" + this.toUri())
    FileSystems.newFileSystem(zipUri, emptyMap<String, Any?>(), null).use { zipfs ->
        val zipPath = zipfs.getPath("/")
        zipPath.recursiveCopyTo(directory)
    }
}
