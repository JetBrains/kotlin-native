package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel

data class DeclarationId(val id: Long, val isLocal: Boolean)

class CombinedIrFileReader(file: File) {
    var buffer = file.map(FileChannel.MapMode.READ_ONLY)
    val declarationToOffsetSize = mutableMapOf<DeclarationId, Pair<Int, Int>>()

    init {
        val declarationsCount = buffer.int
        for (i in 0 until declarationsCount) {
            val id = buffer.long
            val isLocal = buffer.int != 0
            val offset = buffer.int
            val size = buffer.int
            declarationToOffsetSize[DeclarationId(id, isLocal)] = offset to size
        }
    }
    fun declarationBytes(id: DeclarationId): ByteArray {
        val offsetSize = declarationToOffsetSize[id] ?: throw Error("No declaration with $id here")
        val result = ByteArray(offsetSize.second)
        // Unfortunately natural "buffer.get(result, offsetSize.first, offsetSize.second)" has
        // nasty bug with bounds checking.
        for (i in 0 until offsetSize.second) {
            result[i] = buffer[i + offsetSize.first]
        }
        return result
    }
}

class CombinedIrFileWriter(val declarationCount: Int) {
    private var currentDeclaration = 0
    private var currentPosition = 0
    private val file = org.jetbrains.kotlin.konan.file.createTempFile("ir").deleteOnExit()
    private val randomAccessFile = RandomAccessFile(file.path, "rw")

    init {
        randomAccessFile.writeInt(declarationCount)
        for (i in 0 until declarationCount) {
            randomAccessFile.writeLong(-1) // id
            randomAccessFile.writeInt(-1)  // isLocal
            randomAccessFile.writeInt(-1)  // offset
            randomAccessFile.writeInt(-1)  // size
        }
        currentPosition = randomAccessFile.filePointer.toInt()
    }

    fun skipDeclaration() {
        currentDeclaration++
    }

    fun addDeclaration(id: DeclarationId, bytes: ByteArray) {
        randomAccessFile.seek((currentDeclaration * 20 + 4).toLong())
        randomAccessFile.writeLong(id.id)
        randomAccessFile.writeInt(if (id.isLocal) 1 else 0)
        randomAccessFile.writeInt(currentPosition)
        randomAccessFile.writeInt(bytes.size)
        randomAccessFile.seek(currentPosition.toLong())
        randomAccessFile.write(bytes)
        assert(randomAccessFile.filePointer < Int.MAX_VALUE.toLong())
        currentPosition = randomAccessFile.filePointer.toInt()
        currentDeclaration++
    }

    fun finishWriting(): File {
        assert(currentDeclaration == declarationCount)
        randomAccessFile.close()
        return file
    }
}

