/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer
import org.jetbrains.report.BenchmarkResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// Entity to describe avarage values which conssists of mean and variance values.
data class MeanVariance(val mean: Double, val variance: Double) {
    override fun toString(): String {
        val format = { number: Double -> number.format(2)}
        return "${format(mean)} ± ${format(variance)}"
    }
}

open class MeanVarianceBenchmark(name: String, status: BenchmarkResult.Status, score: Double, metric: BenchmarkResult.Metric,
                            runtimeInUs: Double, repeat: Int, warmup: Int, val variance: Double) :
        BenchmarkResult(name, status, score, metric, runtimeInUs, repeat, warmup) {

    constructor(name: String, score: Double, variance: Double) : this(name, BenchmarkResult.Status.PASSED, score,
            BenchmarkResult.Metric.EXECUTION_TIME, 0.0, 0, 0, variance)

    // Calculate difference in percentage compare to another.
    fun calcPercentageDiff(other: MeanVarianceBenchmark): MeanVariance {
        assert(other.score >= 0 &&
                other.variance >= 0 &&
                other.score - other.variance != 0.0,
                { "Mean and variance should be positive and not equal!" })
        val exactMean = (score - other.score) / other.score
        // Analyze intervals. Calculate difference between border points.
        val (bigValue, smallValue) = if (score > other.score) Pair(this, other) else Pair(other, this)
        val bigValueIntervalStart = bigValue.score - bigValue.variance
        val bigValueIntervalEnd = bigValue.score + bigValue.variance
        val smallValueIntervalStart = smallValue.score - smallValue.variance
        val smallValueIntervalEnd = smallValue.score + smallValue.variance
        if (smallValueIntervalEnd > bigValueIntervalStart) {
            // Interval intersect.
            return MeanVariance(0.0, 0.0)
        }
        val mean = ((smallValueIntervalEnd - bigValueIntervalStart) / bigValueIntervalStart) *
                (if (score > other.score) -1 else 1)

        val maxValueChange = ((bigValueIntervalEnd - smallValueIntervalEnd) / bigValueIntervalEnd)
        val minValueChange =  ((bigValueIntervalStart - smallValueIntervalStart) / bigValueIntervalStart)
        val variance = abs(abs(mean) - max(minValueChange, maxValueChange))
        return MeanVariance(mean * 100, variance * 100)
    }

    // Calculate ratio value compare to another.
    fun calcRatio(other: MeanVarianceBenchmark): MeanVariance {
        assert(other.score >= 0 &&
                other.variance >= 0 &&
                other.score - other.variance != 0.0,
                { "Mean and variance should be positive and not equal!" })
        val mean = score / other.score
        val minRatio = (score - variance) / (other.score + other.variance)
        val maxRatio = (score + variance) / (other.score - other.variance)
        val ratioConfInt = min(abs(minRatio - mean), abs(maxRatio - mean))
        return MeanVariance(mean, ratioConfInt)
    }

    override fun toString(): String =
            "${score.format()} ± ${variance.format()}"

    override fun serializeFields(): String {
        return """
            ${super.serializeFields()},
            "variance": ${variance.toString()}
            """
    }
}

fun geometricMean(values: Collection<Double>, totalNumber: Int = values.size) =
    with(values.asSequence().filter { it != 0.0 }) {
        if (count() == 0) {
            0.0
        } else {
            map { it.pow(1.0 / totalNumber) }.reduce { a, b -> a * b }
        }
    }

fun computeMeanVariance(samples: List<Double>): MeanVariance {
    val zStar = 1.67    // Critical point for 90% confidence of normal distribution.
    val mean = samples.sum() / samples.size
    val variance = samples.indices.sumByDouble { (samples[it] - mean) * (samples[it] - mean) } / samples.size
    val confidenceInterval = sqrt(variance / samples.size) * zStar
    return MeanVariance(mean, confidenceInterval)
}

// Calculate avarage results for bencmarks (each becnhmark can be run several times).
fun collectMeanResults(benchmarks: Map<String, List<BenchmarkResult>>): BenchmarksTable {
    return benchmarks.map {(name, resultsSet) ->
        val repeatedSequence = IntArray(resultsSet.size)
        var metric = BenchmarkResult.Metric.EXECUTION_TIME
        var currentStatus = BenchmarkResult.Status.PASSED
        var currentWarmup = -1

        // Collect common becnhmark values and check them.
        resultsSet.forEachIndexed { index, result ->
            // If there was at least one failure, summary is marked as failure.
            if (result.status == BenchmarkResult.Status.FAILED) {
                currentStatus = result.status
            }
            repeatedSequence[index] = result.repeat
            if (currentWarmup != -1)
                if (result.warmup != currentWarmup)
                    println("Check data consistency. Warmup value for benchmark '${result.name}' differs.")
            currentWarmup = result.warmup
            metric = result.metric
        }

        repeatedSequence.sort()
        // Check if there are missed loop during running benchmarks.
        repeatedSequence.forEachIndexed { index, element ->
            if (index != 0)
                if ((element - repeatedSequence[index - 1]) != 1)
                    println("Check data consistency. For benchmark '$name' there is no run" +
                            " between ${repeatedSequence[index - 1]} and $element.")
        }

        // Create mean and variance benchmarks result.
        val scoreMeanVariance = computeMeanVariance(resultsSet.map { it.score })
        val runtimeInUsMeanVariance = computeMeanVariance(resultsSet.map { it.runtimeInUs })
        val meanBenchmark = MeanVarianceBenchmark(name, currentStatus, scoreMeanVariance.mean, metric,
                runtimeInUsMeanVariance.mean, repeatedSequence[resultsSet.size - 1],
                currentWarmup, scoreMeanVariance.variance)
        name to meanBenchmark
    }.toMap()
}

fun collectBenchmarksDurations(benchmarks: Map<String, List<BenchmarkResult>>): Map<String, Double> =
        benchmarks.map { (name, resultsSet) ->
            name to resultsSet.sumByDouble { it.runtimeInUs }
        }.toMap()