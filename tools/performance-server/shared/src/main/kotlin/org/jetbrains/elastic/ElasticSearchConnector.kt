/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.elastic

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import org.jetbrains.report.json.*

// Connector with InfluxDB.
class ElasticSearchConnector(private val host: String, private val port: Int = 9200,
                        private val user: String? = null, private val password: String? = null) {

    val url = "$host:$port"
    // Execute ElasticSearch request.
    fun request(method: RequestMethod, url: String, acceptJsonContentType: Boolean = true, body: String? = null): Promise<String> {
        return sendRequest(method, url, user, password, acceptJsonContentType, body)
    }

    // Execute select query. Can be used to get full measurement or separate field.
    /*inline fun <reified T: Any>selectQuery(query: String, measurement: Measurement<*>? = null): Promise<List<T>> {
        return query(query).then { response ->
            // Parse response.
            if (measurement is T) {
                // Request objects.
                measurement.fromInfluxJson(JsonTreeParser.parse(response)) as? List<T>
                        ?: error("Expected response describe object of class ${T::class.simpleName}")
            } else if (T::class == String::class){
                // Request separate fields.
                fieldsFromInfluxJson(JsonTreeParser.parse(response)) as? List<T>
                        ?: error("Expected response includes string fields.")
            } else {
                error("Wrong type")
            }
        }
    }

    // Execute select of [columns] with condition [where].
    fun select(columns: Expression<String>):
            Promise<List<String>> {
        val query = "SELECT ${columns.lineProtocol}"
        return selectQuery<String>(query)
    }

    // Parse separate field value from InfluxDb json response.
    fun fieldsFromInfluxJson(data: JsonElement): List<String> {
        val points = mutableListOf<String>()
        if (data is JsonObject) {
            val results = data.getRequiredField("results") as? JsonArray
                    ?: error("Wrong format of response. Field 'results' should be an array.")
            results.map {
                if (it is JsonObject) {
                    val series = it.getRequiredField("series") as? JsonArray
                            ?: error("Wrong format of response. Field 'series' should be an array.")
                    series.map {
                        if (it is JsonObject) {
                            val values = it.getRequiredField("values") as? JsonArray
                                    ?: error("Wrong format of response. Field 'values' should be an array.")
                            values.forEach {
                                points.add(((it as JsonArray)[1] as JsonLiteral).unquoted())
                            }
                        } else {
                            error("Wrong format of response. Each seria should be an object.")
                        }
                    }
                }
            }
        }
        return points
    }

    // Insert several measurements.
    fun <T : Measurement<T>>insert(points: Collection<Measurement<T>>): Array<Promise<String>> {
        checkConnection()
        // InfluxDb has limitations to 5,000 points in one request.
        val insertLimit = 5000
        return points.chunked(insertLimit).map {
            val description = it.joinToString(separator = "\n") { it.lineProtocol }
            val writeUrl = "$host:$port/write?db=$databaseName"
            sendRequest(RequestMethod.POST, writeUrl, user, password, body = description)
        }.toTypedArray()
    }*/
}