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

class SimpleCost: Cost {
    var value: Int

    init(cost: Int) {
        value = cost
    }

    func plus(other: Cost) -> Cost {
        let otherInstance = other as! SimpleCost
        return SimpleCost(cost: value + otherInstance.value)
    }

    func minus(other: Cost) -> Cost {
        let otherInstance = other as! SimpleCost
        return SimpleCost(cost: value - otherInstance.value)
    }

    func compareTo(other: Cost) -> Int32 {
        let otherInstance = other as! SimpleCost
        if (value < otherInstance.value) {
            return -1
        }
        if (value == otherInstance.value) {
            return 0
        }
        return 1
    }
}

class SwiftInteropBenchmarks {

    let BENCHMARK_SIZE = 100000
    var multigraph = Multigraph()
    var cityMap = CityMap()

    func randomInt(border: Int) -> Int {
        return Int.random(in: 1 ..< border)
    }

    func randomDouble(border: Double) -> Double {
        return Double.random(in: 0 ..< border)
    }

    func randomString(length: Int) -> String {
        let letters = "abcdefghijklmnopqrst"
        return String((0..<length).map{ _ in letters.randomElement()! })
    }

    func randomTransport() -> Transport {
        let allValues = [Transport.car, Transport.underground, Transport.bus, Transport.trolleybus, Transport.tram, Transport.taxi, Transport.foot]
        return allValues.randomElement()!
    }

    func randomInterest() -> Interest {
        let allValues = [Interest.sight, Interest.culture, Interest.park, Interest.entertainment]
        return allValues.randomElement()!
    }

    func randomPlace() -> Place {
        return Place(geoCoordinateX: randomDouble(border: 180), geoCoordinateY: randomDouble(border: 90), name: randomString(length: 5), interestCategory: randomInterest())
    }

    func randomRouteCost() -> RouteCost {
        let transportCount = randomInt(border: 7)
        let interestCount = randomInt(border: 4)
        var transports = Set<Transport>()
        var interests = Set<Interest>()

        for _ in 0...transportCount {
            transports.insert(randomTransport())
        }

        for _ in 0...interestCount {
            interests.insert(randomInterest())
        }

        return RouteCost(moneyCost: randomDouble(border: 10000), timeCost: randomDouble(border: 24), interests: interests, transport: transports)
    }

    func createMultigraphOfInt() {
        for _ in 0...BENCHMARK_SIZE {
            multigraph.addEdge(from: randomInt(border: BENCHMARK_SIZE), to: randomInt(border: BENCHMARK_SIZE), cost: SimpleCost(cost: randomInt(border: BENCHMARK_SIZE)))
        }
    }

    func fillCityMap() {
        for _ in 0...BENCHMARK_SIZE {
            cityMap.addRoute(from: randomPlace(), to: randomPlace(), cost: randomRouteCost())
        }
    }

    func searchRoutesInSwiftMultigraph() {
        for _ in 0...BENCHMARK_SIZE {
            var result = multigraph.searchRoutesWithLimits(start: randomInt(border: BENCHMARK_SIZE), finish: randomInt(border: BENCHMARK_SIZE), limits: SimpleCost(cost: randomInt(border: BENCHMARK_SIZE)))
            var count = result.map{ $0.count }.reduce(0, +)
        }
    }

    func searchTravelRoutes() {
        for _ in 0...BENCHMARK_SIZE {
            var result = cityMap.getRoutes(start: randomPlace(), finish: randomPlace(), limits: randomRouteCost())
            var totalCost = result.flatMap{ $0 }.map { $0.cost.moneyCost }.reduce(0, +)
        }

    }

    func availableTransportOnMap() {
        for _ in 0...BENCHMARK_SIZE {
            Set(cityMap.allRoutes.map { (cityMap.getRouteById(id: $0.id).cost as! RouteCost).transport })
        }
    }

    func allPlacesMapedByInterests() {
        for _ in 0...BENCHMARK_SIZE {
            let placesByInterests = cityMap.allPlaces.reduce([Interest: Array<Place>]()) { (dict, place) -> [Interest: Array<Place>] in
                var dict = dict
                if (dict[place.interestCategory] != nil) {
                    dict[place.interestCategory]?.append(place)
                } else {
                    dict[place.interestCategory] = [place]
                }
                return dict
            }
        }
    }

    func getAllPlacesWithStraightRoutesTo() {
        for _ in 0...BENCHMARK_SIZE {
            let start = cityMap.allPlaces.randomElement()!
            var availableRoutes = cityMap.getAllStraightRoutesFrom(place: start)
            var availablePlaces = availableRoutes.map { $0.to }
        }
    }

    func goToAllAvailablePlaces() {
        for _ in 0...BENCHMARK_SIZE {
            let start = cityMap.allPlaces.randomElement()!
            var availableRoutes = cityMap.getAllStraightRoutesFrom(place: start)
            availableRoutes.map { 2 * $0.cost.moneyCost }
        }
    }

    func removeVertexAndEdgesSwiftMultigraph() {
        var multigraphCopy = multigraph.copy() as! Multigraph
        var edges = multigraphCopy.allEdges
        while (!edges.isEmpty) {
            multigraphCopy.removeEdge(id: UInt32(edges.randomElement()!))
            let vertexes = multigraphCopy.allVertexes
            multigraphCopy.removeVertex(vertex: vertexes.randomElement()!)
            edges = multigraphCopy.allEdges
        }
    }

    func removePlacesAndRoutes() {
        var cityMapCopy = cityMap.copy() as! CityMap
        var routes = cityMapCopy.allRoutes
        while (!cityMapCopy.isEmpty()) {
            cityMapCopy.removeRouteById(id: routes.first!.id)
            routes = cityMapCopy.allRoutes
        }
    }
}