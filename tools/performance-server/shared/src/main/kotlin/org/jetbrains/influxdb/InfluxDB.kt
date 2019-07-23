/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.influxdb

import kotlin.reflect.KProperty
import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.js.json               // TODO - migrate to multiplatform.
import org.jetbrains.report.json.*

// Connector with InfluxDB.
object InfluxDBConnector {
    private lateinit var host: String
    private lateinit var databaseName: String
    private var port: Int = 8086
    private var user: String? = null
    private var password: String? = null

    // Initialize connection.
    fun initConnection(host: String, databaseName: String, port: Int = 8086, user: String? = null,
                       password: String? = null) {
        this.host = host
        this.databaseName = databaseName
        this.port = port
        this.user = user
        this.password = password
    }

    // Execute InfluxDb query.
    fun query(query: String): Promise<String> {
        checkConnection()

        val queryUrl = "$host:$port/query?db=$databaseName&q=$query"
        return sendRequest(RequestMethod.GET, queryUrl, user, password, true)
    }

    // Check that connection with database can be set.
    private inline fun checkConnection() {
        if (!::host.isInitialized || !::databaseName.isInitialized) {
            error("Please, firstly set connection to Influx database to have opportunity to send requests.")
        }
    }

    // Insert measurement.
    fun insert(point: Measurement): Promise<String> {
        checkConnection()
        val description = point.lineProtocol
        val writeUrl = "$host:$port/write?db=$databaseName"
        return sendRequest(RequestMethod.POST, writeUrl, user, password, body = description)
    }

    // Execute select query. Can be used to get full measurement or separate field.
    inline fun <reified T: Any>selectQuery(query: String, measurement: Measurement? = null): Promise<List<T>> {
        return query(query).then { response ->
            // Parse response.
            if (measurement is T) {
                // Request objects.
                measurement.fromInfluxJson(JsonTreeParser.parse(response)) as List<T>
            } else if (T::class == String::class){
                // Request separate fields.
                fieldsFromInfluxJson(JsonTreeParser.parse(response)) as List<T>
            } else {
                error("Wrong type")
            }
        }
    }

    // Execute select of [columns] with condition [where].
    inline fun select(columns: Expression<String>, from: Expression<String>, where: WhereExpression? = null):
            Promise<List<String>> {
        val query = "SELECT ${columns.lineProtocol} FROM (${from.lineProtocol}) ${where?.lineProtocol ?: ""}"
        return selectQuery<String>(query)
    }

    // Parse separate field value from InfluxDb json response.
    fun fieldsFromInfluxJson(data: JsonElement): List<String> {
        val points = mutableListOf<String>()
        var columnsIndexes: Map<String, Int>
        if (data is JsonObject) {
            val results = data.getRequiredField("results") as JsonArray
            results.map {
                if (it is JsonObject) {
                    val series = it.getRequiredField("series") as JsonArray
                    series.map {
                        if (it is JsonObject) {
                            val values = it.getRequiredField("values") as JsonArray
                            values.forEach {
                                points.add(((it as JsonArray)[1] as JsonLiteral).unquoted())
                            }
                        }
                    }
                }
            }
        }
        return points
    }

    // Insert several measurements.
    fun insert(points: Collection<Measurement>): Array<Promise<String>> {
        checkConnection()
        // InfluxDb has limitations to 5,000 points in one request.
        val insertLimit = 5000
        return points.chunked(insertLimit).map {
            val description = with(StringBuilder()) {
                var prefix = ""
                it.forEach {
                    append("${prefix}${it.lineProtocol}")
                    prefix = "\n"
                }
                toString()
            }
            val writeUrl = "$host:$port/write?db=$databaseName"
            sendRequest(RequestMethod.POST, writeUrl, user, password, body = description)
        }.toTypedArray()
    }
}

// Hack for Kotlin/JS.
// Need separate classes to describe types, because Int and Double are same in Kotlin/JS.
sealed class FieldType<T : Any>(val value: T) {
    override fun toString(): String = value.toString()
    class InfluxInt(value: Int): FieldType<Int>(value)
    class InfluxFloat(value: Double): FieldType<Double>(value)
    class InfluxString(value: Any): FieldType<String>(value.toString())
    class InfluxBoolean(value: Boolean): FieldType<Boolean>(value)
}

// Base class for measurements.
abstract class Measurement(val name: String) {
    var timestamp: Long? = null
        protected set
    val fields = mutableMapOf<String, ColumnEntity.FieldEntity<FieldType<*>>>()
    val tags = mutableMapOf<String, ColumnEntity.TagEntity<*>>()

    // Get field by name.
    fun field(fieldName: String) = fields[fieldName] ?: error ("No field $fieldName in measurement $name")
    // Get tag by name.
    fun tag(tagName: String) = tags[tagName] ?: error ("No tag $tagName in measurement $name")

    // InfluxDB field.
    inner class Field<T: FieldType<*>>(val fieldName: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ColumnEntity.FieldEntity<T> {
            val field = ColumnEntity.FieldEntity<T>(fieldName ?: prop.name, name)
            if (field.name in fields.keys) {
                error("Field ${field.name} already exists in measurement $name")
            }
            fields[field.name] = field as ColumnEntity.FieldEntity<FieldType<*>>
            return field
        }
    }

    // InfluxDB tag.
    inner class Tag<T: Any>(val tagName: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ColumnEntity.TagEntity<T> {
            val tag = ColumnEntity.TagEntity<T>(tagName ?: prop.name, name)
            if (tag.name in tags.keys) {
                error("Field ${tag.name} already exists in measurement $name")
            }
            tags[tag.name] = tag
            return tag
        }
    }

