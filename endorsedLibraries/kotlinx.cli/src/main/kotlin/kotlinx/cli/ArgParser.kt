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
internal class ArgumentsQueue(argumentsDescriptors: List<ArgDescriptor<*, *>>) {
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
 * A property delegate that provides access to the argument/option value.
 */
interface ArgumentValueDelegate<T> {
    /**
     * The value of an option or argument parsed from command line.
     *
     * Accessing this value before [ArgParser.parse] method is called will result in an exception.
     *
     * @see CLIEntity.value
     */
    var value: T

    /** Provides the value for the delegated property getter. Returns the [value] property.
     * @throws IllegalStateException in case of accessing the value before [ArgParser.parse] method is called.
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    /** Sets the [value] to the [ArgumentValueDelegate.value] property from the delegated property setter.
     * This operation is possible only after command line arguments were parsed with [ArgParser.parse]
     * @throws IllegalStateException in case of resetting value before command line arguments are parsed.
     */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

/**
 * Abstract base class for subcommands.
 */
@ExperimentalCli
abstract class Subcommand(val name: String): ArgParser(name) {
    /**
     * Execute action if subcommand was provided.
     */
    abstract fun execute()
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
 * @property programName the name of the current program.
 * @property useDefaultHelpShortName specifies whether to register "-h" option for printing the usage information.
 * @property prefixStyle the style of option prefixing.
 * @property skipExtraArguments specifies whether the extra unmatched arguments in a command line string
 * can be skipped without producing an error message.
 */
open class ArgParser(
    val programName: String,
    var useDefaultHelpShortName: Boolean = true,
    var prefixStyle: OptionPrefixStyle = OptionPrefixStyle.LINUX,
    var skipExtraArguments: Boolean = false
) {

    /**
     * Map of options: key - full name of option, value - pair of descriptor and parsed values.
     */
    private val options = mutableMapOf<String, ParsingValue<*, *>>()
    /**
     * Map of arguments: key - full name of argument, value - pair of descriptor and parsed values.
     */
    private val arguments = mutableMapOf<String, ParsingValue<*, *>>()

    /**
     * Map with declared options.
     */
    private val declaredOptions = mutableListOf<CLIEntityWrapper>()

    /**
     * Map with declared arguments.
     */
    private val declaredArguments = mutableListOf<CLIEntityWrapper>()

    /**
     * State of parser. Stores last parsing result or null.
     */
    private var parsingState: ArgParserResult? = null

    /**
     * Map of subcommands.
     */
    @UseExperimental(ExperimentalCli::class)
    protected val subcommands = mutableMapOf<String, Subcommand>()

    /**
     * Mapping for short options names for quick search.
     */
    private val shortNames = mutableMapOf<String, ParsingValue<*, *>>()

    /**
     * Used prefix form for full option form.
     */
    private val optionFullFormPrefix = if (prefixStyle == OptionPrefixStyle.LINUX) "--" else "-"

    /**
     * Used prefix form for short option form.
     */
    private val optionShortFromPrefix = "-"

    /**
     * Name with all commands that should be executed.
     */
    protected val fullCommandName = mutableListOf(programName)

    /**
     * The way an option/argument has got its value.
     */
    enum class ValueOrigin {
        /* The value was parsed from command line arguments. */
        SET_BY_USER,
        /* The value was missing in command line, therefore the default value was used. */
        SET_DEFAULT_VALUE,
        /* The value is not initialized by command line values or  by default values. */
        UNSET,
        /* The value was redefined after parsing manually (usually with the property setter). */
        REDEFINED,
        /* The value is undefined, because parsing wasn't called. */
        UNDEFINED
    }

    /**
     * The style of option prefixing.
     */
    enum class OptionPrefixStyle {
        /* Linux style: the full name of an option is prefixed with two hyphens "--" and the short name — with one "-". */
        LINUX,
        /* JVM style: both full and short names are prefixed with one hyphen "-". */
        JVM,
    }

    @Deprecated("OPTION_PREFIX_STYLE is deprecated. Please, use OptionPrefixStyle.",
        ReplaceWith("OptionPrefixStyle", "kotlinx.cli.OptionPrefixStyle"))
    @Suppress("TOPLEVEL_TYPEALIASES_ONLY")
    typealias OPTION_PREFIX_STYLE = OptionPrefixStyle

    /**
     * Declares a named option and returns an object which can be used to access the option value
     * after all arguments are parsed or to delegate a property for accessing the option value to.
     *
     * By default, the option supports only a single value, is optional, and has no default value,
     * therefore its value's type is `T?`.
     *
     * You can alter the option properties by chaining extensions for the option type on the returned object:
     *   - [AbstractSingleOption.default] to provide a default value that is used when the option is not specified;
     *   - [SingleNullableOption.required] to make the option non-optional;
     *   - [AbstractSingleOption.delimiter] to allow specifying multiple values in one command line argument with a delimiter;
     *   - [AbstractSingleOption.multiple] to allow specifying the option several times.
     *
     * @param type The type describing how to parse an option value from a string,
     * an instance of [ArgType], e.g. [ArgType.String] or [ArgType.Choice].
     * @param fullName the full name of the option, can be omitted if the option name is inferred
     * from the name of a property delegated to this option.
     * @param shortName the short name of the option, `null` if the option cannot be specified in a short form.
     * @param description the description of the option used when rendering the usage information.
     * @param deprecatedWarning the deprecation message for the option.
     * Specifying anything except `null` makes this option deprecated. The message is rendered in a help message and
     * issued as a warning when the option is encountered when parsing command line arguments.
     */
    fun <T : Any> option(
        type: ArgType<T>,
        fullName: String? = null,
        shortName: String ? = null,
        description: String? = null,
        deprecatedWarning: String? = null
    ): SingleNullableOption<T> {
        val option = SingleNullableOption(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type,
                fullName, shortName, description, deprecatedWarning = deprecatedWarning), CLIEntityWrapper())
        option.owner.entity = option
        declaredOptions.add(option.owner)
        return option
    }

