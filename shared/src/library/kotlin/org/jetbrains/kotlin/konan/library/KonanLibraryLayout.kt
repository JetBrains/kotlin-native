/**
 * Copyright 2010-2019 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.AbstractFile
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.IrKotlinLibraryLayout
import org.jetbrains.kotlin.library.KotlinLibraryLayout
import org.jetbrains.kotlin.library.MetadataKotlinLibraryLayout

interface TargetedKotlinLibraryLayout<T: AbstractFile> : KotlinLibraryLayout<T> {
    val target: KonanTarget?
        // This is a default implementation. Can't make it an assignment.
        get() = null
    val targetsDir: T
        get() = componentDir.child("targets") as T
    val targetDir: T
        get() = targetsDir.child(target!!.visibleName) as T
    val includedDir: T
        get() = targetDir.child("included") as T
}

interface BitcodeKotlinLibraryLayout<T: AbstractFile> : TargetedKotlinLibraryLayout<T>, KotlinLibraryLayout<T> {
    val kotlinDir: T
        get() = targetDir.child("kotlin") as T
    val nativeDir: T
        get() = targetDir.child("native") as T
    // TODO: Experiment with separate bitcode files.
    // Per package or per class.
    val mainBitcodeFile: T
        get() = kotlinDir.child("program.kt.bc") as T
    val mainBitcodeFileName: String
        get() = mainBitcodeFile.path
}

interface KonanLibraryLayout<T: AbstractFile> :
        MetadataKotlinLibraryLayout<T>,
        BitcodeKotlinLibraryLayout<T>,
        IrKotlinLibraryLayout<T>
