/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import benchmark

var runner = BenchmarksRunner()
let args = KotlinArray(size: Int32(CommandLine.arguments.count - 1), init: {index in
    CommandLine.arguments[Int(truncating: index) + 1]
})

let companion = BenchmarkEntryWithInit.Companion()

var swiftLauncher = SwiftLauncher()

swiftLauncher.add(name: "AbstractMethod.sortStrings", benchmark: companion.create(ctor: { return AbstractMethodBenchmark() },
        lambda: { ($0 as! AbstractMethodBenchmark).sortStrings() }))
swiftLauncher.add(name: "AbstractMethod.sortStringsWithComparator", benchmark: companion.create(ctor: { return AbstractMethodBenchmark() },
        lambda: { ($0 as! AbstractMethodBenchmark).sortStringsWithComparator() }))

swiftLauncher.add(name: "ClassArray.copy", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).copy() }))
swiftLauncher.add(name: "ClassArray.copyManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).copyManual() }))
swiftLauncher.add(name: "ClassArray.filterAndCount", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).filterAndCount() }))
swiftLauncher.add(name: "ClassArray.filterAndMap", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).filterAndMap() }))
swiftLauncher.add(name: "ClassArray.filterAndMapManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).filterAndMapManual() }))
swiftLauncher.add(name: "ClassArray.filter", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).filter() }))
swiftLauncher.add(name: "ClassArray.filterManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).filterManual() }))
swiftLauncher.add(name: "ClassArray.countFilteredManual", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).countFilteredManual() }))
swiftLauncher.add(name: "ClassArray.countFiltered", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).countFiltered() }))
swiftLauncher.add(name: "ClassArray.countFilteredLocal", benchmark: companion.create(ctor: { return ClassArrayBenchmark() },
        lambda: { ($0 as! ClassArrayBenchmark).countFilteredLocal() }))

swiftLauncher.add(name: "ClassBaseline.consume", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).consume() }))
swiftLauncher.add(name: "ClassBaseline.consumeField", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).consumeField() }))
swiftLauncher.add(name: "ClassBaseline.allocateList", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).allocateList() }))
swiftLauncher.add(name: "ClassBaseline.allocateArray", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).allocateArray() }))
swiftLauncher.add(name: "ClassBaseline.allocateListAndFill", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).allocateListAndFill() }))
swiftLauncher.add(name: "ClassBaseline.allocateListAndWrite", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).allocateListAndWrite() }))
swiftLauncher.add(name: "ClassBaseline.allocateArrayAndFill", benchmark: companion.create(ctor: { return ClassBaselineBenchmark() },
        lambda: { ($0 as! ClassBaselineBenchmark).allocateArrayAndFill() }))

swiftLauncher.add(name: "ClassStream.copy", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).copy() }))
swiftLauncher.add(name: "ClassStream.copyManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).copyManual() }))
swiftLauncher.add(name: "ClassStream.filterAndCount", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).filterAndCount() }))
swiftLauncher.add(name: "ClassStream.filterAndMap", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).filterAndMap() }))
swiftLauncher.add(name: "ClassStream.filterAndMapManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).filterAndMapManual() }))
swiftLauncher.add(name: "ClassStream.filter", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).filter() }))
swiftLauncher.add(name: "ClassStream.filterManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).filterManual() }))
swiftLauncher.add(name: "ClassStream.countFilteredManual", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).countFilteredManual() }))
swiftLauncher.add(name: "ClassStream.reduce", benchmark: companion.create(ctor: { return ClassStreamBenchmark() },
        lambda: { ($0 as! ClassStreamBenchmark).reduce() }))

swiftLauncher.add(name: "CompanionObject.invokeRegularFunction", benchmark: companion.create(ctor: { return CompanionObjectBenchmark() },
        lambda: { ($0 as! CompanionObjectBenchmark).invokeRegularFunction() }))

