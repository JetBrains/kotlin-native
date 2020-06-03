package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.file.AbstractFile
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.file.ZippedFile
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.*
import java.nio.file.FileSystem
import java.util.zip.ZipFile

open class TargetedLibraryLayoutImpl(klib: File, component: String, override val target: KonanTarget?) :
    KotlinLibraryLayoutImpl(klib, component), TargetedKotlinLibraryLayout<File> {

    override val extractingToTemp: TargetedKotlinLibraryLayout<File> by lazy {
        ExtractingTargetedLibraryImpl(this)
    }

    override fun directlyFromZip(zipFile: ZipFile): TargetedKotlinLibraryLayout<ZippedFile> =
        FromZipTargetedLibraryImpl(this, zipFile)

}

class BitcodeLibraryLayoutImpl(klib: File, component: String, target: KonanTarget?) :
    TargetedLibraryLayoutImpl(klib, component, target), BitcodeKotlinLibraryLayout<File> {
    override val extractingToTemp: BitcodeKotlinLibraryLayout<File> by lazy {
        ExtractingBitcodeLibraryImpl(this)
    }

    override fun directlyFromZip(zipFile: ZipFile): BitcodeKotlinLibraryLayout<ZippedFile> =
        FromZipBitcodeLibraryImpl(this, zipFile)

}

// TODO: We don't have a right hierarchy for Accesses.
open class TargetedLibraryAccess(klib: File, component: String, val target: KonanTarget?) :
    AbstractLibraryAccess<TargetedKotlinLibraryLayout<File>, TargetedKotlinLibraryLayout<AbstractFile>>(klib) {
    override val layout = TargetedLibraryLayoutImpl(klib, component, target)
}

open class BitcodeLibraryAccess(klib: File, component: String, target: KonanTarget?) :
    AbstractLibraryAccess<BitcodeKotlinLibraryLayout<File>, BitcodeKotlinLibraryLayout<AbstractFile>>(klib) {
    override val layout = BitcodeLibraryLayoutImpl(klib, component, target)
}

private open class FromZipTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl, zipFile: ZipFile) :
    FromZipBaseLibraryImpl(zipped, zipFile), TargetedKotlinLibraryLayout<ZippedFile>

private class FromZipBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl, zipFile: ZipFile) :
    FromZipTargetedLibraryImpl(zipped, zipFile), BitcodeKotlinLibraryLayout<ZippedFile>

open class ExtractingTargetedLibraryImpl(zipped: TargetedLibraryLayoutImpl) :
    ExtractingKotlinLibraryLayout(zipped),
    TargetedKotlinLibraryLayout<File> {

    override val includedDir: File by lazy { zipped.extractDir(zipped.includedDir) }
}

class ExtractingBitcodeLibraryImpl(zipped: BitcodeLibraryLayoutImpl) :
    ExtractingTargetedLibraryImpl(zipped), BitcodeKotlinLibraryLayout<File> {

    override val kotlinDir: File by lazy { zipped.extractDir(zipped.kotlinDir) }
    override val nativeDir: File by lazy { zipped.extractDir(zipped.nativeDir) }
}
