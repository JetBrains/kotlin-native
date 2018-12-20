package symbolication

import platform.darwin.*

import kotlinx.cinterop.*
import CoreSymbolication.*

fun main(args: Array<String>) {
    val program = if (args.size > 0 && args[0].isNotEmpty())
        args[0] else throw Error("please specify .dSYM executable name as first argument")
    val arch = if (args.size > 1 && args[1].isNotEmpty()) args[1] else "current"
    val input = generateSequence { readLine() }
    analyzeTrace(program, arch, input)
}

fun archToCpuName(arch: String): cpu_type_t = when (arch) {
    "x64", "x86_64" -> CPU_TYPE_X86_64
    "arm32" -> CPU_TYPE_ARM
    "arm64" -> CPU_TYPE_ARM64
    "current" -> CSArchitectureGetCurrent()
    else -> TODO("unsupported $arch")
}

fun analyzeTrace(program: String, arch: String, input: Sequence<String>) {
    val symbolicator = CSSymbolicatorCreateWithPathAndArchitecture(program, archToCpuName(arch))
    if (CSIsNull(symbolicator)) throw Error("Cannot create \"$arch\" symbolicator for $program")
    val owner = CSSymbolicatorGetSymbolOwner(symbolicator)
    val imageName = CSSymbolOwnerGetName(owner)!!.toKString()
    val imageBase = CSSymbolOwnerGetBaseAddress(owner)
    // Format is like
    // at 14  test.kexe                           0x0000000106a2e071 Konan_run_start + 113 [0x106a26000]
    val matcher = "(at \\d+\\s+)(\\S+)(\\s+)0x([0-9a-f]+)\\s+([^+]+)\\+\\s\\d+\\s+\\[0x([0-9a-f]+)\\]".toRegex()
    for (line in input) {
        val match = matcher.find(line)
        var result = line
        if (match != null) {
            val atPart =  match.groupValues[1]
            val moduleName =  match.groupValues[2]
            val spaceAfterModuleName = match.groupValues[3]
            val address = match.groupValues[4].toULong(16)
            val functionName = match.groupValues[5].trim()
            val moduleBase = match.groupValues[6].toULong(16)
            val imageAddress = address - moduleBase + imageBase
            if (moduleName == imageName) {
                val sourceInfo = CSSymbolOwnerGetSourceInfoWithAddress(owner, imageAddress)
                if (!CSIsNull(sourceInfo)) {
                    val fileName = CSSourceInfoGetFilename(sourceInfo)?.toKString()
                    val lineNumber = CSSourceInfoGetLineNumber(sourceInfo)
                    val lineNumberString = if (lineNumber != 0u) lineNumber.toString() else "<unknown>"
                    if (fileName != null)
                        result = matcher.replaceFirst(line,
                                "$atPart$moduleName$spaceAfterModuleName$functionName $fileName:$lineNumberString")
                }
            }
        }
        println(result)
    }
    CSRelease(symbolicator)
}
