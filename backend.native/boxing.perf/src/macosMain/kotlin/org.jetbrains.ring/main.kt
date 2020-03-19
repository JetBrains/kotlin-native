package org.jetbrains.ring

import kotlin.system.measureNanoTime
import kotlin.native.internal.GC
import platform.posix.*
import kotlinx.cinterop.*

fun writeToFile(fileName: String, text: String) {
    val file = fopen(fileName, "at") ?: error("Cannot write file '$fileName'")
    try {
        if (fputs(text, file) == EOF) throw Error("File write error")
    } finally {
        fclose(file)
    }
}

open class BenchmarkEntry<R>(val benchmark: () -> R) {
    open fun run() {
        benchmark()
    }

    companion object {
        fun <T : Any, R> create(ctor: () -> T, benchmark: T.() -> R) = BenchmarkEntry {
            ctor().benchmark()
        }
        fun <T : Any, R> createWithInstance(instance: T, benchmark: T.() -> R) = BenchmarkEntry {
            instance.benchmark()
        }
    }

}

class TimerBenchmarkEntry<R>(val entry: BenchmarkEntry<R>): BenchmarkEntry<R>(entry.benchmark) {
    private val warmupIterations = 20000
    private val repeatIterations = 50000

    override fun run() {
        for (i in 1..warmupIterations) {
            super.run()
            GC.collect()
        }
        var i = repeatIterations
        GC.collect()
        val result = mutableListOf<Long>()
        for (i in 1..repeatIterations) {
            result.add(measureNanoTime { super.run() })
            GC.collect()
        }
        val answer = """
            Results:             ${result.joinToString()}
            Min:                 ${result.min()}
            Max:                 ${result.max()}
            Mean:                ${result.sum() / repeatIterations}
            Median:              ${result.sorted()[result.size / 2]}
        """.trimIndent()
        writeToFile("result.txt", "$answer\n\n")
    }
}

val elvisBenchmark = ElvisBenchmark()

