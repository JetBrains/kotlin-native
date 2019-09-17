/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.influxdb

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

// DISTINCT InfluxDB function.
class DistinctFunction(field: ColumnEntity.FieldEntity<*>) : InfluxFunction<String>(field) {
    override val lineProtocol = "DISTINCT(\"${entity.name}\")"
}