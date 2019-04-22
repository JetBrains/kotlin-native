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

package org.jetbrains.model

import org.jetbrains.multigraph.*

enum class Transport { CAR, UNDERGROUND, BUS, TROLLEYBUS, TRAM, TAXI, FOOT }
enum class Interest { SIGHT, CULTURE, PARK, ENTERTAINMENT }

class PlaceAbsenceException(message: String): Exception(message) {}

data class RouteCost(val moneyCost: Double, val timeCost: Double, val interests: Set<Interest>, val transport: Set<Transport>): Cost {
    override operator fun plus(other: Cost) =
            if (other is RouteCost) {
                RouteCost(moneyCost + other.moneyCost, timeCost + other.timeCost,
                        interests.union(other.interests), transport.union(other.transport))
            } else {
                error("Expected type is RouteCost")
            }

    override operator fun minus(other: Cost) =
            if (other is RouteCost) {
                RouteCost(if (moneyCost > other.moneyCost) moneyCost - other.moneyCost else 0.0,
                        if (timeCost > other.timeCost) timeCost - other.timeCost else 0.0,
                        interests.subtract(other.interests), transport.subtract(other.transport))
            } else {
                error("Expected type is RouteCost")
            }

    override operator fun compareTo(other: Cost) =
            if (other is RouteCost) {
                when {
                    this == other -> 0
                    moneyCost < other.moneyCost -> -1
                    timeCost < other.timeCost -> -1
                    interests.size < other.interests.size -> -1
                    transport.size < other.transport.size -> -1
                    else -> 1
                }
            } else {
                error("Expected type is RouteCost")
            }
}

class Place(val geoCoordinateX: Double, val geoCoordinateY: Double, val name: String, val interestCategory: Interest) {
    companion object {
        var counter = 0u
    }
    val id: UInt
    init {
        id = counter
        counter++
    }

    override operator fun equals(other: Any?): Boolean {
        return if (other is Place) geoCoordinateX == other.geoCoordinateX && geoCoordinateY == other.geoCoordinateY &&
                name == other.name && interestCategory == other.interestCategory
        else false
    }

    fun compareTo(other: Place): Int {
        return when {
            this == other -> 0
            geoCoordinateX < other.geoCoordinateX -> -1
            geoCoordinateY < other.geoCoordinateY -> -1
            else -> 1
        }
    }
}

data class Path(val from: Place, val to: Place, val cost: RouteCost)

object CityMap {
    data class RouteId(val id: UInt, val from: UInt, val to: UInt)
    private val graph = Multigraph<Place>()

    val allPlaces: Set<Place>
        get() = graph.allVertexes
    val allRoutes: List<RouteId>
        get() {
            val edges = graph.allEdges
            val result = mutableListOf<RouteId>()
            edges.forEach {
                result.add(RouteId(it, graph.getFrom(it).id, graph.getTo(it).id))
            }
            return result
        }

    fun addRoute(from: Place, to: Place, cost: RouteCost): UInt {
        return graph.addEdge(from, to, cost)
    }

    fun getPlaceById(id: UInt): Place {
        graph.allVertexes.forEach {
            if (it.id == id) {
                return it
            }
        }
        throw PlaceAbsenceException("Place with id $id wasn't found.")
    }

    fun removePlaceById(id: UInt) {
        graph.allVertexes.forEach { it
            if (it.id == id) {
                graph.removeVertex(it)
                return
            }
        }
    }

    fun removeRouteById(id: UInt) {
        graph.removeEdge(id)
    }

    fun getRoutes(start: Place, finish: Place, limits: RouteCost): List<List<Path>> {
        val result = graph.searchRoutesWithLimits(start, finish, limits)
        return result.map {
            it.map {
                Path(graph.getFrom(it), graph.getTo(it), graph.getCost(it) as RouteCost)
            }
        }
    }

    fun getRouteById(id: UInt) =
        graph.getEdgeById(id)

    fun getAllStraightRoutesFrom(place: Place) =
            graph.getEdgesFrom(place).map { Path(graph.getFrom(it.id), graph.getTo(it.id), graph.getCost(it.id) as RouteCost) }

    fun isEmpty() = graph.isEmpty()
}