    val lineProtocol =
        with(StringBuilder("$name,")) {
            var prefix = ""
            tags.values.forEach {
                it.value?.let {value ->
                    append("${prefix}${it.lineProtocol}")
                    prefix = ","
                } ?: println("Tag ${it.name} isn't initialized.")
            }
            prefix = " "
            fields.values.forEach {
                it.value?.let { value ->
                    append("${prefix}${it.lineProtocol}")
                    prefix = ","
                } ?: println("Field ${it.name} isn't initialized.")
            }
            timestamp?.let {
                append(" $timestamp")
            }
            toString()
        }

    // Execute DISTINCT InfluxDb function by [fieldName].
    fun distinct(fieldName: String): DistinctFunction {
        if (fieldName !in fields.keys) {
            error("There is no field with $fieldName in measurement $name.")
        }
        return fields[fieldName]!!.let { DistinctFunction(it) }
    }

    // Execute select from measurement with [where] condition.
    inline fun <reified T: Any>select(columns: Expression<T>, where: WhereExpression? = null): Promise<List<T>> {
        val query = "SELECT ${columns.lineProtocol} FROM \"$name\" ${where?.lineProtocol ?: ""}"
        return InfluxDBConnector.selectQuery<T>(query, this)
    }

    // Get expression describing all fields/tags in measurement.
    fun all() = object : Expression<Measurement>() {
        override val lineProtocol: String = "*"
    }

    // Parse InfluxDb json format to instances.
    abstract fun fromInfluxJson(data: JsonElement): List<Measurement>
}

// Base class for expression of InfluxDB query.
abstract class Expression<T: Any>() {
    abstract val lineProtocol: String
}

// Base class for InfluxDB expression describing condition of selected points.
abstract class WhereExpression: Expression<String>() {
    // AND InfluxDb operator to combine sevral conditions.
    infix fun and(other: WhereExpression) = object : WhereExpression() {
        override val lineProtocol: String =
                "WHERE ${this@WhereExpression.lineProtocol.replace("WHERE", "")} AND ${other.lineProtocol.replace("WHERE", "")}"
    }
}

// Base class for InfluxDB functions.
abstract class InfluxFunction<T : Any>(val entity: ColumnEntity<*>): Expression<T>()

// DISTINC InfluxDB function.
class DistinctFunction(field: ColumnEntity.FieldEntity<*>) : InfluxFunction<String>(field) {
    override val lineProtocol = "DISTINCT(\"${entity.name}\")"
}

// Entity describing column in InfluxDB. There are two kinds only - tags and fields.
sealed class ColumnEntity<T : Any>(val name: String, val measurement: String): Expression<T>() {
    var value: T? = null
        protected set

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = value
    operator fun setValue(thisRef: Any?, property: KProperty<*>, definedValue: T?) {
        value = definedValue
    }

    // Get expression checking equality of entity to value.
    infix fun eq(value: T) = object : WhereExpression() {
        override val lineProtocol: String = "WHERE \"$name\"='$value'"
    }

    // Get expression checking matching entity value to some regex.
    infix fun match(regex: String) = object : WhereExpression() {
        override val lineProtocol: String = "WHERE \"$name\"=~/$regex/"
    }

    // Select entity with some [where] condition.
    infix fun select(where: WhereExpression) = object : Expression<String>() {
        override val lineProtocol: String = "SELECT \"$name\" FROM $measurement ${where.lineProtocol}"
    }

    // InfluxDB field.
    class FieldEntity<T : FieldType<*>>(name: String, measurement: String) : ColumnEntity<T>(name, measurement) {
         override val lineProtocol =
                 value?.let {
                     when(it) {
                         is FieldType.InfluxInt -> "$name=${it.value}i"
                         is FieldType.InfluxFloat -> "$name=${it.value}"
                         is FieldType.InfluxBoolean -> "$name=${it.value}"
                         else -> "$name=\"$it\""
                     }
                 } ?: ""
    }

    // InfluxDb tag.
    class TagEntity<T : Any>(name: String, measurement: String) : ColumnEntity<T>(name, measurement) {
        private fun escape() = "$value".replace(" |,|=".toRegex()) { match -> "\\${match.value}" }

        override val lineProtocol = "$name=${escape()}"
    }
}

// Now implemenation for network connection only for Node.js. TODO - multiplatform.
external fun require(module: String): dynamic

fun getAuth(user: String, password: String): String {
    val buffer = js("Buffer").from(user + ":" + password)
    val based64String = buffer.toString("base64")
    return "Basic " + based64String
}

enum class RequestMethod {
    POST, GET, PUT
}

fun sendRequest(method: RequestMethod, url: String, user: String? = null, password: String? = null,
                acceptJsonContentType: Boolean = false, body: String? = null): Promise<String> {
    val request = require("node-fetch")
    val headers = mutableListOf<Pair<String, String>>()
    if (user != null && password != null) {
        headers.add("Authorization" to getAuth(user, password))
    }
    if (acceptJsonContentType) {
        headers.add("Accept" to "application/json")
    }
    return request(url,
            json(
                    "method" to method.toString(),
                    "headers" to json(*(headers.toTypedArray())),
                    "body" to body
            )
    ).then { response ->
        if (!response.ok)
            error("Error during getting response from $url\n" +
                    "${response}")
        else
            response.text()
    }
}