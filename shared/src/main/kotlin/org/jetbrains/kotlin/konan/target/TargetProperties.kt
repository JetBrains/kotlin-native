/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.properties

import org.jetbrains.kotlin.konan.target.*

fun Properties.hostString(name: String, host: KonanTarget): String?
    = this.resolvablePropertyString(name, host.name)

fun Properties.hostList(name: String, host: KonanTarget): List<String>
    = this.resolvablePropertyList(name, host.name)

fun Properties.targetString(name: String, target: KonanTarget): String?
    = this.resolvablePropertyString(name, target.name)

fun Properties.targetList(name: String, target: KonanTarget): List<String>
    = this.resolvablePropertyList(name, target.name)

fun Properties.hostTargetString(name: String, target: KonanTarget, host: KonanTarget): String?
    = this.resolvablePropertyString(name, hostTargetSuffix(host, target))

fun Properties.hostTargetList(name: String, target: KonanTarget, host: KonanTarget): List<String>
    = this.resolvablePropertyList(name, hostTargetSuffix(host, target))

/**
 * Wraps [propertyList] with resolving mechanism. See [String.resolveValue].
 */
private fun Properties.resolvablePropertyList(
        key: String, suffix: String? = null, escapeInQuotes: Boolean = false,
        visitedProperties: MutableSet<String> = mutableSetOf()
): List<String> =
    propertyList(key, suffix, escapeInQuotes).flatMap { it.resolveValue(this, visitedProperties) }

/**
 * Wraps [propertyString] with resolving mechanism. See [String.resolveValue].
 */
private fun Properties.resolvablePropertyString(
        key: String, suffix: String? = null,
        visitedProperties: MutableSet<String> = mutableSetOf()
): String? =
    propertyString(key, suffix)?.resolveValue(this, visitedProperties)?.let {
        it.singleOrNull() ?: error("$key's value should be a single string. Got ${it.joinToString()} instead.")
    }

/**
 * Adds trivial symbol resolving mechanism to properties files.
 *
 * Given the following properties file:
 *
 *  key0 = value1 value2
 *  key1 = value3 $key0
 *  key2 = $key1
 *
 * "$key1".resolveValue(properties) will return List("value3", "value1", "value2")
 */
private fun String.resolveValue(properties: Properties, visitedProperties: MutableSet<String> = mutableSetOf()): List<String> =
        when {
            startsWith("$") -> {
                val property = this.substringAfter('$')
                // Keep track of visited properties to avoid running in circles.
                if (!visitedProperties.add(property)) {
                    error("Circular dependency: ${visitedProperties.joinToString()}")
                }
                visitedProperties += property
                properties.resolvablePropertyList(property, visitedProperties = visitedProperties)
            }
            else -> listOf(this)
        }

