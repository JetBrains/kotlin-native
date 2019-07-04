/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kliopt

import kotlin.reflect.KProperty

// Queue of arguments descriptors.
class ArgumentsQueue(argumentsDescriptors: List<ArgParser.ArgDescriptor<*>>) {
    // Map of arguments descriptors and their current usage number.
    private val argumentsUsageNumber = linkedMapOf(*argumentsDescriptors.map { it to 0 }.toTypedArray())

    fun pop(): String? {
        if (argumentsUsageNumber.isEmpty())
            return null

        val (currentDescriptor, usageNumber) = argumentsUsageNumber.iterator().next()
        currentDescriptor.number?.let {
            // Parse all arguments for current argument description.
            if (usageNumber + 1 >= currentDescriptor.number) {
                // All needed arguments were provided.
                argumentsUsageNumber.remove(currentDescriptor)
            } else {
                argumentsUsageNumber[currentDescriptor] = usageNumber + 1
            }
        }
        return currentDescriptor.fullName
    }
}

// Abstract base class for subcommands.
abstract class Subcommand(val name: String): ArgParser(name) {
    // Execute action if subcommand was provided.
    abstract fun execute()
}

// Common descriptor both for options and positional arguments.
abstract class Descriptor<T : Any>(val type: ArgType<T>,
                          val fullName: String,
                          val description: String? = null,
                          val defaultValue: List<T> = emptyList(),
                          val isRequired: Boolean = false,
                          val deprecatedWarning: String? = null) {
    abstract val textDescription: String
    abstract val helpMessage: String
}

