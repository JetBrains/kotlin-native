/*
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
    swiftLauncher.benchmarks["removePlacesAndRoutes"] =  swiftBenchmarks.removePlacesAndRoutes
    return swiftLauncher.launch(benchmarksToRun: parser.getAll(name: "filter"))
}, parseArgs: runner.parse, collect: { (benchmarks: [BenchmarkResult], parser: ArgParser) -> KotlinUnit in
    runner.collect(results: benchmarks, parser: parser)
    return KotlinUnit()
})