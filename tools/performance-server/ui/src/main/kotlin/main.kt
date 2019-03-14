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

import kotlin.browser.*
import org.w3c.xhr.*
import org.jetbrains.report.json.*
import org.jetbrains.build.Build
import kotlin.js.*
import org.w3c.dom.*

// API for interop with JS library Chartist.
external class ChartistPlugins {
    fun legend(data: dynamic): dynamic
}

external object Chartist {
    val plugins: ChartistPlugins
    fun Line(query: String, data: dynamic, options: dynamic): dynamic
}

fun sendGetRequest(url: String) : String {
    val request = XMLHttpRequest()

    request.open("GET", url, false)
    request.send()
    if (request.status == 200.toShort()) {
        return request.responseText
    }
    error("Request to $url has status ${request.status}")
}

// Parse description with values for metrics.
fun <T : Any> separateValues(values: String, valuesContainer: MutableMap<String, MutableList<T?>>, convert: (String) -> T = { it as T }) {
    val existingSamples = mutableListOf<String>()
    val splittedValues = values.split(";")
    val insertedList = valuesContainer.values.firstOrNull()?.let { MutableList<T?>(it.size) { null } } ?: mutableListOf<T?>()
    splittedValues.forEach { it ->
        val valueParts = it.split("-", limit = 2)
        if (valueParts.size != 2) {
            error("Wrong format of value $it.")
        }
        val (sampleName, value) = valueParts
        existingSamples.add(sampleName)
        val currentList = mutableListOf<T?>()
        currentList.addAll(insertedList)
        valuesContainer.getOrPut(sampleName) { currentList }.add(convert(value))
    }
    // Check if there are other keys that are absent in current record.
    val missedSamples = valuesContainer.keys - existingSamples
    missedSamples.forEach {
        valuesContainer[it]!!.add(null)
    }
}

fun getChartData(labels: List<String>, valuesList: Collection<List<*>>): dynamic {
    val chartData: dynamic = object{}
    chartData["labels"] = labels.toTypedArray()
    chartData["series"] = valuesList.map { it.toTypedArray() }.toTypedArray()
    return chartData
}

fun getChartOptions(samples: Array<String>): dynamic {
    val chartOptions: dynamic = object{}
    chartOptions["fullWidth"] = true
    val paddingObject: dynamic = object{}
    paddingObject["right"] = 40
    chartOptions["chartPadding"] = paddingObject
    val axisXObject: dynamic = object{}
    axisXObject["offset"] = 40
    chartOptions["axisX"] = axisXObject
    val legendObject: dynamic = object{}
    legendObject["legendNames"] = samples
    chartOptions["plugins"] = arrayOf(Chartist.plugins.legend(legendObject))
    return chartOptions
}

fun main(args: Array<String>) {
    val serverUrl = "http://localhost:3000/builds"

    // Get parameters from request.
    val url = window.location.href
    val parametersPart = url.substringAfter("?").split('&')
    val parameters = mutableMapOf("target" to "Linux", "type" to "dev", "build" to "")
    parametersPart.forEach {
        val parsedParameter = it.split("=", limit = 2)
        if (parsedParameter.size == 2) {
            val (key, value) = parsedParameter
            if (parameters.containsKey(key)) {
                parameters[key] = value
            }
        }
    }

    // Get builds.
    val buildsUrl = buildString {
        append("$serverUrl")
        append("/${parameters["target"]}")
        append("/${parameters["type"]}")
        append("/${parameters["build"]}")
    }
    val response = sendGetRequest(buildsUrl)

    val data = JsonTreeParser.parse(response)
    if (data !is JsonArray) {
        error("Response is expected to be an array.")
    }
    val builds = data.jsonArray.map { Build.create(it as JsonObject) }

    // Fill autocomplete list.
    val buildsNumbers = builds.map { json("value" to it.buildNumber, "data" to it.buildNumber) }.toTypedArray()
    js("$( \"#highligted_build\" ).autocomplete({ lookup: buildsNumbers });")

    // Change inputs values connected with parameters and add events listeners.
    document.querySelector("#inputGroupTarget [value=\"${parameters["target"]}\"]")?.setAttribute("selected", "true")
    document.querySelector("#inputGroupBuildType [value=\"${parameters["type"]}\"]")?.setAttribute("selected", "true")
    (document.getElementById("highligted_build") as HTMLInputElement).value = parameters["build"]!!
    // TODO - add onChange events for fields.

    // Collect information for charts library.
    val labels = mutableListOf<String>()
    val executionTime = mutableMapOf<String, MutableList<Double?>>()
    val compileTime = mutableMapOf<String, MutableList<Double?>>()
    val codeSize = mutableMapOf<String, MutableList<Int?>>()
    val bundleSize = mutableListOf<Int?>()

    builds.forEach {
        labels.add(it.buildNumber)
        separateValues(it.executionTime, executionTime) { value -> value.toDouble() }
        separateValues(it.compileTime, compileTime) { value -> value.toDouble() }
        separateValues(it.codeSize, codeSize) { value -> value.toInt() }
        bundleSize.add(it.bundleSize?.toInt())
    }

    // Draw charts.
    Chartist.Line("#exec_chart", getChartData(labels, executionTime.values),
            getChartOptions(executionTime.keys.toTypedArray()))
    Chartist.Line("#compile_chart", getChartData(labels, compileTime.values),
            getChartOptions(compileTime.keys.toTypedArray()))
    Chartist.Line("#codesize_chart", getChartData(labels, codeSize.values),
            getChartOptions(codeSize.keys.toTypedArray()))
    Chartist.Line("#bundlesize_chart", getChartData(labels, listOf(bundleSize)),
            getChartOptions(arrayOf("Bundle size")))
    // TODO - tooltips.
    // TODO - highligted build.
    // TODO - builds with failures.
}