// Arguments parser.
open class ArgParser(val programName: String, var useDefaultHelpShortName: Boolean = true,
                var prefixStyle: OPTION_PREFIX_STYLE = OPTION_PREFIX_STYLE.LINUX, var skipExtraArguments: Boolean = false) {

    // Map of options: key - fullname of option, value - pair of descriptor and parsed values.
    protected val options = mutableMapOf<String, ParsingValue<*, *>>()
    // Map of arguments: key - fullname of argument, value - pair of descriptor and parsed values.
    protected val arguments = mutableMapOf<String, ParsingValue<*, *>>()
    // Map of subcommands.
    protected val subcommands = mutableMapOf<String, Subcommand>()

    // Mapping for short options names for quick search.
    private lateinit var shortNames: Map<String, ParsingValue<*, *>>

    // Use Linux-style of options description.
    protected val optionFullFormPrefix = if (prefixStyle == OPTION_PREFIX_STYLE.LINUX) "--" else "-"
    protected val optionShortFromPrefix = "-"

    // Origin of option/argument value.
    enum class ValueOrigin { SET_BY_USER, SET_DEFAULT_VALUE, UNSET, REDEFINED }
    // Options prefix style.
    enum class OPTION_PREFIX_STYLE { LINUX, JVM }

    operator fun get(key: String): ArgumentValue<*> =
        options[key]?.argumentValue ?: arguments[key]?.argumentValue ?:
        printError("There is no option or argument with name $key")

    // Option descriptor.
    inner class OptionDescriptor<T : Any>(
            type: ArgType<T>,
            fullName: String,
            val shortName: String ? = null,
            description: String? = null,
            defaultValue: List<T> = emptyList(),
            isRequired: Boolean = false,
            val isMultiple: Boolean = false,
            val delimiter: String? = null,
            deprecatedWarning: String? = null) : Descriptor<T> (type, fullName, description, defaultValue,
            isRequired, deprecatedWarning) {

        override val textDescription: String
            get() = "option $optionFullFormPrefix$fullName"

        override val helpMessage: String
            get() {
                val result = StringBuilder()
                result.append("    $optionFullFormPrefix$fullName")
                shortName?.let { result.append(", $optionShortFromPrefix$it") }
                (defaultValue.joinToString(",") { it.toString() }).also { if (!it.isEmpty()) result.append(" [$it]") }
                description?.let {result.append(" -> ${it}")}
                if (isRequired) result.append(" (always required)")
                result.append(" ${type.description}")
                deprecatedWarning?.let { result.append(" Warning: $it") }
                result.append("\n")
                return result.toString()
            }
    }

    // Argument descriptor.
    inner class ArgDescriptor<T : Any>(
            type: ArgType<T>,
            fullName: String,
            val number: Int? = null,
            description: String? = null,
            defaultValue: List<T> = emptyList(),
            isRequired: Boolean = true,
            deprecatedWarning: String? = null) : Descriptor<T> (type, fullName, description, defaultValue,
            isRequired, deprecatedWarning) {

        init {
            // Check arguments number correctness.
            number?.let {
                if (it < 0)
                    printError("Number of arguments for argument description $fullName should be greater than zero.")
            }
        }

        override val textDescription: String
            get() = "argument $fullName"

        override val helpMessage: String
            get() {
                val result = StringBuilder()
                result.append("    ${fullName}")
                (defaultValue.joinToString(",") { it.toString() }).also { if (!it.isEmpty()) result.append(" [$it]") }
                description?.let { result.append(" -> ${it}") }
                if (!isRequired) result.append(" (optional)")
                result.append(" ${type.description}")
                deprecatedWarning?.let { result.append(" Warning: $it") }
                result.append("\n")
                return result.toString()
            }
    }

    inner class SingleNullableOptionLoader<T : Any>(val type: ArgType<T>,
                    val fullName: String? = null,
                    val shortName: String ? = null,
                    val description: String? = null,
                    val isRequired: Boolean = false,
                    val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<T?> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, emptyList(),
                    isRequired, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(type.convertion)
            options[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    inner class SingleOptionWithDefaultLoader<T : Any>(val type: ArgType<T>,
                                                       val fullName: String? = null,
                                                       val shortName: String ? = null,
                                                       val description: String? = null,
                                                       val defaultValue: T,
                                                       val isRequired: Boolean = false,
                                                       val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<T> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, listOf(defaultValue),
                    isRequired, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(type.convertion)
            options[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    inner class MultipleOptionsLoader<T : Any>(val type: ArgType<T>,
                                               val fullName: String? = null,
                                               val shortName: String ? = null,
                                               val description: String? = null,
                                               val defaultValue: List<T> = emptyList(),
                                               val isRequired: Boolean = false,
                                               val isMultiple: Boolean = false,
                                               val delimiter: String? = null,
                                               val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<MutableList<T>> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, defaultValue,
                    isRequired, isMultiple, delimiter, deprecatedWarning)
            if (!isMultiple && delimiter == null)
                printError("Several values are expected for option $name. " +
                        "Option must be used multiple times or split with delimiter.")
            val cliElement = ArgumentMultipleValues(type.convertion)
            options[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    // Add option and get delegator to its value.
    fun <T : Any>option(type: ArgType<T>,
               fullName: String? = null,
               shortName: String ? = null,
               description: String? = null,
               isRequired: Boolean = false,
               deprecatedWarning: String? = null) = SingleNullableOptionLoader(type, fullName, shortName,
            description, isRequired, deprecatedWarning)

    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        defaultValue: T,
                        isRequired: Boolean = false,
                        deprecatedWarning: String? = null) = SingleOptionWithDefaultLoader(type, fullName, shortName,
            description, defaultValue, isRequired, deprecatedWarning)

    // Add options with multiple values and get delegator to its value.
    fun <T : Any>options(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        defaultValue: List<T> = emptyList(),
                        isRequired: Boolean = false,
                        isMultiple: Boolean = false,
                        delimiter: String? = null,
                        deprecatedWarning: String? = null) = MultipleOptionsLoader(type, fullName, shortName,
            description, defaultValue, isRequired, isMultiple, delimiter, deprecatedWarning)

    inner class SingleNullableArgumentLoader<T : Any>(val type: ArgType<T>,
                                                      val fullName: String? = null,
                                                      val description: String? = null,
                                                      val isRequired: Boolean = true,
                                                      val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<T?> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, 1, description,
                    emptyList(), isRequired, deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(type.convertion)
            arguments[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    inner class SingleArgumentWithDefaultLoader<T : Any>(val type: ArgType<T>,
                                                         val fullName: String? = null,
                                                         val description: String? = null,
                                                         val defaultValue: T,
                                                         val isRequired: Boolean = true,
                                                         val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<T> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, 1, description,
                    listOf(defaultValue), isRequired, deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(type.convertion)
            arguments[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    inner class MultipleArgumentsLoader<T : Any>(val type: ArgType<T>,
                                               val fullName: String? = null,
                                               val number: Int? = null,
                                               val description: String? = null,
                                               val defaultValue: List<T> = emptyList(),
                                               val isRequired: Boolean = true,
                                               val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueInterface<MutableList<T>> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, number, description,
                    defaultValue, isRequired, deprecatedWarning)
            val cliElement = ArgumentMultipleValues(type.convertion)
            arguments[name] = ParsingValue(descriptor, cliElement)
            return cliElement
        }
    }

    // Add argument and get delegator to its value.
    fun <T : Any>argument(type: ArgType<T>,
                 fullName: String? = null,
                 description: String? = null,
                 isRequired: Boolean = true,
                 deprecatedWarning: String? = null) = SingleNullableArgumentLoader(type, fullName, description,
            isRequired, deprecatedWarning)

    fun <T : Any>argument(type: ArgType<T>,
                          fullName: String? = null,
                          description: String? = null,
                          defaultValue: T,
                          isRequired: Boolean = true,
                          deprecatedWarning: String? = null) = SingleArgumentWithDefaultLoader(type, fullName,
            description, defaultValue, isRequired, deprecatedWarning )

    // Add argument with several and get delegator to its value.
    fun <T : Any>arguments(type: ArgType<T>,
                           fullName: String? = null,
                           number: Int? = null,
                           description: String? = null,
                           defaultValue: List<T> = emptyList(),
                           isRequired: Boolean = true,
                           deprecatedWarning: String? = null) = MultipleArgumentsLoader(type, fullName, number,
            description, defaultValue, isRequired, deprecatedWarning)

    // Add subcommands.
    fun subcommands(vararg subcommandsList: Subcommand) {
        subcommandsList.forEach {
            if (it.name in subcommands) {
                printError("Subcommand with name ${it.name} was already defined.")
            }

            // Set same settings as main parser.
            it.prefixStyle = prefixStyle
            it.useDefaultHelpShortName = useDefaultHelpShortName
            subcommands[it.name] = it
        }
    }

    // Get all free arguments as unnamed list.
    fun <T : Any>arguments(type: ArgType<T>,
                           description: String? = null,
                           defaultValue: List<T> = emptyList(),
                           isRequired: Boolean = true,
                           deprecatedWarning: String? = null): ArgumentValueInterface<MutableList<T>> {
        val descriptor = ArgDescriptor(type, "", null, description,
                defaultValue, isRequired, deprecatedWarning)
        val cliElement = ArgumentMultipleValues(type.convertion)
        if ("" in arguments) {
            printError("You can have only one unnamed list with positional arguments.")
        }
        arguments[""] = ParsingValue(descriptor, cliElement)
        return cliElement
    }

    // Parsing value of option/argument.
    inner class ParsingValue<T: Any, U: Any>(val descriptor: Descriptor<T>, val argumentValue: ArgumentValue<U>) {

        // Add parsed value from command line.
        fun addValue(stringValue: String,
                     setValue: ArgumentValue<U>.(String, String) -> Unit = ArgumentValue<U>::addValue) {
            // Check of possibility to set several values to one option/argument.
            if (descriptor is OptionDescriptor<*> && !descriptor.isMultiple &&
                    !argumentValue.isEmpty() && descriptor.delimiter == null) {
                printError("Try to provide more than one value for ${descriptor.fullName}.")
            }
            // Show deprecated warning only first time of using option/argument.
            descriptor.deprecatedWarning?.let {
                if (argumentValue.isEmpty())
                    println ("Warning: $it")
            }
            // Split value if needed.
            if (descriptor is OptionDescriptor<*> && descriptor.delimiter != null) {
                stringValue.split(descriptor.delimiter).forEach {
                    argumentValue.setValue(it, descriptor.fullName)
                }
            } else {
                argumentValue.setValue(stringValue, descriptor.fullName)
            }
        }

        // Set default value to option.
        fun addDefaultValue() {
            if (!descriptor.defaultValue.isEmpty()) {
                addValue(descriptor.defaultValue.toString(), ArgumentValue<U>::addDefaultValue)
            } else {
                if (descriptor.isRequired) {
                    printError("Please, provide value for ${descriptor.textDescription}. It should be always set.")
                }
            }
        }
    }

    interface ArgumentValueInterface<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    }

    // Argument/option value.
    abstract class ArgumentValue<T : Any>(val conversion: (value: String, name: String, helpMessage: String)->T) {

        protected lateinit var values: T
        var valueOrigin = ValueOrigin.UNSET
            protected set

        abstract fun addValue(stringValue: String, argumentName: String)

        fun addDefaultValue(stringValue: String, argumentName: String) {
            addValue(stringValue, argumentName)
            valueOrigin = ValueOrigin.SET_DEFAULT_VALUE
        }

        abstract fun isEmpty(): Boolean
        protected fun valuesAreInitialized() = ::values.isInitialized
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            values = value
            valueOrigin = ValueOrigin.REDEFINED
        }
    }

    // Single argument value.
    inner abstract class ArgumentSingleValue<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentValue<T>(conversion) {

        override fun addValue(stringValue: String, argumentName: String) {
            if (!valuesAreInitialized()) {
                values = conversion(stringValue, argumentName, makeUsage())
                valueOrigin = ValueOrigin.SET_BY_USER
            } else {
                printError("Try to provide more than one value $values and $stringValue for $argumentName.")
            }
        }

        override fun isEmpty(): Boolean = !valuesAreInitialized()
    }

    inner class ArgumentSingleNullableValue<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentSingleValue<T>(conversion), ArgumentValueInterface<T?> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = if (!isEmpty()) values else null
    }

    inner class ArgumentSingleValueWithDefault<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentSingleValue<T>(conversion), ArgumentValueInterface<T> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = values
    }

    inner class ArgumentMultipleValues<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentValue<MutableList<T>> (
            { value, name, _ -> mutableListOf(conversion(value, name, makeUsage())) }
    ), ArgumentValueInterface<MutableList<T>> {

        init {
            values = mutableListOf()
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): MutableList<T> = values

        override fun addValue(stringValue: String, argumentName: String) {
            values.addAll(conversion(stringValue, argumentName, makeUsage()))
            valueOrigin = ValueOrigin.SET_BY_USER
        }

        override fun isEmpty() = values.isEmpty()
    }

    // Output error. Also adds help usage information for easy understanding of problem.
    fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

    // Get origin of option value.
    fun getOrigin(name: String) = options[name]?.argumentValue?.valueOrigin ?:
        arguments[name]?.argumentValue?.valueOrigin ?: printError("No option/argument $name in list of avaliable options")

    // Save value as argument value.
    private fun saveAsArg(arg: String, argumentsQueue: ArgumentsQueue): Boolean {
        // Find next uninitialized arguments.
        val name = argumentsQueue.pop()
        name?.let {
            val argumentValue = arguments[name]!!
            argumentValue.descriptor.deprecatedWarning?.let { println ("Warning: $it") }
            argumentValue.addValue(arg)
            return true
        }
        return false
    }

    // Save value as option value.
    private fun <T : Any, U: Any> saveAsOption(parsingValue: ParsingValue<T, U>, value: String) {
        parsingValue.addValue(value)
    }

    // Try to recognize command line element as full form of option.
    protected fun recognizeOptionFullForm(candidate: String) =
        if (candidate.startsWith(optionFullFormPrefix))
            options[candidate.substring(optionFullFormPrefix.length)]
        else null

    // Try to recognize command line element as short form of option.
    protected fun recognizeOptionShortForm(candidate: String) =
            if (candidate.startsWith(optionShortFromPrefix))
                shortNames[candidate.substring(optionShortFromPrefix.length)]
            else null

    // Parse arguments.
    // Returns true if all arguments were parsed, otherwise return false and print help message.
    fun parse(args: Array<String>): Boolean {
        // Add help option.
        val helpDescriptor = if (useDefaultHelpShortName) OptionDescriptor(ArgType.Boolean,
                "help", "h", "Usage info")
            else OptionDescriptor(ArgType.Boolean, "help", description = "Usage info")
        options["help"] = ParsingValue(helpDescriptor, ArgumentSingleNullableValue(helpDescriptor.type.convertion))

        // Add default list with arguments if there can be extra free arguments.
        if (skipExtraArguments) {
            arguments(ArgType.String)
        }
        val argumentsQueue = ArgumentsQueue(arguments.map { it.value.descriptor as ArgDescriptor<*> })

        // Fill map with short names of options.
        shortNames = options.filter { (it.value.descriptor as? OptionDescriptor<*>)?.shortName != null }.
                map { (it.value.descriptor as OptionDescriptor<*>).shortName!! to it.value }.toMap()

        var index = 0
        while (index < args.size) {
            val arg = args[index]
            // Check for subcommands.
            subcommands.forEach { (name, subcommand) ->
                if (arg == name) {
                    // Use parser for this subcommand.
                    val parseResult = subcommand.parse(args.slice(index + 1..args.size - 1).toTypedArray())
                    if (parseResult)
                        subcommand.execute()
                    return true
                }
            }
            // Parse argumnets from command line.
            if (arg.startsWith('-')) {
                // Candidate in being option.
                // Option is found.
                val argValue = recognizeOptionShortForm(arg) ?: recognizeOptionFullForm(arg)
                argValue?.descriptor?.let {
                    if (argValue.descriptor.type.hasParameter) {
                        if (index < args.size - 1) {
                            saveAsOption(argValue, args[index + 1])
                            index++
                        } else {
                            // An error, option with value without value.
                            printError("No value for ${argValue.descriptor.textDescription}")
                        }
                    } else {
                        // Boolean flags.
                        if (argValue.descriptor.fullName == "help") {
                            println(makeUsage())
                            return false
                        }
                        saveAsOption(argValue, "true")
                    }
                } ?: run {
                    // Try save as argument.
                    if (!saveAsArg(arg, argumentsQueue)) {
                        printError("Unknown option $arg")
                    }
                }
            } else {
                // Argument is found.
                if (!saveAsArg(arg, argumentsQueue)) {
                    printError("Too many arguments! Couldn't proccess argument $arg!")
                }
            }
            index++
        }

        // Postprocess results of parsing.
        options.values.union(arguments.values).forEach { value ->
            // Not inited, append default value if needed.
            if (value.argumentValue.isEmpty()) {
                value.addDefaultValue()
            }
        }
        return true
    }

    private fun makeUsage(): String {
        val result = StringBuilder()
        result.append("Usage: $programName options_list\n")
        if (!arguments.isEmpty()) {
            result.append("Arguments: \n")
            arguments.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        result.append("Options: \n")
        options.forEach {
            result.append(it.value.descriptor.helpMessage)
        }
        return result.toString()
    }
}