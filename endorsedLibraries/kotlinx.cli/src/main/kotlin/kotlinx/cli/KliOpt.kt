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
internal class ArgumentsQueue(argumentsDescriptors: List<ArgParser.ArgDescriptor<*, *>>) {
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

interface DelegateProvider<T> {
    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T>
}

/**
 * Interface of argument value.
 */
interface ArgumentValueDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
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
internal abstract class Descriptor<T : Any, TResult : Any>(val type: ArgType<T>,
                                   val fullName: String,
                                   val description: String? = null,
                                   val defaultValue: TResult? = null,
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

    /**
     * Provide text description of value.
     *
     * @param value value got getting text description for.
     */
    fun valueDescription(value: TResult?) = value?.let {
        if (it is List<*> && !it.isEmpty())
            " [${it.joinToString { it.toString() }}]"
        else if (it !is List<*>)
            " [$it]"
        else null
    }

    /**
     * Flag to check if descriptor has set default value for option/argument.
     */
    val defaultValueSet by lazy {
        defaultValue != null && (defaultValue is List<*> && defaultValue.isNotEmpty() || defaultValue !is List<*>)
    }
}

/**
 * Argument parsing result.
 * Contains name of subcommand which was called.
 *
 * @property commandName name of command which was called.
 */
class ArgParserResult(val commandName: String)

/**
 * Parsing value of option/argument.
 */
internal abstract class ParsingValue<T: Any, TResult: Any>(val descriptor: Descriptor<T, TResult>) {
    /**
     * Values of arguments.
     */
    protected lateinit var value: TResult

    /**
     * Value origin.
     */
    var valueOrigin = ArgParser.ValueOrigin.UNSET
        protected set

    /**
     * Check if values of argument are empty.
     */
    abstract fun isEmpty(): Boolean

    /**
     * Check if value of argument was initialized.
     */
    protected fun valueIsInitialized() = ::value.isInitialized

    /**
     * Sace value from command line.
     *
     * @param stringValue value from command line.
     */
    protected abstract fun saveValue(stringValue: String)

    /**
     * Set value of delegated property.
     */
    fun setDelegatedValue(providedValue: TResult) {
        value = providedValue
        valueOrigin = ArgParser.ValueOrigin.REDEFINED
    }

    /**
     * Add parsed value from command line.
     */
    fun addValue(stringValue: String) {
        // Check of possibility to set several values to one option/argument.
        if (descriptor is ArgParser.OptionDescriptor<*, *> && !descriptor.multiple &&
                !isEmpty() && descriptor.delimiter == null) {
            throw ParsingException("Try to provide more than one value for ${descriptor.fullName}.")
        }
        // Show deprecated warning only first time of using option/argument.
        descriptor.deprecatedWarning?.let {
            if (isEmpty())
                println ("Warning: $it")
        }
        // Split value if needed.
        if (descriptor is ArgParser.OptionDescriptor<*, *> && descriptor.delimiter != null) {
            stringValue.split(descriptor.delimiter).forEach {
                saveValue(it)
            }
        } else {
            saveValue(stringValue)
        }
    }

