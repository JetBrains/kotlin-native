/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlinx.cli

import kotlin.reflect.KProperty

internal expect fun exitProcess(status: Int): Nothing

/**
 * Queue of arguments descriptors.
 * Arguments can have several values, so one descriptor can be returned several times.
 */
internal class ArgumentsQueue(argumentsDescriptors: List<ArgParser.ArgDescriptor<*>>) {
    /**
     * Map of arguments descriptors and their current usage number.
     */
    private val argumentsUsageNumber = linkedMapOf(*argumentsDescriptors.map { it to 0 }.toTypedArray())

    /**
     * Get next descriptor from queue.
     */
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

/**
 * Abstract base class for subcommands.
 */
@SinceKotlin("1.3")
@ExperimentalCli
abstract class Subcommand(val name: String): ArgParser(name) {
    /**
     * Execute action if subcommand was provided.
     */
    abstract fun execute()
}

/**
 * Common descriptor both for options and positional arguments.
 *
 * @property type option/argument type, one of [ArgType].
 * @property fullName option/argument full name.
 * @property description text description of option/argument.
 * @property defaultValue default value for option/argument.
 * @property required if option/argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
 * @property deprecatedWarning text message with information in case if option is deprecated.
 */
internal abstract class Descriptor<T : Any>(val type: ArgType<T>,
                                   val fullName: String,
                                   val description: String? = null,
                                   val defaultValue: List<T> = emptyList(),
                                   val required: Boolean = false,
                                   val deprecatedWarning: String? = null) {
    /**
     * Text description for help message.
     */
    abstract val textDescription: String
    /**
     * Help message for descriptor.
     */
    abstract val helpMessage: String
}

/**
 * Argument parsing result.
 * Contains name of subcommand which was called.
 *
 * @property commandName name of command which was called.
 */
class ArgParserResult(val commandName: String)

/**
 * Arguments parser.
 *
 * @property programName name of current program.
 * @property useDefaultHelpShortName add or not -h flag for help message.
 * @property prefixStyle style of expected options prefix.
 * @property skipExtraArguments just skip extra arhuments in command line string without producing error message.
 */
open class ArgParser(val programName: String, var useDefaultHelpShortName: Boolean = true,
                     var prefixStyle: OPTION_PREFIX_STYLE = OPTION_PREFIX_STYLE.LINUX,
                     var skipExtraArguments: Boolean = false) {

    /**
     * Map of options: key - fullname of option, value - pair of descriptor and parsed values.
     */
    internal val options = mutableMapOf<String, ParsingValue<*, *>>()
    /**
     * Map of arguments: key - fullname of argument, value - pair of descriptor and parsed values.
     */
    internal val arguments = mutableMapOf<String, ParsingValue<*, *>>()
    /**
     * Map of subcommands.
     */
    @UseExperimental(ExperimentalCli::class)
    protected val subcommands = mutableMapOf<String, Subcommand>()

    /**
     * Mapping for short options names for quick search.
     */
    private var shortNames = mutableMapOf<String, ParsingValue<*, *>>()

    /**
     * Used prefix form for full option form.
     */
    protected val optionFullFormPrefix = if (prefixStyle == OPTION_PREFIX_STYLE.LINUX) "--" else "-"

    /**
     * Used prefix form for short option form.
     */
    protected val optionShortFromPrefix = "-"

    /**
     * Name with all commands that should be executed.
     */
    protected val fullCommandName = mutableListOf<String>(programName)

    /**
     * Origin of option/argument value.
     *
     * Possible values:
     * SET_BY_USER - value of option was provided in command line string;
     * SET_DEFAULT_VALUE - value of option wasn't provided in command line, but set using default value;
     * UNSET - value of option is unset
     * REDEFINED - value of option was redefined in source code after parsing.
     */
    enum class ValueOrigin { SET_BY_USER, SET_DEFAULT_VALUE, UNSET, REDEFINED }
    /**
     * Options prefix style.
     *
     * Possible values:
     * LINUX - Linux style, for full forms of options "--", for short form - "-"
     * JVM - JVM style, both for full and short forms of options "-"
     */
    enum class OPTION_PREFIX_STYLE { LINUX, JVM }

    /**
     * Option descriptor.
     *
     * Command line entity started with some prefix (-/—) and can have value as next entity in command line string.
     *
     * @property type option type, one of [ArgType].
     * @property fullName option full name.
     * @property shortName option short name.
     * @property description text description of option.
     * @property defaultValue default value for option.
     * @property required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @property multiple if option can be repeated several times in command line with different values. All values are stored.
     * @property delimiter delimiter that separate option provided as one string to several values.
     * @property deprecatedWarning text message with information in case if option is deprecated.
     */
     internal inner class OptionDescriptor<T : Any>(
            type: ArgType<T>,
            fullName: String,
            val shortName: String ? = null,
            description: String? = null,
            defaultValue: List<T> = emptyList(),
            required: Boolean = false,
            val multiple: Boolean = false,
            val delimiter: String? = null,
            deprecatedWarning: String? = null) : Descriptor<T>(type, fullName, description, defaultValue,
            required, deprecatedWarning) {

        override val textDescription: String
            get() = "option $optionFullFormPrefix$fullName"

        override val helpMessage: String
            get() {
                val result = StringBuilder()
                result.append("    $optionFullFormPrefix$fullName")
                shortName?.let { result.append(", $optionShortFromPrefix$it") }
                (defaultValue.joinToString(",") { it.toString() }).also { if (!it.isEmpty()) result.append(" [$it]") }
                description?.let {result.append(" -> ${it}")}
                if (required) result.append(" (always required)")
                result.append(" ${type.description}")
                deprecatedWarning?.let { result.append(" Warning: $it") }
                result.append("\n")
                return result.toString()
            }
    }

    /**
     * Argument descriptor.
     *
     * Command line entity which role is connected only with its position.
     *
     * @property type argument type, one of [ArgType].
     * @property fullName argument full name.
     * @property number expected number of values. Null means any possible number of values.
     * @property description text descrition of argument.
     * @property defaultValue default value for argument.
     * @property required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @property deprecatedWarning text message with information in case if argument is deprecated.
     */
     internal inner class ArgDescriptor<T : Any>(
            type: ArgType<T>,
            fullName: String,
            val number: Int? = null,
            description: String? = null,
            defaultValue: List<T> = emptyList(),
            required: Boolean = true,
            deprecatedWarning: String? = null) : Descriptor<T>(type, fullName, description, defaultValue,
            required, deprecatedWarning) {

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
                if (!required) result.append(" (optional)")
                result.append(" ${type.description}")
                deprecatedWarning?.let { result.append(" Warning: $it") }
                result.append("\n")
                return result.toString()
            }
    }

