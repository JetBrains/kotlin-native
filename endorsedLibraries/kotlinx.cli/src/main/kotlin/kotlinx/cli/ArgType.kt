/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cli

/**
 * Possible types of arguments.
 *
 * Inheritors describe type of argument value. New types can be added by user.
 * In case of options type can have parameter or not.
 */
abstract class ArgType<T : Any>(val hasParameter: kotlin.Boolean) {
    /**
     * Text description of type for helpMessage.
     */
    abstract val description: kotlin.String

    /**
     * Function to convert string argument value to its type.
     * In case of error during conversion also provides help message.
     *
     * @param value value
     */
    abstract fun convert(value: kotlin.String, name: kotlin.String): T

    /**
     * Argument type for flags that can be only set/unset.
     */
    object Boolean : ArgType<kotlin.Boolean>(false) {
        override val description: kotlin.String
            get() = ""

        override fun convert(value: kotlin.String, name: kotlin.String): kotlin.Boolean =
            value != "false"
    }

    /**
     * Argument type for string values.
     */
    object String : ArgType<kotlin.String>(true) {
        override val description: kotlin.String
            get() = "{ String }"

        override fun convert(value: kotlin.String, name: kotlin.String): kotlin.String = value
    }

    /**
     * Argument type for integer values.
     */
    object Int : ArgType<kotlin.Int>(true) {
        override val description: kotlin.String
            get() = "{ Int }"

        override fun convert(value: kotlin.String, name: kotlin.String): kotlin.Int =
            value.toIntOrNull()
                    ?: throw ParsingException("Option $name is expected to be integer number. $value is provided.")
    }

    /**
     * Argument type for double values.
     */
    object Double : ArgType<kotlin.Double>(true) {
        override val description: kotlin.String
            get() = "{ Double }"

        override fun convert(value: kotlin.String, name: kotlin.String): kotlin.Double =
            value.toDoubleOrNull()
                    ?: throw ParsingException("Option $name is expected to be double number. $value is provided.")
    }

    /**
     * Type for arguments that have limited set of possible values.
     */
    class Choice(val values: List<kotlin.String>) : ArgType<kotlin.String>(true) {
        override val description: kotlin.String
            get() = "{ Value should be one of $values }"

        override fun convert(value: kotlin.String, name: kotlin.String): kotlin.String =
            if (value in values) value
            else throw ParsingException("Option $name is expected to be one of $values. $value is provided.")
    }
}

internal class ParsingException(message: String) : Exception(message)