    /**
     * Set default value to option.
     */
    fun addDefaultValue() {
        if (!descriptor.defaultValueSet && descriptor.required) {
            throw ParsingException("Please, provide value for ${descriptor.textDescription}. It should be always set.")
        }
        if (descriptor.defaultValueSet) {
            value = descriptor.defaultValue!!
            valueOrigin = ArgParser.ValueOrigin.SET_DEFAULT_VALUE
        }
    }
}

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
    private val options = mutableMapOf<String, ParsingValue<*, *>>()
    /**
     * Map of arguments: key - fullname of argument, value - pair of descriptor and parsed values.
     */
    private val arguments = mutableMapOf<String, ParsingValue<*, *>>()

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
     internal inner class OptionDescriptor<T : Any, TResult: Any>(
            type: ArgType<T>,
            fullName: String,
            val shortName: String ? = null,
            description: String? = null,
            defaultValue: TResult? = null,
            required: Boolean = false,
            val multiple: Boolean = false,
            val delimiter: String? = null,
            deprecatedWarning: String? = null) : Descriptor<T, TResult>(type, fullName, description, defaultValue,
            required, deprecatedWarning) {

        override val textDescription: String
            get() = "option $optionFullFormPrefix$fullName"

        override val helpMessage: String
            get() {
                val result = StringBuilder()
                result.append("    $optionFullFormPrefix$fullName")
                shortName?.let { result.append(", $optionShortFromPrefix$it") }
                valueDescription(defaultValue)?.let {
                    result.append(" [$it]")
                }
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
     * @property description text description of argument.
     * @property defaultValue default value for argument.
     * @property required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @property deprecatedWarning text message with information in case if argument is deprecated.
     */
     internal inner class ArgDescriptor<T : Any, TResult : Any>(
            type: ArgType<T>,
            fullName: String,
            val number: Int? = null,
            description: String? = null,
            defaultValue: TResult? = null,
            required: Boolean = true,
            deprecatedWarning: String? = null) : Descriptor<T, TResult>(type, fullName, description, defaultValue,
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
                valueDescription(defaultValue)?.let {
                    result.append(" [$it]")
                }
                description?.let { result.append(" -> ${it}") }
                if (!required) result.append(" (optional)")
                result.append(" ${type.description}")
                deprecatedWarning?.let { result.append(" Warning: $it") }
                result.append("\n")
                return result.toString()
            }
    }

    private fun addOption(descriptor: OptionDescriptor<*, *>, value: ParsingValue<*, *>) {
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

    private fun addArgument(name: String, value: ParsingValue<*, *>) {
        if (arguments.containsKey(name)) {
            error("Option with full name $name was already added.")
        }
        arguments[name] = value
    }

    /**
     * Loader for option with multiple possible values.
     */
    inner class MultipleOptionsLoader<T : Any>(private val type: ArgType<T>,
                                               private val fullName: String? = null,
                                               private val shortName: String? = null,
                                               private val description: String? = null,
                                               private val defaultValue: List<T> = emptyList(),
                                               private val required: Boolean = false,
                                               private val multiple: Boolean = false,
                                               private val delimiter: String? = null,
                                               private val deprecatedWarning: String? = null) : DelegateProvider<List<T>> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, defaultValue.toMutableList(),
                    required, multiple, delimiter, deprecatedWarning)
            if (!multiple && delimiter == null)
                printError("Several values are expected for option $name. " +
                        "Option must be used multiple times or split with delimiter.")
            val cliElement = ArgumentMultipleValues(descriptor)
            addOption(descriptor, cliElement)
            return cliElement
        }
    }

    /**
     * Add option with single possible value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param shortName option short name.
     * @param description text description of option.
     * @param required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        required: Boolean = false,
                        deprecatedWarning: String? = null) = object : DelegateProvider<T?> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor<T, T>(type, name, shortName, description, null,
                    required, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(descriptor)
            addOption(descriptor, cliElement)
            return cliElement
        }
    }

    /**
     * Add option with single possible value with default and get delegator to its value.
     *
     * @param type option type, one of [ArgType].
     * @param fullName option full name.
     * @param shortName option short name.
     * @param description text description of option.
     * @param defaultValue default value for option.
     * @param required if option is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if option is deprecated.
     */
    fun <T : Any>option(type: ArgType<T>,
                        fullName: String? = null,
                        shortName: String ? = null,
                        description: String? = null,
                        defaultValue: T,
                        deprecatedWarning: String? = null) = object : DelegateProvider<T> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
            val name = fullName ?: prop.name
            val descriptor = OptionDescriptor(type, name, shortName, description, defaultValue,
                    false, deprecatedWarning = deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(descriptor)
            addOption(descriptor, cliElement)
            return cliElement
        }
    }

    /**
     * Add option with multiple possible values and get delegator to its values.
     *
     * @param type option type, one of [ArgType].
     * @param fullName option full name.
     * @param shortName option short name.
     * @param description text description of option.
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
     * Loader for option with multiple possible values.
     */
    inner class MultipleArgumentsLoader<T : Any>(private val type: ArgType<T>,
                                                 private val fullName: String? = null,
                                                 private val number: Int? = null,
                                                 private val description: String? = null,
                                                 private val defaultValue: List<T> = emptyList(),
                                                 private val required: Boolean = true,
                                                 private val deprecatedWarning: String? = null) : DelegateProvider<List<T>> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, number, description,
                    defaultValue.toMutableList(), required, deprecatedWarning)
            val cliElement = ArgumentMultipleValues(descriptor)
            // Inspect usage of default and required.
            inspectRequiredUsage(required, name)
            addArgument(name, cliElement)
            return cliElement
        }
    }

    /**
     * Check convenance of required property usage for arguments.
     * Make sense only for several last arguments.
     *
     * @param required required flag for current argument.
     * @param fullName full name of current argument.
     */
    private fun inspectRequiredUsage(required: Boolean, fullName: String) {
        if (required) {
            val previousArgument = arguments.values.lastOrNull()
            previousArgument?.let {
                // Previous argument is optional.
                if (!it.descriptor.required) {
                    printWarning("Argument ${previousArgument.descriptor.fullName} will be always required, " +
                            "because next argument $fullName is always required.")
                }
                // Previous argument has default value.
                it.descriptor.defaultValueSet?.let {
                    printWarning("Default value of argument ${previousArgument.descriptor.fullName} will be unused,  " +
                            "because next argument $fullName is always required.")
                }
            }
        }
    }

    /**
     * Add argument with single nullable value and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param description text description of argument.
     * @param required if argument is required or not. If it's required and not provided in command line and have no default value, error will be generated.
     * @param deprecatedWarning text message with information in case if argument is deprecated.
     */
    fun <T : Any>argument(type: ArgType<T>,
                          fullName: String? = null,
                          description: String? = null,
                          required: Boolean = true,
                          deprecatedWarning: String? = null) = object : DelegateProvider<T?> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor<T, T>(type, name, 1, description,
                    null, required, deprecatedWarning)
            val cliElement = ArgumentSingleNullableValue(descriptor)
            // Inspect usage of default and required.
            inspectRequiredUsage(required, name)
            addArgument(name, cliElement)
            return cliElement
        }
    }

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
                          deprecatedWarning: String? = null) = object : DelegateProvider<T> {
        override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
            val name = fullName ?: prop.name
            val descriptor = ArgDescriptor(type, name, 1, description,
                    defaultValue, false, deprecatedWarning)
            val cliElement = ArgumentSingleValueWithDefault(descriptor)

            addArgument(name, cliElement)
            return cliElement
        }
    }

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
                description, defaultValue, false, deprecatedWarning)
    }

    /**
     * Add argument with [number] possible values and get delegator to its value.
     *
     * @param type argument type, one of [ArgType].
     * @param fullName argument full name.
     * @param number expected number of values. Null means any possible number of values.
     * @param description text description of argument.
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
     * @param description text description of argument.
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
                defaultValue.toMutableList(), required, deprecatedWarning)
        val cliElement = ArgumentMultipleValues(descriptor)
        if ("" in arguments) {
            printError("You can have only one unnamed list with positional arguments.")
        }
        arguments[""] = cliElement
        return cliElement
    }

    /**
     * Single argument value.
     *
     * @property descriptor descriptor of option/argument.
     */
    internal abstract class ArgumentSingleValue<T : Any>(descriptor: Descriptor<T, T>):
            ParsingValue<T, T>(descriptor) {

        override fun saveValue(stringValue: String) {
            if (!valueIsInitialized()) {
                value = descriptor.type.conversion(stringValue, descriptor.fullName)
                valueOrigin = ValueOrigin.SET_BY_USER
            } else {
                throw ParsingException("Try to provide more than one value $value and $stringValue for ${descriptor.fullName}.")
            }
        }

        override fun isEmpty(): Boolean = !valueIsInitialized()
    }

    /**
     * Single nullable argument value.
     *
     * @property descriptor descriptor of option/argument.
     */
    internal inner class ArgumentSingleNullableValue<T : Any>(descriptor: Descriptor<T, T>):
            ArgumentSingleValue<T>(descriptor), ArgumentValueDelegate<T?> {
        private var setToNull = false
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = if (!isEmpty() && !setToNull) value else null
        override operator fun setValue(thisRef: Any?, property: KProperty<*>, providedValue: T?) {
            providedValue?.let {
                setDelegatedValue(it)
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
     * @property descriptor descriptor of option/argument.
     */
    internal inner class ArgumentSingleValueWithDefault<T : Any>(descriptor: Descriptor<T, T>):
            ArgumentSingleValue<T>(descriptor), ArgumentValueDelegate<T> {
        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setDelegatedValue(value)
        }
    }

    /**
     * Multiple argument values.
     *
     * @property descriptor descriptor of option/argument.
     */
    internal inner class ArgumentMultipleValues<T : Any>(descriptor: Descriptor<T, MutableList<T>>):
    ParsingValue<T, MutableList<T>>(descriptor), ArgumentValueDelegate<List<T>> {

        init {
            value = mutableListOf()
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): List<T> = value

        override fun saveValue(stringValue: String) {
            value.add(descriptor.type.conversion(stringValue, descriptor.fullName))
            valueOrigin = ValueOrigin.SET_BY_USER
        }

        override fun isEmpty() = value.isEmpty()

        override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: List<T>) {
            setDelegatedValue(value.toMutableList())
        }
    }

    /**
     * Output error. Also adds help usage information for easy understanding of problem.
     *
     * @param message error message.
     */
    internal fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

    /**
     * Output warning
     *
     * @param message warning message.
     */
    internal fun printWarning(message: String) {
        println("WARNING $message")
    }

    /**
     * Get origin of option value.
     *
     * @param name name of argument/option.
     */
    fun getOrigin(name: String) = options[name]?.valueOrigin ?:
        arguments[name]?.valueOrigin ?: printError("No option/argument $name in list of available options")

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
    private fun recognizeOptionFullForm(candidate: String) =
        if (candidate.startsWith(optionFullFormPrefix))
            options[candidate.substring(optionFullFormPrefix.length)]
        else null

    /**
     * Try to recognize command line element as short form of option.
     *
     * @param candidate string with candidate in options.
     */
    private fun recognizeOptionShortForm(candidate: String) =
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
    fun parse(args: Array<String>) = parse(args.asList())

    protected fun parse(args: List<String>): ArgParserResult {
        // Add help option.
        val helpDescriptor = if (useDefaultHelpShortName) OptionDescriptor<Boolean, Boolean>(ArgType.Boolean,
                "help", "h", "Usage info")
            else OptionDescriptor(ArgType.Boolean, "help", description = "Usage info")
        addOption(helpDescriptor, ArgumentSingleNullableValue(helpDescriptor))

        // Add default list with arguments if there can be extra free arguments.
        if (skipExtraArguments) {
            arguments(ArgType.String)
        }
        val argumentsQueue = ArgumentsQueue(arguments.map { it.value.descriptor as ArgDescriptor<*, *> })

        var index = 0
        try {
            while (index < args.size) {
                val arg = args[index]
                // Check for subcommands.
                @UseExperimental(ExperimentalCli::class)
                subcommands.forEach { (name, subcommand) ->
                    if (arg == name) {
                        // Use parser for this subcommand.
                        subcommand.parse(args.slice(index + 1..args.size - 1))
                        subcommand.execute()

                        return ArgParserResult(name)
                    }
                }
                // Parse arguments from command line.
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
                        printError("Too many arguments! Couldn't process argument $arg!")
                    }
                }
                index++
            }
            // Postprocess results of parsing.
            options.values.union(arguments.values).forEach { value ->
                // Not inited, append default value if needed.
                if (value.isEmpty()) {
                    value.addDefaultValue()
                }
            }
        } catch (exception: ParsingException) {
            printError(exception.message!!)
        }

        return ArgParserResult(programName)
    }

    /**
     * Create message with usage description.
     */
    internal fun makeUsage(): String {
        val result = StringBuilder()
        result.append("Usage: ${fullCommandName.joinToString(" ")} options_list\n")
        if (arguments.isNotEmpty()) {
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