    internal fun addOption(descriptor: OptionDescriptor<*>, value: ParsingValue<*, *>) {
        if (options.containsKey(descriptor.fullName)) {
            error("Option with full name ${descriptor.fullName} was already added.")
        }
        if (descriptor.shortName != null && shortNames.containsKey(descriptor.shortName)) {
            error("Option with short name ${descriptor.shortName} was already added.")
        }
        options[descriptor.fullName] = value
        descriptor.shortName?.let {
            shortNames[it] = value
        }
    }

    internal fun addArgument(name: String, value: ParsingValue<*, *>) {
        if (arguments.containsKey(name)) {
            error("Option with full name $name was already added.")
        }
        arguments[name] = value
    }

    /**
     * Loader for option with single possible value which is nullable.
     */
    inner class SingleNullableOptionLoader<T : Any>(val type: ArgType<T>,
                                                    val fullName: String? = null,
                                                    val shortName: String? = null,
                                                    val description: String? = null,
                                                    val required: Boolean = false,
                                                    val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, emptyList(),
                    required, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(type.conversion)
            addOption(descriptor, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Loader for option with single possible value which has default value.
     */
    inner class SingleOptionWithDefaultLoader<T : Any>(val type: ArgType<T>,
                                                       val fullName: String? = null,
                                                       val shortName: String ? = null,
                                                       val description: String? = null,
                                                       val defaultValue: T,
                                                       val required: Boolean = false,
                                                       val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, listOf(defaultValue),
                    required, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(type.conversion)
            cliElement.initDefaultValue(defaultValue)
            addOption(descriptor, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Loader for option with multiple possible values.
     */
    inner class MultipleOptionsLoader<T : Any>(val type: ArgType<T>,
                                               val fullName: String? = null,
                                               val shortName: String ? = null,
                                               val description: String? = null,
                                               val defaultValue: List<T> = emptyList(),
                                               val required: Boolean = false,
                                               val multiple: Boolean = false,
                                               val delimiter: String? = null,
                                               val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, defaultValue,
                    required, multiple, delimiter, deprecatedWarning)
            if (!multiple && delimiter == null)
                printError("Several values are expected for option $name. " +
                        "Option must be used multiple times or split with delimiter.")
            val cliElement = ArgumentMultipleValues(type.conversion)
            cliElement.initDefaultValue(defaultValue.toMutableList())
            addOption(descriptor, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Add option with single possible value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param shortName option short name.
     * @param description text descrition of option.
     * @param required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        required: Boolean = false,
                        deprecatedWarning: String? = null) = SingleNullableOptionLoader(type, fullName, shortName,
            description, required, deprecatedWarning)

    /**
     * Add option with single possible value with default and get delegator to its value.
     *
     * @param type option type, one of [ArgType].
     * @param fullName option full name.
     * @param shortName option short name.
     * @param description text descrition of option.
     * @param defaultValue default value for option.
     * @param required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        defaultValue: T,
                        deprecatedWarning: String? = null) = SingleOptionWithDefaultLoader(type, fullName, shortName,
            description, defaultValue, false, deprecatedWarning)

    /**
     * Add option with multiple possible values and get delegator to its values.
     *
     * @param type option type, one of [ArgType].
     * @param fullName option full name.
     * @param shortName option short name.
     * @param description text descrition of option.
     * @param defaultValue default value for option.
     * @param multiple if option can be repeated several times in command line with different values. All values are stored.
     * @param delimiter delimiter that separate option provided as one string to several values.
     * @param deprecatedWarning text message with information in case if option is deprecated.
    */
    fun <T : Any>options(type: ArgType<T>,
                         fullName: String? = null,
                         shortName: String ? = null,
                         description: String? = null,
                         defaultValue: List<T>,
                         multiple: Boolean = false,
                         delimiter: String? = null,
                         deprecatedWarning: String? = null): MultipleOptionsLoader<T> {
        // Default value can't be empty list.
        if (defaultValue.isEmpty()) {
            error("List with default values should contain at least one element.")
        }
        return MultipleOptionsLoader(type, fullName, shortName,
            description, defaultValue, false, multiple, delimiter, deprecatedWarning)
    }

    /**
     * Add option with multiple possible values and get delegator to its values.
     *
     * @param type option type, one of [ArgType].
     * @param fullName option full name.
     * @param shortName option short name.
     * @param description text description of option.
     * @param required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param multiple if option can be repeated several times in command line with different values. All values are stored.
     * @param delimiter delimiter that separate option provided as one string to several values.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>options(type: ArgType<T>,
                         fullName: String? = null,
                         shortName: String ? = null,
                         description: String? = null,
                         required: Boolean = false,
                         multiple: Boolean = false,
                         delimiter: String? = null,
                         deprecatedWarning: String? = null) = MultipleOptionsLoader(type, fullName, shortName,
            description, emptyList(), required, multiple, delimiter, deprecatedWarning)

    /**
     * Loader for argument with single possible value which is nullable.
     */
    inner class SingleNullableArgumentLoader<T : Any>(val type: ArgType<T>,
                                                      val fullName: String? = null,
                                                      val description: String? = null,
                                                      val required: Boolean = true,
                                                      val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, 1, description,
                    emptyList(), required, deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(type.conversion)
            addArgument(name, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Loader for argument with single possible value which has default one.
     */
    inner class SingleArgumentWithDefaultLoader<T : Any>(val type: ArgType<T>,
                                                         val fullName: String? = null,
                                                         val description: String? = null,
                                                         val defaultValue: T,
                                                         val required: Boolean = true,
                                                         val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, 1, description,
                    listOf(defaultValue), required, deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(type.conversion)
            cliElement.initDefaultValue(defaultValue)
            addArgument(name, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Loader for option with multiple possible values.
     */
    inner class MultipleArgumentsLoader<T : Any>(val type: ArgType<T>,
                                                 val fullName: String? = null,
                                                 val number: Int? = null,
                                                 val description: String? = null,
                                                 val defaultValue: List<T> = emptyList(),
                                                 val required: Boolean = true,
                                                 val deprecatedWarning: String? = null) {
        operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, number, description,
                    defaultValue, required, deprecatedWarning)
            val cliElement = ArgumentMultipleValues(type.conversion)
            cliElement.initDefaultValue(defaultValue.toMutableList())
            addArgument(name, ParsingValue(descriptor, cliElement))
            return cliElement
        }
    }

    /**
     * Add argument with single nullable value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param description text descrition of argument.
     * @param required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>argument(type: ArgType<T>,
                          fullName: String? = null,
                          description: String? = null,
                          required: Boolean = true,
                          deprecatedWarning: String? = null) = SingleNullableArgumentLoader(type, fullName, description,
            required, deprecatedWarning)

    /**
     * Add argument with single value with default and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param description text description of argument.
     * @param defaultValue default value for argument.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>argument(type: ArgType<T>,
                          fullName: String? = null,
                          description: String? = null,
                          defaultValue: T,
                          deprecatedWarning: String? = null) = SingleArgumentWithDefaultLoader(type, fullName,
            description, defaultValue, true, deprecatedWarning)

    /**
     * Add argument with [number] possible values and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param number expected number of values. Null means any possible number of values.
     * @param description text description of argument.
     * @param defaultValue default value for argument.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>arguments(type: ArgType<T>,
                           fullName: String? = null,
                           number: Int? = null,
                           description: String? = null,
                           defaultValue: List<T>,
                           deprecatedWarning: String? = null): MultipleArgumentsLoader<T> {
        // Default value can't be empty list.
        if (defaultValue.isEmpty()) {
            error("List with default values should contain at least one element.")
        }
        return MultipleArgumentsLoader(type, fullName, number,
                description, defaultValue, true, deprecatedWarning)
    }

    /**
     * Add argument with [number] possible values and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param number expected number of values. Null means any possible number of values.
     * @param description text descrition of argument.
     * @param required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>arguments(type: ArgType<T>,
                           fullName: String? = null,
                           number: Int? = null,
                           description: String? = null,
                           required: Boolean = true,
                           deprecatedWarning: String? = null) = MultipleArgumentsLoader(type, fullName, number,
            description, emptyList(), required, deprecatedWarning)

    /**
     * Add subcommands.
     *
     * @param subcommandsList subcommands to add.
     */
    @SinceKotlin("1.3")
    @ExperimentalCli
    fun subcommands(vararg subcommandsList: Subcommand) {
        subcommandsList.forEach {
            if (it.name in subcommands) {
                printError("Subcommand with name ${it.name} was already defined.")
            }

            // Set same settings as main parser.
            it.prefixStyle = prefixStyle
            it.useDefaultHelpShortName = useDefaultHelpShortName
            fullCommandName.forEachIndexed { index, namePart ->
                it.fullCommandName.add(index, namePart)
            }
            subcommands[it.name] = it
        }
    }

    /**
     * Get all free arguments as unnamed list.
     *
     * @param type argument type, one of [ArgType].
     * @param description text descrition of argument.
     * @param defaultValue default value for argument.
     * @param required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>arguments(type: ArgType<T>,
                           description: String? = null,
                           defaultValue: List<T> = emptyList(),
                           required: Boolean = true,
                           deprecatedWarning: String? = null): ArgumentValueDelegate<List<T>> {
        val descriptor = ArgDescriptor(type, "", null, description,
                defaultValue, required, deprecatedWarning)
        val cliElement = ArgumentMultipleValues(type.conversion)
        cliElement.initDefaultValue(defaultValue.toMutableList())
        if ("" in arguments) {
            printError("You can have only one unnamed list with positional arguments.")
        }
        arguments[""] = ParsingValue(descriptor, cliElement)
        return cliElement
    }

    /**
     * Parsing value of option/argument.
     */
     internal inner class ParsingValue<T: Any, U: Any>(val descriptor: Descriptor<T>, val argumentValue: ArgumentValue<U>) {

        /**
         * Add parsed value from command line.
         */
        fun addValue(stringValue: String) {
            // Check of possibility to set several values to one option/argument.
            if (descriptor is OptionDescriptor<*> && !descriptor.multiple &&
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
                    argumentValue.addValue(it, descriptor.fullName)
                }
            } else {
                argumentValue.addValue(stringValue, descriptor.fullName)
            }
        }

        /**
         * Set default value to option.
         */
        fun addDefaultValue() {
            if (!argumentValue.addDefaultValue() && descriptor.required) {
                printError("Please, provide value for ${descriptor.textDescription}. It should be always set.")
            }
        }
    }

    /**
     * Interface of argument value.
     */
    interface ArgumentValueDelegate<T> {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    }

    /**
     * Argument/option value.
     */
    abstract class ArgumentValue<T : Any>(val conversion: (value: String, name: String, helpMessage: String)->T) {
        private lateinit var defaultValues: T
        /**
         * Values of arguments.
         */
        protected lateinit var values: T
        /**
         * Value origin.
         */
        var valueOrigin = ValueOrigin.UNSET
            protected set

        /**
         * Add value from command line.
         *
         * @param stringValue value from command line.
         * @param argumentName name of argument value is added for.
         */
        abstract fun addValue(stringValue: String, argumentName: String)

        /**
         * Add default value.
         *
         * @param stringValue value from command line.
         * @param argumentName name of argument value is added for.
         */
        fun addDefaultValue(): Boolean {
            if (!::defaultValues.isInitialized) {
                return false
            }
            values = defaultValues
            valueOrigin = ValueOrigin.SET_DEFAULT_VALUE
            return true
        }

        /**
         * Check if values of argument are empty.
         */
        abstract fun isEmpty(): Boolean

        /**
         * Check if value of argument was initialized.
         */
        protected fun valuesAreInitialized() = ::values.isInitialized

        /**
         * Set value from delegated property.
         */
        fun setValue(value: T) {
            values = value
            valueOrigin = ValueOrigin.REDEFINED
        }

        fun initDefaultValue(defaultValue: T) {
            defaultValues = defaultValue
        }
    }

    /**
     * Single argument value.
     *
     * @property conversion conversion function from string value from command line to expected type.
     */
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

    /**
     * Single nullable argument value.
     *
     * @property conversion conversion function from string value from command line to expected type.
     */
    inner class ArgumentSingleNullableValue<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentSingleValue<T>(conversion), ArgumentValueDelegate<T?> {
        private var setToNull = false
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = if (!isEmpty() && !setToNull) values else null
        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            value?.let {
                setValue(value)
                setToNull = false
            } ?: run {
                setToNull = true
                valueOrigin = ValueOrigin.REDEFINED
            }
        }
    }

    /**
     * Single argument value with default.
     *
     * @property conversion conversion function from string value from command line to expected type.
     */
    inner class ArgumentSingleValueWithDefault<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentSingleValue<T>(conversion), ArgumentValueDelegate<T> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = values
        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setValue(value)
        }
    }

    /**
     * Multiple argument values.
     *
     * @property conversion conversion function from string value from command line to expected type.
     */
    inner class ArgumentMultipleValues<T : Any>(conversion: (value: String, name: String, helpMessage: String)->T):
            ArgumentValue<MutableList<T>>(
            { value, name, _ -> mutableListOf(conversion(value, name, makeUsage())) }
    ), ArgumentValueDelegate<List<T>> {

        init {
            values = mutableListOf()
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): List<T> = values

        override fun addValue(stringValue: String, argumentName: String) {
            values.addAll(conversion(stringValue, argumentName, makeUsage()))
            valueOrigin = ValueOrigin.SET_BY_USER
        }

        override fun isEmpty() = values.isEmpty()

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
            setValue(value.toMutableList())
        }
    }

    /**
     * Output error. Also adds help usage information for easy understanding of problem.
     *
     * @param message error message.
     */
    fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

    /**
     * Get origin of option value.
     *
     * @param name name of argument/option.
     */
    fun getOrigin(name: String) = options[name]?.argumentValue?.valueOrigin ?:
        arguments[name]?.argumentValue?.valueOrigin ?: printError("No option/argument $name in list of avaliable options")

    /**
     * Save value as argument value.
     *
     * @param arg string with argument value.
     * @param argumentsQueue queue with active argument descriptors.
     */
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

    /**
     * Save value as option value.
     */
    private fun <T : Any, U: Any> saveAsOption(parsingValue: ParsingValue<T, U>, value: String) {
        parsingValue.addValue(value)
    }

    /**
     * Try to recognize command line element as full form of option.
     *
     * @param candidate string with candidate in options.
     */
    internal fun recognizeOptionFullForm(candidate: String) =
        if (candidate.startsWith(optionFullFormPrefix))
            options[candidate.substring(optionFullFormPrefix.length)]
        else null

    /**
     * Try to recognize command line element as short form of option.
     *
     * @param candidate string with candidate in options.
     */
    internal fun recognizeOptionShortForm(candidate: String) =
            if (candidate.startsWith(optionShortFromPrefix))
                shortNames[candidate.substring(optionShortFromPrefix.length)]
            else null

    /**
     * Parse arguments.
     *
     * @param args array with command line arguments.
     *
     * @return true if all arguments were parsed successfully, otherwise return false and print help message.
     */
    fun parse(args: Array<String>): ArgParserResult {
        // Add help option.
        val helpDescriptor = if (useDefaultHelpShortName) OptionDescriptor(ArgType.Boolean,
                "help", "h", "Usage info")
            else OptionDescriptor(ArgType.Boolean, "help", description = "Usage info")
        addOption(helpDescriptor, ParsingValue(helpDescriptor, ArgumentSingleNullableValue(helpDescriptor.type.conversion)))

        // Add default list with arguments if there can be extra free arguments.
        if (skipExtraArguments) {
            arguments(ArgType.String)
        }
        val argumentsQueue = ArgumentsQueue(arguments.map { it.value.descriptor as ArgDescriptor<*> })

        var index = 0
        while (index < args.size) {
            val arg = args[index]
            // Check for subcommands.
            @UseExperimental(ExperimentalCli::class)
            subcommands.forEach { (name, subcommand) ->
                if (arg == name) {
                    // Use parser for this subcommand.
                    subcommand.parse(args.slice(index + 1..args.size - 1).toTypedArray())
                    subcommand.execute()

                    return ArgParserResult(name)
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
                            exitProcess(0)
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
        return ArgParserResult(programName)
    }

    /**
     * Create message with usage description.
     */
    internal fun makeUsage(): String {
        val result = StringBuilder()
        result.append("Usage: ${fullCommandName.joinToString(" ")} options_list\n")
        if (!arguments.isEmpty()) {
            result.append("Arguments: \n")
            arguments.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        if (options.isEmpty()) {
            result.append("Options: \n")
            options.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        return result.toString()
    }
}