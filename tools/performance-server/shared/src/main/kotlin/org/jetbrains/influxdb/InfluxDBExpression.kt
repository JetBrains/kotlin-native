/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.influxdb

// Base class for expression of InfluxDB query.
abstract class Expression<T: Any>() {
    abstract val lineProtocol: String

    infix fun where(condition: Condition<*>): Expression<T> = object : Expression<T>() {
        override val lineProtocol: String =
                "${this@Expression.lineProtocol} ${WhereExpression<T>(condition).lineProtocol}"

    }

    infix fun from(expression: Expression<String>): Expression<T> = object : Expression<T>() {
        override val lineProtocol: String =
                "FROM ${expression.lineProtocol}"
    }
}

abstract class Condition<T: Any>: Expression<T>() {
    // AND InfluxDb operator to combine several conditions.
    infix fun and(other: Condition<T>): Condition<T> = object : Condition<T>() {
        override val lineProtocol: String =
                "${this@Condition.lineProtocol} AND ${other.lineProtocol}"
    }
}

// Base class for InfluxDB expression describing condition of selected points.
class WhereExpression<T: Any>(val condition: Condition<*>): Expression<T>() {
    override val lineProtocol: String = "WHERE ${condition.lineProtocol}"
}

// Base class for InfluxDB functions.
abstract class InfluxFunction<T : Any>(val entity: ColumnEntity<*>): Expression<T>()

// DISTINCT InfluxDB function.
class DistinctFunction(field: ColumnEntity.FieldEntity<*>, from: String? = null) : InfluxFunction<String>(field) {
    override val lineProtocol = "DISTINCT(\"${entity.name}\")${from?.let {" FROM $from"}}"
}