val benchmarks = mutableMapOf(
        "AbstractMethod.sortStrings"                        to BenchmarkEntry.create(::AbstractMethodBenchmark) { sortStrings() },
        "AbstractMethod.sortStringsWithComparator"          to BenchmarkEntry.create(::AbstractMethodBenchmark) { sortStringsWithComparator() },

        "Calls.finalMethodCall"                             to BenchmarkEntry.create(::CallsBenchmark) { finalMethodCall() },
        "Calls.classOpenMethodCall_MonomorphicCallsite"     to BenchmarkEntry.create(::CallsBenchmark) { classOpenMethodCall_MonomorphicCallsite() },
        "Calls.classOpenMethodCall_BimorphicCallsite"       to BenchmarkEntry.create(::CallsBenchmark) { classOpenMethodCall_BimorphicCallsite() },
        "Calls.interfaceMethodCall_BimorphicCallsite"       to BenchmarkEntry.create(::CallsBenchmark) { interfaceMethodCall_BimorphicCallsite() },
        "Calls.interfaceMethodCall_TrimorphicCallsite"      to BenchmarkEntry.create(::CallsBenchmark) { interfaceMethodCall_TrimorphicCallsite() },
        "Calls.interfaceMethodCall_HexamorphicCallsite"     to BenchmarkEntry.create(::CallsBenchmark) { interfaceMethodCall_HexamorphicCallsite() },
        "Calls.returnBoxUnboxFolding"                       to BenchmarkEntry.create(::CallsBenchmark) { returnBoxUnboxFolding() },
        "Calls.parameterBoxUnboxFolding"                    to BenchmarkEntry.create(::CallsBenchmark) { parameterBoxUnboxFolding() },

        "Casts.classCast"                                   to BenchmarkEntry.create(::CastsBenchmark) { classCast() },
        "Casts.interfaceCast"                               to BenchmarkEntry.create(::CastsBenchmark) { interfaceCast() },

        "ClassArray.copy"                                   to BenchmarkEntry.create(::ClassArrayBenchmark) { copy() },
        "ClassArray.copyManual"                             to BenchmarkEntry.create(::ClassArrayBenchmark) { copyManual() },
        "ClassArray.filterAndCount"                         to BenchmarkEntry.create(::ClassArrayBenchmark) { filterAndCount() },
        "ClassArray.filterAndMap"                           to BenchmarkEntry.create(::ClassArrayBenchmark) { filterAndMap() },
        "ClassArray.filterAndMapManual"                     to BenchmarkEntry.create(::ClassArrayBenchmark) { filterAndMapManual() },
        "ClassArray.filter"                                 to BenchmarkEntry.create(::ClassArrayBenchmark) { filter() },
        "ClassArray.filterManual"                           to BenchmarkEntry.create(::ClassArrayBenchmark) { filterManual() },
        "ClassArray.countFilteredManual"                    to BenchmarkEntry.create(::ClassArrayBenchmark) { countFilteredManual() },
        "ClassArray.countFiltered"                          to BenchmarkEntry.create(::ClassArrayBenchmark) { countFiltered() },
        "ClassArray.countFilteredLocal"                     to BenchmarkEntry.create(::ClassArrayBenchmark) { countFilteredLocal() },

        "ClassBaseline.consume"                             to BenchmarkEntry.create(::ClassBaselineBenchmark) { consume() },
        "ClassBaseline.consumeField"                        to BenchmarkEntry.create(::ClassBaselineBenchmark) { consumeField() },
        "ClassBaseline.allocateList"                        to BenchmarkEntry.create(::ClassBaselineBenchmark) { allocateList() },
        "ClassBaseline.allocateArray"                       to BenchmarkEntry.create(::ClassBaselineBenchmark) { allocateArray() },
        "ClassBaseline.allocateListAndFill"                 to BenchmarkEntry.create(::ClassBaselineBenchmark) { allocateListAndFill() },
        "ClassBaseline.allocateListAndWrite"                to BenchmarkEntry.create(::ClassBaselineBenchmark) { allocateListAndWrite() },
        "ClassBaseline.allocateArrayAndFill"                to BenchmarkEntry.create(::ClassBaselineBenchmark) { allocateArrayAndFill() },

        "ClassList.copy"                                    to BenchmarkEntry.create(::ClassListBenchmark) { copy() },
        "ClassList.copyManual"                              to BenchmarkEntry.create(::ClassListBenchmark) { copyManual() },
        "ClassList.filterAndCount"                          to BenchmarkEntry.create(::ClassListBenchmark) { filterAndCount() },
        "ClassList.filterAndMap"                            to BenchmarkEntry.create(::ClassListBenchmark) { filterAndMap() },
        "ClassList.filterAndMapManual"                      to BenchmarkEntry.create(::ClassListBenchmark) { filterAndMapManual() },
        "ClassList.filter"                                  to BenchmarkEntry.create(::ClassListBenchmark) { filter() },
        "ClassList.filterManual"                            to BenchmarkEntry.create(::ClassListBenchmark) { filterManual() },
        "ClassList.countFilteredManual"                     to BenchmarkEntry.create(::ClassListBenchmark) { countFilteredManual() },
        "ClassList.countFiltered"                           to BenchmarkEntry.create(::ClassListBenchmark) { countFiltered() },
        "ClassList.reduce"                                  to BenchmarkEntry.create(::ClassListBenchmark) { reduce() },

        "ClassStream.copy"                                  to BenchmarkEntry.create(::ClassStreamBenchmark) { copy() },
        "ClassStream.copyManual"                            to BenchmarkEntry.create(::ClassStreamBenchmark) { copyManual() },
        "ClassStream.filterAndCount"                        to BenchmarkEntry.create(::ClassStreamBenchmark) { filterAndCount() },
        "ClassStream.filterAndMap"                          to BenchmarkEntry.create(::ClassStreamBenchmark) { filterAndMap() },
        "ClassStream.filterAndMapManual"                    to BenchmarkEntry.create(::ClassStreamBenchmark) { filterAndMapManual() },
        "ClassStream.filter"                                to BenchmarkEntry.create(::ClassStreamBenchmark) { filter() },
        "ClassStream.filterManual"                          to BenchmarkEntry.create(::ClassStreamBenchmark) { filterManual() },
        "ClassStream.countFilteredManual"                   to BenchmarkEntry.create(::ClassStreamBenchmark) { countFilteredManual() },
        "ClassStream.countFiltered"                         to BenchmarkEntry.create(::ClassStreamBenchmark) { countFiltered() },
        "ClassStream.reduce"                                to BenchmarkEntry.create(::ClassStreamBenchmark) { reduce() },

        "CompanionObject.invokeRegularFunction"             to BenchmarkEntry.create(::CompanionObjectBenchmark) { invokeRegularFunction() },

        "CoordinatesSolverBenchmark.solve"                  to BenchmarkEntry.create(::CoordinatesSolverBenchmark) { solve() },

        "DefaultArgument.testOneOfTwo"                      to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testOneOfTwo() },
        "DefaultArgument.testTwoOfTwo"                      to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testTwoOfTwo() },
        "DefaultArgument.testOneOfFour"                     to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testOneOfFour() },
        "DefaultArgument.testFourOfFour"                    to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testFourOfFour() },
        "DefaultArgument.testOneOfEight"                    to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testOneOfEight() },
        "DefaultArgument.testEightOfEight"                  to BenchmarkEntry.create(::DefaultArgumentBenchmark) { testEightOfEight() },

        "Elvis.testElvis1"                                  to BenchmarkEntry.createWithInstance(elvisBenchmark) { testElvis1() },
        "Elvis.testElvis2"                                  to BenchmarkEntry.createWithInstance(elvisBenchmark) { testElvis2() },
        "Elvis.testElvis3"                                  to BenchmarkEntry.createWithInstance(elvisBenchmark) { testElvis3() },
        "Elvis.testElvis4"                                  to BenchmarkEntry.createWithInstance(elvisBenchmark) { testElvis4() },
        "Elvis.unnecessaryElvis1"                           to BenchmarkEntry.createWithInstance(elvisBenchmark) { testUnnecessaryElvis1() },
        "Elvis.unnecessaryElvis2"                           to BenchmarkEntry.createWithInstance(elvisBenchmark) { testUnnecessaryElvis2() },

        "String.stringConcat"                               to BenchmarkEntry.create(::StringBenchmark) { stringConcat() },
        "String.stringConcatNullable"                       to BenchmarkEntry.create(::StringBenchmark) { stringConcatNullable() },
        "String.stringBuilderConcat"                        to BenchmarkEntry.create(::StringBenchmark) { stringBuilderConcat() },
        "String.stringBuilderConcatNullable"                to BenchmarkEntry.create(::StringBenchmark) { stringBuilderConcatNullable() },
        "String.summarizeSplittedCsv"                       to BenchmarkEntry.create(::StringBenchmark) { summarizeSplittedCsv() },

        "Switch.testSparseIntSwitch"                        to BenchmarkEntry.create(::SwitchBenchmark) { testSparseIntSwitch() },
        "Switch.testDenseIntSwitch"                         to BenchmarkEntry.create(::SwitchBenchmark) { testDenseIntSwitch() },
        "Switch.testConstSwitch"                            to BenchmarkEntry.create(::SwitchBenchmark) { testConstSwitch() },
        "Switch.testObjConstSwitch"                         to BenchmarkEntry.create(::SwitchBenchmark) { testObjConstSwitch() },
        "Switch.testVarSwitch"                              to BenchmarkEntry.create(::SwitchBenchmark) { testVarSwitch() },
        "Switch.testStringsSwitch"                          to BenchmarkEntry.create(::SwitchBenchmark) { testStringsSwitch() },
        "Switch.testEnumsSwitch"                            to BenchmarkEntry.create(::SwitchBenchmark) { testEnumsSwitch() },
        "Switch.testDenseEnumsSwitch"                       to BenchmarkEntry.create(::SwitchBenchmark) { testDenseEnumsSwitch() },
        "Switch.testSealedWhenSwitch"                       to BenchmarkEntry.create(::SwitchBenchmark) { testSealedWhenSwitch() },

        "WithIndices.withIndcies"                           to BenchmarkEntry.create(::WithIndices) { withIndices() },
        "WithIndices.withIndciesManual"                     to BenchmarkEntry.create(::WithIndices) { withIndicesManual() }
)

fun main() {
    benchmarks.forEach {
        writeToFile("result.txt", "${it.key}: ")
        TimerBenchmarkEntry(it.value).run()
    }
}