swiftLauncher.add(name: "DefaultArgument.testOneOfTwo", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testOneOfTwo() }))
swiftLauncher.add(name: "DefaultArgument.testTwoOfTwo", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testTwoOfTwo() }))
swiftLauncher.add(name: "DefaultArgument.testOneOfFour", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testOneOfFour() }))
swiftLauncher.add(name: "DefaultArgument.testFourOfFour", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testFourOfFour() }))
swiftLauncher.add(name: "DefaultArgument.testOneOfEight", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testOneOfEight() }))
swiftLauncher.add(name: "DefaultArgument.testEightOfEight", benchmark: companion.create(ctor: { return DefaultArgumentBenchmark() },
        lambda: { ($0 as! DefaultArgumentBenchmark).testEightOfEight() }))

swiftLauncher.add(name: "Elvis.testElvis", benchmark: companion.create(ctor: { return ElvisBenchmark() },
        lambda: { ($0 as! ElvisBenchmark).testElvis() }))

swiftLauncher.add(name: "Euler.problem1bySequence", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem1bySequence() }))
swiftLauncher.add(name: "Euler.problem1", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem1() }))
swiftLauncher.add(name: "Euler.problem2", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem2() }))
swiftLauncher.add(name: "Euler.problem4", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem4() }))
swiftLauncher.add(name: "Euler.problem8", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem8() }))
swiftLauncher.add(name: "Euler.problem9", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem9() }))
swiftLauncher.add(name: "Euler.problem14", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem14() }))
swiftLauncher.add(name: "Euler.problem14full", benchmark: companion.create(ctor: { return EulerBenchmark() },
        lambda: { ($0 as! EulerBenchmark).problem14full() }))

swiftLauncher.add(name: "Fibonacci.calcClassic", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { ($0 as! FibonacciBenchmark).calcClassic() }))
swiftLauncher.add(name: "Fibonacci.calc", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { ($0 as! FibonacciBenchmark).calc() }))
swiftLauncher.add(name: "Fibonacci.calcWithProgression", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { ($0 as! FibonacciBenchmark).calcWithProgression() }))
swiftLauncher.add(name: "Fibonacci.calcSquare", benchmark: companion.create(ctor: { return FibonacciBenchmark() },
        lambda: { ($0 as! FibonacciBenchmark).calcSquare() }))

swiftLauncher.add(name: "Calls.finalMethod", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).finalMethodCall() }))
swiftLauncher.add(name: "Calls.openMethodMonomorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).classOpenMethodCall_MonomorphicCallsite() }))
swiftLauncher.add(name: "Calls.openMethodBimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).classOpenMethodCall_BimorphicCallsite() }))
swiftLauncher.add(name: "Calls.openMethodTrimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).classOpenMethodCall_TrimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodMonomorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
            lambda: { ($0 as! CallsBenchmarks).interfaceMethodCall_MonomorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodBimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).interfaceMethodCall_BimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodTrimorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).interfaceMethodCall_TrimorphicCallsite() }))
swiftLauncher.add(name: "Calls.interfaceMethodHexamorphic", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).interfaceMethodCall_HexamorphicCallsite() }))
swiftLauncher.add(name: "Calls.returnBoxUnboxFolding", benchmark: companion.create(ctor: { return CallsBenchmarks() },
        lambda: { ($0 as! CallsBenchmarks).returnBoxUnboxFolding() }))

runner.runBenchmarks(args: args, run: { (arguments: BenchmarkArguments) -> [BenchmarkResult] in

    if arguments is BaseBenchmarkArguments {
        let argumentsList: BaseBenchmarkArguments = arguments as! BaseBenchmarkArguments
        return swiftLauncher.launch(numWarmIterations: argumentsList.warmup,
            numberOfAttempts: argumentsList.repeat,
            prefix: argumentsList.prefix, filters: argumentsList.filter,
            filterRegexes: argumentsList.filterRegex,
            verbose: argumentsList.verbose)
    }
    return [BenchmarkResult]()
}, parseArgs: { (args: KotlinArray,  benchmarksListAction: (() -> KotlinUnit)) -> BenchmarkArguments? in
    return runner.parse(args: args, benchmarksListAction: swiftLauncher.benchmarksListAction) },
  collect: { (benchmarks: [BenchmarkResult], arguments: BenchmarkArguments) -> Void in
    runner.collect(results: benchmarks, arguments: arguments)
}, benchmarksListAction: swiftLauncher.benchmarksListAction)