    /**
     * Check usage of required property for arguments.
     * Make sense only for several last arguments.
     */
    private fun inspectRequiredAndDefaultUsage() {
        var previousArgument: ParsingValue<*, *>? = null
        arguments.forEach { (_, currentArgument) ->
            previousArgument?.let { previous ->
                // Previous argument has default value.
                if (previous.descriptor.defaultValueSet) {
                    if (!currentArgument.descriptor.defaultValueSet && currentArgument.descriptor.required) {
                        error("Default value of argument ${previous.descriptor.fullName} will be unused,  " +
                                "because next argument ${currentArgument.descriptor.fullName} is always required and has no default value.")
                    }
                }
                // Previous argument is optional.
                if (!previous.descriptor.required) {
                    if (!currentArgument.descriptor.defaultValueSet && currentArgument.descriptor.required) {
                        error("Argument ${previous.descriptor.fullName} will be always required, " +
                                "because next argument ${currentArgument.descriptor.fullName} is always required.")
                    }
                }
            }
            previousArgument = currentArgument
        }
    }

    /**
     * Declares an argument and returns an object which can be used to access the argument value
     * after all arguments are parsed or to delegate a property for accessing the argument value to.
     *
     * By default, the argument supports only a single value, is required, and has no default value,
     * therefore its value's type is `T`.
     *
     * You can alter the argument properties by chaining extensions for the argument type on the returned object:
     *   - [AbstractSingleArgument.default] to provide a default value that is used when the argument is not specified;
     *   - [SingleArgument.optional] to allow omitting the argument;
     *   - [AbstractSingleArgument.multiple] to require the argument to have exactly the number of values specified;
     *   - [AbstractSingleArgument.vararg] to allow specifying an unlimited number of values for the _last_ argument.
     *
     * @param type The type describing how to parse an option value from a string,
     * an instance of [ArgType], e.g. [ArgType.String] or [ArgType.Choice].
     * @param fullName the full name of the argument, can be omitted if the argument name is inferred
     * from the name of a property delegated to this argument.
     * @param description the description of the argument used when rendering the usage information.
     * @param deprecatedWarning the deprecation message for the argument.
     * Specifying anything except `null` makes this argument deprecated. The message is rendered in a help message and
     * issued as a warning when the argument is encountered when parsing command line arguments.
     */
    fun <T : Any> argument(
        type: ArgType<T>,
        fullName: String? = null,
        description: String? = null,
        deprecatedWarning: String? = null
    ) : SingleArgument<T, DefaultRequiredType.Required> {
        val argument = SingleArgument<T, DefaultRequiredType.Required>(ArgDescriptor(type, fullName, 1,
                description, deprecatedWarning = deprecatedWarning), CLIEntityWrapper())
        argument.owner.entity = argument
        declaredArguments.add(argument.owner)
        return argument
    }

