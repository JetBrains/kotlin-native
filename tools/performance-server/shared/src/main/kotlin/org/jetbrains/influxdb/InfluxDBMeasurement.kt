/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.influxdb

import kotlin.reflect.KProperty
import kotlin.js.Promise            // TODO - migrate to multiplatform.
import org.jetbrains.report.json.*

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
abstract class Measurement<T : Measurement<T>>(val name: String, protected val connector: InfluxDBConnector) {
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

    val lineProtocol: String
        get() =
            with(StringBuilder("$name,")) {
                var prefix = ""
                tags.values.forEach {
                    it.value?.let { _ ->
                        append("${prefix}${it.lineProtocol}")
                        prefix = ","
                    } ?: println("Tag ${it.name} isn't initialized.")
                }
                prefix = " "
                fields.values.forEach {
                    it.value?.let { _ ->
                        append("${prefix}${it.lineProtocol}")
                        prefix = ","
                    } ?: println("Field ${it.name} isn't initialized.")
                }
                timestamp?.let {
                    append(" $timestamp")
                }
                toString()
            }

    // Execute select from measurement with [where] condition.
    inline fun <reified U: Any>select(columns: Expression<U>): Promise<List<U>> {
        val query = "SELECT ${columns.lineProtocol}"
        return connector.selectQuery<U>(query, this)
    }

    // Get expression describing all fields/tags in measurement.
    fun all() = object : Expression<T>() {
        override val lineProtocol: String = "* FROM $name"
    }

    // Parse InfluxDb json format to instances.
    abstract fun fromInfluxJson(data: JsonElement): List<T>
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
    infix fun <U : Any> eq(value: U) = object : Condition<U>() {
        override val lineProtocol: String = "\"$name\"='$value'"
    }

    // Get expression checking matching entity value to some regex.
    infix fun match(regex: String) = object : Condition<T>() {
        override val lineProtocol: String = "\"$name\"=~/$regex/"
    }

    // Select entity with some [where] condition.
    infix fun select(where: Condition<String>) = object : Expression<String>() {
        override val lineProtocol: String = "SELECT \"$name\" FROM $measurement WHERE ${where.lineProtocol}"
    }

    // InfluxDB field.
    class FieldEntity<T : FieldType<*>>(name: String, measurement: String) : ColumnEntity<T>(name, measurement) {
         override val lineProtocol
             get() =
                 value?.let {
                     when(it) {
                         is FieldType.InfluxInt -> "$name=${it.value}i"
                         is FieldType.InfluxFloat -> "$name=${it.value}"
                         is FieldType.InfluxBoolean -> "$name=${it.value}"
                         else -> "$name=\"$it\""
                     }
                 } ?: ""

        fun distinct() = DistinctFunction(name, measurement)
    }

    // InfluxDb tag.
    class TagEntity<T : Any>(name: String, measurement: String) : ColumnEntity<T>(name, measurement) {
        private fun escape() = "$value".replace(" |,|=".toRegex()) { match -> "\\${match.value}" }

        override val lineProtocol
            get() = "$name=${escape()}"
    }
}