/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.multigraph

interface Cost {
    operator fun plus(other: Cost): Cost
    operator fun minus(other: Cost): Cost
    override operator fun equals(other: Any?): Boolean
    operator fun compareTo(other: Cost): Int
}

data class Edge<T>(val id: UInt, val from: T, val to: T, val cost: Cost) {
    override operator fun equals(other: Any?): Boolean {
        return if (other is Edge<*>) (from == other.from && to == other.to && cost == other.cost) else false
    }
}

class EdgeAbsenceMultigraphException(message: String): Exception(message) {}
class VertexAbsenceMultigraphException(message: String): Exception(message) {}

class Multigraph<T>() {
    private var edges = mutableMapOf<T, MutableList<Edge<T>>>()
    private var idCounter = 0u

    val allVertexes: Set<T>
        get() {
            val outerVertexes = edges.keys
            val innerVertexes = edges.map { (key, values) ->
                values.map { it.to }.filter { it in outerVertexes }
            }.flatten()
            return outerVertexes.union(innerVertexes)
        }

    val allEdges: List<UInt>
        get() = edges.values.flatten().map { it.id }

    fun getEdgeById(id: UInt): Edge<T> {
        edges.forEach { (_, value) ->
            value.forEach {
                if (it.id == id)
                    return it
            }
        }
        throw EdgeAbsenceMultigraphException("Edge with id $id wasn't found.")
    }

    fun copy(other: Multigraph<T>): Multigraph<T> {
        val newInstance = Multigraph<T>()
        edges.forEach { (vertex, edges) ->
            edges.forEach { edge ->
                newInstance.addEdge(vertex, edge.to, edge.cost)
            }
        }
        return newInstance
    }

    fun addEdge(from: T, to: T, cost: Cost): UInt {
        val edge = Edge(idCounter, from, to, cost)
        edges.getOrPut(from) { mutableListOf() }.add(edge)
        idCounter++
        return edge.id
    }

    fun removeEdge(id: UInt) {
        try {
            val edge = getEdgeById(id)
            edges[edge.from]?.remove(edge)
        } catch (exception: EdgeAbsenceMultigraphException) {
            println("WARNING: no edge with id $id was found.")
        }
    }

    fun removeVertex(vertex: T) {
        edges.remove(vertex)
        edges.map { (key, values) ->
            Pair(key, values.filter {
                it.to == vertex
            })
        }.toMap()
    }

    fun checkVertexExistance(vertex: T): Boolean {
        return vertex in allVertexes
    }

    fun getTo(edgeId: UInt) =
            getEdgeById(edgeId).to

    fun getFrom(edgeId: UInt) =
            getEdgeById(edgeId).from

    fun getCost(edgeId: UInt) =
            getEdgeById(edgeId).cost

    fun getEdgesFrom(vertex: T) =
            edges[vertex] ?: listOf<Edge<T>>()

    fun isEmpty() = edges.isEmpty()

    fun searchRoutesWithLimits(start: T, finish: T, limits: Cost): List<List<UInt>> {
        data class WaveStep(val costs: MutableList<Cost> = mutableListOf(),
                            val routes: MutableList<MutableList<UInt>> = mutableListOf(),
                            val vertexes: MutableList<MutableList<T>> = mutableListOf())

        val currentStepsState = mutableMapOf<T, WaveStep>()
        var oldFront = mutableSetOf<T>(start)
        val newFront = mutableSetOf<T>()

        if (!checkVertexExistance(start)) {
            throw(VertexAbsenceMultigraphException("Start vertex wasn't found in graph."))
        }
        if (!checkVertexExistance(finish)) {
            throw(VertexAbsenceMultigraphException("Finish vertex wasn't found in graph."))
        }

        edges.keys.forEach {
            currentStepsState[it] = WaveStep()
        }

        while (!oldFront.isEmpty()) {
            oldFront.forEach {
                val currentStepState = currentStepsState[it]!!
                val newRoutes = mutableListOf<UInt>()
                // Lookup all edges from vertex.
                val values = edges.get(it)!!
                values.forEach { edge ->
                    val toStep = currentStepsState[edge.to]!!

                    // Create new pathes and count their costs.
                    if (currentStepState.routes.isEmpty()) {
                        val newVertexes = mutableListOf(edge.from, edge.to)
                        val newCost = edge.cost
                        if (newCost <= limits && edge.from != edge.to) {
                            newRoutes.add(edge.id)
                            toStep.routes.add(newRoutes)
                            toStep.costs.add(edge.cost)
                            toStep.vertexes.add(newVertexes)
                            currentStepsState[edge.to] = toStep
                            // Add new wave front.
                            if (edge.to != finish && edge.to !in oldFront) {
                                newFront.add(edge.to)
                            }
                        }
                    } else {
                        currentStepState.routes.forEachIndexed { index, it ->
                            val newRoutes = listOf(it)
                            val oldCost = currentStepState.costs.get(index)
                            val newCost = edge.cost + oldCost
                            val newVertexes = listOf(currentStepState.vertexes.get(index))
                            if (newCost <= limits && edge.to != currentStepState.vertexes.get(index)) {
                                newFront.add(edge.to)
                            }
                        }
                    }
                }
            }
            oldFront = newFront.toMutableSet()
            newFront.clear()
        }
        return currentStepsState[finish]!!.routes
    }
}