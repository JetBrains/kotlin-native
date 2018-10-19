/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.descriptors.SourceFile

private class DeserializedSourceFile(val path: String, val index: Int) : SourceFile {
    override fun getName(): String? = path

    override fun equals(other: Any?): Boolean {
        return other is DeserializedSourceFile && index == other.index
    }
}

class SourceFileMap {
    private val sourceToIndex = mutableMapOf<SourceFile, Int>()
    private val indexToSource = mutableMapOf<Int, SourceFile>()

    fun assign(file: SourceFile): Int {
        return sourceToIndex.getOrPut(file) {
            sourceToIndex.size
        }
    }

    fun provide(fileName: String, index: Int) {
        indexToSource[index] = DeserializedSourceFile(fileName, index)
    }

    fun sourceFile(index: Int): SourceFile =
            indexToSource[index] ?: throw Error("Unknown file for $index")

    val files get() =
        sourceToIndex.keys.sortedBy {
            sourceToIndex[it]
        }
}