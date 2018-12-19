package symbolication

import platform.posix.*
import platform.darwin.*

import kotlinx.cinterop.*
import CoreSymbolication.*

fun main(args: Array<String>) {
    val program = if (args.size > 0 && args[0].isNotEmpty())
        args[0] else "/Users/nikolay.igotti/kotlin/kotlin-native/samples/symbolication/test.kexe.dSYM/Contents/Resources/DWARF/test.kexe"
    val arch = if (args.size > 1 && args[1].isNotEmpty()) args[1] else "current"
    val input = generateSequence { readLine() }
    analyzeTrace(program, arch, input)
}

fun archToCpuName(arch: String): cpu_type_t = when (arch) {
    "x64", "x86_64" -> CPU_TYPE_X86_64
    "current" -> CSArchitectureGetCurrent()
    else -> TODO("unsupported $arch")
}

fun analyzeTrace(program: String, arch: String, input: Sequence<String>) = memScoped {
    val symbolicator = CSSymbolicatorCreateWithPathAndArchitecture(program, archToCpuName(arch))
    if (CSIsNull(symbolicator)) throw Error("Cannot create symbolicator")
    val matcher = "0x([0-9a-f]+)\\s+([^+]+)\\+\\s\\d+\\s+\\[0x([0-9a-f]+)\\]".toRegex()
    for (line in input) {
        val match = matcher.find(line)
        var result = line
        if (match != null) {
            val address = match.groupValues[1].toLong(16)
            val functionName = match.groupValues[2].trim()
            val base = match.groupValues[3].toLong(16)
            val imageOffset = (address - base).toULong()
            var owner = CSSymbolicatorGetSymbolOwnerWithAddressAtTime(symbolicator, imageOffset, kCSNow)
            val ownerBase = CSSymbolOwnerGetBaseAddress(owner)
            if (!CSIsNull(owner)) {
                val sourceInfo = CSSymbolOwnerGetSourceInfoWithAddress(owner, (imageOffset + ownerBase).convert())
                if (!CSIsNull(sourceInfo)) {
                    val fileName = CSSourceInfoGetFilename(sourceInfo)?.toKString()
                    val lineNumber = CSSourceInfoGetLineNumber(sourceInfo) + 1u
                    result = matcher.replaceFirst(line, "$functionName $fileName:$lineNumber")
                }
            }
        }
        println(result)
    }
    CSRelease(symbolicator)
}
