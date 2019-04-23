/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import Foundation
import CityMap

var runner = BenchmarksRunner()
let args = KotlinArray(size: Int32(CommandLine.arguments.count), init: {index in
    CommandLine.arguments[Int(truncating: index)]
})
runner.runBenchmarks(args: args, run: { (parser: ArgParser) -> [BenchmarkResult] in
    var swiftBenchmarks = SwiftInteropBenchmarks()
    var swiftLauncher = SwiftLauncher(numWarmIterations: parser.get(name: "warmup") as! Int32,
        numberOfAttempts: parser.get(name: "repeat") as! Int32, prefix: parser.get(name: "prefix") as! String)
    swiftLauncher.benchmarks["createMultigraphOfInt"] =  swiftBenchmarks.createMultigraphOfInt
    swiftLauncher.benchmarks["fillCityMap"] =  swiftBenchmarks.fillCityMap
    swiftLauncher.benchmarks["searchRoutesInSwiftMultigraph"] =  swiftBenchmarks.searchRoutesInSwiftMultigraph
    swiftLauncher.benchmarks["searchTravelRoutes"] =  swiftBenchmarks.searchTravelRoutes
    swiftLauncher.benchmarks["availableTransportOnMap"] =  swiftBenchmarks.availableTransportOnMap
    swiftLauncher.benchmarks["allPlacesMapedByInterests"] =  swiftBenchmarks.allPlacesMapedByInterests
    swiftLauncher.benchmarks["getAllPlacesWithStraightRoutesTo"] =  swiftBenchmarks.getAllPlacesWithStraightRoutesTo
    swiftLauncher.benchmarks["goToAllAvailablePlaces"] =  swiftBenchmarks.goToAllAvailablePlaces
    swiftLauncher.benchmarks["removeVertexAndEdgesSwiftMultigraph"] =  swiftBenchmarks.removeVertexAndEdgesSwiftMultigraph
    swiftLauncher.benchmarks["removePlacesAndRoutes"] = swiftBenchmarks.removePlacesAndRoutes
    return swiftLauncher.launch(benchmarksToRun: parser.getAll(name: "filter"))
}, parseArgs: runner.parse, collect: { (benchmarks: [BenchmarkResult], parser: ArgParser) -> KotlinUnit in
    runner.collect(results: benchmarks, parser: parser)
    return KotlinUnit()
})