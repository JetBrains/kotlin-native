/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kliopt

// Possible types of arguments.
// TODO Make extension points for new types? Not sealed?
sealed class ArgType<T : Any>(val hasParameter: kotlin.Boolean) {
    // Type description for help messages.
    abstract val description: kotlin.String
    // Function to convert string argument value to its type.
    // In case of error during convertion also provide help message.
    abstract val convertion: (value: kotlin.String, name: kotlin.String, helpMessage: kotlin.String)->T

    // Flags that can be only set/unset.
    class Boolean : ArgType<kotlin.Boolean>(false) {
        override val description: kotlin.String
            get() = ""

        override val convertion: (value: kotlin.String, name: kotlin.String, _: kotlin.String) -> kotlin.Boolean
            get() = { value, _ , _ -> if (value == "false") false else true }
    }

    class String : ArgType<kotlin.String>(true) {
        override val description: kotlin.String
            get() = "{ String }"

        override val convertion: (value: kotlin.String, name: kotlin.String, _: kotlin.String) -> kotlin.String
            get() = { value, _, _ -> value }
    }

    class Int : ArgType<kotlin.Int>(true) {
        override val description: kotlin.String
            get() = "{ Int }"

        override val convertion: (value: kotlin.String, name: kotlin.String, helpMessage: kotlin.String) -> kotlin.Int
            get() = { value, name, helpMessage -> value.toIntOrNull()
                    ?: error("Option $name is expected to be integer number. $value is provided.\n$helpMessage") }
    }

    class Double : ArgType<kotlin.Double>(true) {
        override val description: kotlin.String
            get() = "{ Double }"

        override val convertion: (value: kotlin.String, name: kotlin.String,
                                  helpMessage: kotlin.String) -> kotlin.Double
            get() = { value, name, helpMessage -> value.toDoubleOrNull()
                    ?: error("Option $name is expected to be double number. $value is provided.\n$helpMessage") }
    }

    class Choice(val values: List<kotlin.String>) : ArgType<kotlin.String>(true) {
        override val description: kotlin.String
            get() = "{ Value should be one of $values }"

        override val convertion: (value: kotlin.String, name: kotlin.String,
                                  helpMessage: kotlin.String) -> kotlin.String
            get() = { value, name, helpMessage -> if (value in values) value
            else error("Option $name is expected to be one of $values. $value is provided.\n$helpMessage") }
    }
}