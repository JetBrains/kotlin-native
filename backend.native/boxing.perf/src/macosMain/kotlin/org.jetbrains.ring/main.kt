package org.jetbrains.ring

class BenchmarkEntry<R> private constructor(val benchmark: () -> R) {
    fun run() {
        benchmark()
    }

    companion object {
        fun <T : Any, R> create(ctor: () -> T, benchmark: T.() -> R) = BenchmarkEntry {
            ctor().benchmark()
        }
    }
}

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

        "Elvis.testElvis1"                                  to BenchmarkEntry.create(::ElvisBenchmark) { testElvis1() },
        "Elvis.testElvis2"                                  to BenchmarkEntry.create(::ElvisBenchmark) { testElvis2() },
        "Elvis.testElvis3"                                  to BenchmarkEntry.create(::ElvisBenchmark) { testElvis3() },
        "Elvis.testElvis4"                                  to BenchmarkEntry.create(::ElvisBenchmark) { testElvis4() },

        "Euler.problem1bySequence"                          to BenchmarkEntry.create(::EulerBenchmark) { problem1bySequence() },
        "Euler.problem1"                                    to BenchmarkEntry.create(::EulerBenchmark) { problem1() },
        "Euler.problem2"                                    to BenchmarkEntry.create(::EulerBenchmark) { problem2() },
        "Euler.problem4"                                    to BenchmarkEntry.create(::EulerBenchmark) { problem4() },
        "Euler.problem8"                                    to BenchmarkEntry.create(::EulerBenchmark) { problem8() },
        "Euler.problem9"                                    to BenchmarkEntry.create(::EulerBenchmark) { problem9() },
        "Euler.problem14"                                   to BenchmarkEntry.create(::EulerBenchmark) { problem14() },
        "Euler.problem14full"                               to BenchmarkEntry.create(::EulerBenchmark) { problem14full() },

        "Fibonacci.calcClassic"                             to BenchmarkEntry.create(::FibonacciBenchmark) { calcClassic() },
        "Fibonacci.calc"                                    to BenchmarkEntry.create(::FibonacciBenchmark) { calc() },
        "Fibonacci.calcWithProgression"                     to BenchmarkEntry.create(::FibonacciBenchmark) { calcWithProgression() },
        "Fibonacci.calcSquare"                              to BenchmarkEntry.create(::FibonacciBenchmark) { calcSquare() },

        "ForLoops.arrayLoop"                                to BenchmarkEntry.create(::ForLoopsBenchmark) { arrayLoop() },
        "ForLoops.intArrayLoop"                             to BenchmarkEntry.create(::ForLoopsBenchmark) { intArrayLoop() },
        "ForLoops.floatArrayLoop"                           to BenchmarkEntry.create(::ForLoopsBenchmark) { floatArrayLoop() },
        "ForLoops.charArrayLoop"                            to BenchmarkEntry.create(::ForLoopsBenchmark) { charArrayLoop() },
        "ForLoops.stringLoop"                               to BenchmarkEntry.create(::ForLoopsBenchmark) { stringLoop() },
        "ForLoops.arrayIndicesLoop"                         to BenchmarkEntry.create(::ForLoopsBenchmark) { arrayIndicesLoop() },
        "ForLoops.intArrayIndicesLoop"                      to BenchmarkEntry.create(::ForLoopsBenchmark) { intArrayIndicesLoop() },
        "ForLoops.floatArrayIndicesLoop"                    to BenchmarkEntry.create(::ForLoopsBenchmark) { floatArrayIndicesLoop() },
        "ForLoops.charArrayIndicesLoop"                     to BenchmarkEntry.create(::ForLoopsBenchmark) { charArrayIndicesLoop() },
        "ForLoops.stringIndicesLoop"                        to BenchmarkEntry.create(::ForLoopsBenchmark) { stringIndicesLoop() },

        "Inline.calculate"                                  to BenchmarkEntry.create(::InlineBenchmark) { calculate() },
        "Inline.calculateInline"                            to BenchmarkEntry.create(::InlineBenchmark) { calculateInline() },
        "Inline.calculateGeneric"                           to BenchmarkEntry.create(::InlineBenchmark) { calculateGeneric() },
        "Inline.calculateGenericInline"                     to BenchmarkEntry.create(::InlineBenchmark) { calculateGenericInline() },

        "IntArray.copy"                                     to BenchmarkEntry.create(::IntArrayBenchmark) { copy() },
        "IntArray.copyManual"                               to BenchmarkEntry.create(::IntArrayBenchmark) { copyManual() },
        "IntArray.filterAndCount"                           to BenchmarkEntry.create(::IntArrayBenchmark) { filterAndCount() },
        "IntArray.filterSomeAndCount"                       to BenchmarkEntry.create(::IntArrayBenchmark) { filterSomeAndCount() },
        "IntArray.filterAndMap"                             to BenchmarkEntry.create(::IntArrayBenchmark) { filterAndMap() },
        "IntArray.filterAndMapManual"                       to BenchmarkEntry.create(::IntArrayBenchmark) { filterAndMapManual() },
        "IntArray.filter"                                   to BenchmarkEntry.create(::IntArrayBenchmark) { filter() },
        "IntArray.filterSome"                               to BenchmarkEntry.create(::IntArrayBenchmark) { filterSome() },
        "IntArray.filterPrime"                              to BenchmarkEntry.create(::IntArrayBenchmark) { filterPrime() },
        "IntArray.filterManual"                             to BenchmarkEntry.create(::IntArrayBenchmark) { filterManual() },
        "IntArray.filterSomeManual"                         to BenchmarkEntry.create(::IntArrayBenchmark) { filterSomeManual() },
        "IntArray.countFilteredManual"                      to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredManual() },
        "IntArray.countFilteredSomeManual"                  to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredSomeManual() },
        "IntArray.countFilteredPrimeManual"                 to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredPrimeManual() },
        "IntArray.countFiltered"                            to BenchmarkEntry.create(::IntArrayBenchmark) { countFiltered() },
        "IntArray.countFilteredSome"                        to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredSome() },
        "IntArray.countFilteredPrime"                       to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredPrime() },
        "IntArray.countFilteredLocal"                       to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredLocal() },
        "IntArray.countFilteredSomeLocal"                   to BenchmarkEntry.create(::IntArrayBenchmark) { countFilteredSomeLocal() },
        "IntArray.reduce"                                   to BenchmarkEntry.create(::IntArrayBenchmark) { reduce() },

        "IntBaseline.consume"                               to BenchmarkEntry.create(::IntBaselineBenchmark) { consume() },
        "IntBaseline.allocateList"                          to BenchmarkEntry.create(::IntBaselineBenchmark) { allocateList() },
        "IntBaseline.allocateArray"                         to BenchmarkEntry.create(::IntBaselineBenchmark) { allocateArray() },
        "IntBaseline.allocateListAndFill"                   to BenchmarkEntry.create(::IntBaselineBenchmark) { allocateListAndFill() },
        "IntBaseline.allocateArrayAndFill"                  to BenchmarkEntry.create(::IntBaselineBenchmark) { allocateArrayAndFill() },

        "IntList.copy"                                      to BenchmarkEntry.create(::IntListBenchmark) { copy() },
        "IntList.copyManual"                                to BenchmarkEntry.create(::IntListBenchmark) { copyManual() },
        "IntList.filterAndCount"                            to BenchmarkEntry.create(::IntListBenchmark) { filterAndCount() },
        "IntList.filterAndMap"                              to BenchmarkEntry.create(::IntListBenchmark) { filterAndMap() },
        "IntList.filterAndMapManual"                        to BenchmarkEntry.create(::IntListBenchmark) { filterAndMapManual() },
        "IntList.filter"                                    to BenchmarkEntry.create(::IntListBenchmark) { filter() },
        "IntList.filterManual"                              to BenchmarkEntry.create(::IntListBenchmark) { filterManual() },
        "IntList.countFilteredManual"                       to BenchmarkEntry.create(::IntListBenchmark) { countFilteredManual() },
        "IntList.countFiltered"                             to BenchmarkEntry.create(::IntListBenchmark) { countFiltered() },
        "IntList.countFilteredLocal"                        to BenchmarkEntry.create(::IntListBenchmark) { countFilteredLocal() },
        "IntList.reduce"                                    to BenchmarkEntry.create(::IntListBenchmark) { reduce() },

        "IntStream.copy"                                    to BenchmarkEntry.create(::IntStreamBenchmark) { copy() },
        "IntStream.copyManual"                              to BenchmarkEntry.create(::IntStreamBenchmark) { copyManual() },
        "IntStream.filterAndCount"                          to BenchmarkEntry.create(::IntStreamBenchmark) { filterAndCount() },
        "IntStream.filterAndMap"                            to BenchmarkEntry.create(::IntStreamBenchmark) { filterAndMap() },
        "IntStream.filterAndMapManual"                      to BenchmarkEntry.create(::IntStreamBenchmark) { filterAndMapManual() },
        "IntStream.filter"                                  to BenchmarkEntry.create(::IntStreamBenchmark) { filter() },
        "IntStream.filterManual"                            to BenchmarkEntry.create(::IntStreamBenchmark) { filterManual() },
        "IntStream.countFilteredManual"                     to BenchmarkEntry.create(::IntStreamBenchmark) { countFilteredManual() },
        "IntStream.countFiltered"                           to BenchmarkEntry.create(::IntStreamBenchmark) { countFiltered() },
        "IntStream.countFilteredLocal"                      to BenchmarkEntry.create(::IntStreamBenchmark) { countFilteredLocal() },
        "IntStream.reduce"                                  to BenchmarkEntry.create(::IntStreamBenchmark) { reduce() },

        "Lambda.noncapturingLambda"                         to BenchmarkEntry.create(::LambdaBenchmark) { noncapturingLambda() },
        "Lambda.noncapturingLambdaNoInline"                 to BenchmarkEntry.create(::LambdaBenchmark) { noncapturingLambdaNoInline() },
        "Lambda.capturingLambda"                            to BenchmarkEntry.create(::LambdaBenchmark) { capturingLambda() },
        "Lambda.capturingLambdaNoInline"                    to BenchmarkEntry.create(::LambdaBenchmark) { capturingLambdaNoInline() },
        "Lambda.mutatingLambda"                             to BenchmarkEntry.create(::LambdaBenchmark) { mutatingLambda() },
        "Lambda.mutatingLambdaNoInline"                     to BenchmarkEntry.create(::LambdaBenchmark) { mutatingLambdaNoInline() },
        "Lambda.methodReference"                            to BenchmarkEntry.create(::LambdaBenchmark) { methodReference() },
        "Lambda.methodReferenceNoInline"                    to BenchmarkEntry.create(::LambdaBenchmark) { methodReferenceNoInline() },

        "Loop.arrayLoop"                                    to BenchmarkEntry.create(::LoopBenchmark) { arrayLoop() },
        "Loop.arrayIndexLoop"                               to BenchmarkEntry.create(::LoopBenchmark) { arrayIndexLoop() },
        "Loop.rangeLoop"                                    to BenchmarkEntry.create(::LoopBenchmark) { rangeLoop() },
        "Loop.arrayListLoop"                                to BenchmarkEntry.create(::LoopBenchmark) { arrayListLoop() },
        "Loop.arrayWhileLoop"                               to BenchmarkEntry.create(::LoopBenchmark) { arrayWhileLoop() },
        "Loop.arrayForeachLoop"                             to BenchmarkEntry.create(::LoopBenchmark) { arrayForeachLoop() },
        "Loop.arrayListForeachLoop"                         to BenchmarkEntry.create(::LoopBenchmark) { arrayListForeachLoop() },

        "MatrixMap.add"                                     to BenchmarkEntry.create(::MatrixMapBenchmark) { add() },

        "ParameterNotNull.invokeOneArgWithNullCheck"        to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeOneArgWithNullCheck() },
        "ParameterNotNull.invokeOneArgWithoutNullCheck"     to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeOneArgWithoutNullCheck() },
        "ParameterNotNull.invokeTwoArgsWithNullCheck"       to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeTwoArgsWithNullCheck() },
        "ParameterNotNull.invokeTwoArgsWithoutNullCheck"    to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeTwoArgsWithoutNullCheck() },
        "ParameterNotNull.invokeEightArgsWithNullCheck"     to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeEightArgsWithNullCheck() },
        "ParameterNotNull.invokeEightArgsWithoutNullCheck"  to BenchmarkEntry.create(::ParameterNotNullAssertionBenchmark) { invokeEightArgsWithoutNullCheck() },

        "PrimeList.calcDirect"                              to BenchmarkEntry.create(::PrimeListBenchmark) { calcDirect() },
        "PrimeList.calcEratosthenes"                        to BenchmarkEntry.create(::PrimeListBenchmark) { calcEratosthenes() },

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
        print("${it.key}: ")
        it.value.run()
    }
}