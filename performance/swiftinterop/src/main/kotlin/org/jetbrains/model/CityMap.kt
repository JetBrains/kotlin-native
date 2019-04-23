/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
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
                    moneyCost > other.moneyCost -> 1
                    timeCost < other.timeCost -> -1
                    timeCost > other.timeCost -> 1
                    interests.size < other.interests.size -> -1
                    interests.size > other.interests.size -> 1
                    transport.size < other.transport.size -> -1
                    else -> 1
                }
            } else {
                error("Expected type is RouteCost")
            }
}

internal var placeCounter = 0u

data class Place(val geoCoordinateX: Double, val geoCoordinateY: Double, val name: String, val interestCategory: Interest) {
    val id: UInt
    init {
        id = placeCounter
        placeCounter++
    }

    val fullDescription: String
        get() = "Place $name($geoCoordinateX;$geoCoordinateY)"

    override fun hashCode(): Int {
        return id.toInt()
    }

    fun compareTo(other: Place): Int {
        return when {
            this == other -> 0
            geoCoordinateX < other.geoCoordinateX -> -1
            geoCoordinateX > other.geoCoordinateX -> 1
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