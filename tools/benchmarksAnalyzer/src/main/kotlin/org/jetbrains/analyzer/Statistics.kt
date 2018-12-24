/*
 * Copyright 2010-2018 JetBrains s.r.o.
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


package org.jetbrains.benchmarksAnalyzer
import org.jetbrains.report.BenchmarkResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

// Entity to describe avarage values which conssists of mean and variance values.
data class MeanVariance(val mean: Double, val variance: Double) {
    override fun toString(): String {
        val format = { number: Double -> format(number, 2)}
        return "${format(mean)}+-${format(variance)}"
    }
}

// Composite benchmark which descibe avarage result for several runs and contains mean and variance value
data class MeanVarianceBench(val meanBenchmark: BenchmarkResult, val varianceBenchmark: BenchmarkResult) {

    // Calculate difference in percentage compare to another.
    fun calcPercentageDiff(other: MeanVarianceBench): MeanVariance {
        val mean = (meanBenchmark.score - other.meanBenchmark.score) / other.meanBenchmark.score
        val maxValueChange = abs(meanBenchmark.score + varianceBenchmark.score -
                        other.meanBenchmark.score + other.varianceBenchmark.score) /
                        abs(other.meanBenchmark.score + other.varianceBenchmark.score)

        val minValueChange = abs(meanBenchmark.score - varianceBenchmark.score -
                        other.meanBenchmark.score - other.varianceBenchmark.score) /
                        abs(other.meanBenchmark.score - other.varianceBenchmark.score)

        val variance = abs(abs(mean) - max(minValueChange, maxValueChange))
        return MeanVariance(mean * 100, variance * 100)
    }

    // Calculate ratio value compare to another.
    fun calcRatio(other: MeanVarianceBench): MeanVariance {
        val mean = meanBenchmark.score / other.meanBenchmark.score
        val minRatio = (meanBenchmark.score - varianceBenchmark.score) / (other.meanBenchmark.score + other.varianceBenchmark.score)
        val maxRatio = (meanBenchmark.score + varianceBenchmark.score) / (other.meanBenchmark.score - other.varianceBenchmark.score)
        val ratioConfInt = min(abs(minRatio - mean), abs(maxRatio - mean))
        return MeanVariance(mean, ratioConfInt)
    }

    override fun toString(): String {
        val format = { number: Double -> format(number, 4)}
        return "${format(meanBenchmark.score)}+-${format(varianceBenchmark.score)}"
    }
}

// Class with different statistical operations
class Statistics {
    companion object {
        private val zStar = 1.96

        fun geometricMean(values: List<Double>): Double {
            val iPow = 1.0 / values.size
            return values.map { it.pow(iPow) }.reduce { a, b -> a * b }
        }

        fun getMeanVariance(samples: List<Double>): MeanVariance {
            val mean = samples.sum() / samples.size
            val variance = samples.indices.sumByDouble { (samples[it] - mean) * (samples[it] - mean) } / samples.size
            val confidenceInterval = sqrt(variance / samples.size) * zStar
            return MeanVariance(mean, confidenceInterval)
        }
    }
}