    /**
     * Registers one or more subcommands.
     *
     * @param subcommandsList subcommands to add.
     */
    @ExperimentalCli
    fun subcommands(vararg subcommandsList: Subcommand) {
        subcommandsList.forEach {
            if (it.name in subcommands) {
                error("Subcommand with name ${it.name} was already defined.")
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
     * Outputs an error message adding the usage information after it.
     *
     * @param message error message.
     */
     fun printError(message: String): Nothing {
        error("$message\n${makeUsage()}")
    }

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
            argumentValue.descriptor.deprecatedWarning?.let { printWarning(it) }
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
     * Parses the provided array of command line arguments.
     * After a successful parsing, the options and arguments declared in this parser get their values and can be accessed
     * with the properties delegated to them.
     *
     * @param args the array with command line arguments.
     *
     * @return an [ArgParserResult] if all arguments were parsed successfully.
     * Otherwise, prints the usage information and terminates the program execution.
     * @throws IllegalStateException in case of attempt of calling parsing several times.
     */
    fun parse(args: Array<String>): ArgParserResult = parse(args.asList())

    protected fun parse(args: List<String>): ArgParserResult {
        check(parsingState == null) { "Parsing of command line options can be called only once." }
        // Add help option.
        val helpDescriptor = if (useDefaultHelpShortName) OptionDescriptor<Boolean, Boolean>(
            optionFullFormPrefix,
            optionShortFromPrefix, ArgType.Boolean,
            "help", "h", "Usage info"
        )
        else OptionDescriptor(
            optionFullFormPrefix, optionShortFromPrefix,
            ArgType.Boolean, "help", description = "Usage info"
        )
        val helpOption = SingleNullableOption(helpDescriptor, CLIEntityWrapper())
        helpOption.owner.entity = helpOption
        declaredOptions.add(helpOption.owner)

        // Add default list with arguments if there can be extra free arguments.
        if (skipExtraArguments) {
            argument(ArgType.String, "").vararg()
        }

        // Clean options and arguments maps.
        options.clear()
        arguments.clear()

        // Map declared options and arguments to maps.
        declaredOptions.forEachIndexed { index, option ->
            val value = option.entity?.delegate as ParsingValue<*, *>
            value.descriptor.fullName?.let {
                // Add option.
                if (options.containsKey(it)) {
                    error("Option with full name $it was already added.")
                }
                with(value.descriptor as OptionDescriptor) {
                    if (shortName != null && shortNames.containsKey(shortName)) {
                        error("Option with short name ${shortName} was already added.")
                    }
                    shortName?.let {
                        shortNames[it] = value
                    }
                }
                options[it] = value

            } ?: error("Option was added, but unnamed. Added option under №${index + 1}")
        }

        declaredArguments.forEachIndexed { index, argument ->
            val value = argument.entity?.delegate as ParsingValue<*, *>
            value.descriptor.fullName?.let {
                // Add option.
                if (arguments.containsKey(it)) {
                    error("Argument with full name $it was already added.")
                }
                arguments[it] = value
            } ?: error("Argument was added, but unnamed. Added argument under №${index + 1}")
        }
        // Make inspections for arguments.
        inspectRequiredAndDefaultUsage()

        listOf(arguments, options).forEach {
            it.forEach { (_, value) ->
                value.valueOrigin = ValueOrigin.UNSET
            }
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
                        parsingState = ArgParserResult(name)

                        return parsingState!!
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
                if (value.valueOrigin != ValueOrigin.SET_BY_USER && value.descriptor.required) {
                    printError("Value for ${value.descriptor.textDescription} should be always provided in command line.")
                }
            }
        } catch (exception: ParsingException) {
            printError(exception.message!!)
        }
        parsingState = ArgParserResult(programName)
        return parsingState!!
    }

    /**
     * Creates a message with the usage information.
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
        if (options.isNotEmpty()) {
            result.append("Options: \n")
            options.forEach {
                result.append(it.value.descriptor.helpMessage)
            }
        }
        return result.toString()
    }
}

/**
 * Output warning.
 *
 * @param message warning message.
 */
internal fun printWarning(message: String) {
    println("WARNING $message")
}