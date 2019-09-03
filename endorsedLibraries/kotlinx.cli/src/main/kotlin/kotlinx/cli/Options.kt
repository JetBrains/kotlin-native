/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

/**
 * Option instance interface.
 */
interface Option<T : Any, TResult>: CLIEntity<T, TResult>
/**
 * Common single option instance interface.
 */
interface AbstractSingleOption<T : Any, TResult> : Option<T, TResult>
/**
 * Option wit single non-nullable value.
 */
interface SingleOption<T : Any> : AbstractSingleOption<T, T>
/**
 * Option with single nullable value.
 */
interface SingleNullableOption<T : Any> : AbstractSingleOption<T, T?>

/**
 * Base interface for all possible types of options with multiple values.
 */
interface MultipleOptionType

/**
 * Type of option with multiple values that can be provided several times in command line.
 */
class RepeatedOption: MultipleOptionType

/**
 * Type of option with multiple values that are provided using delimiter.
 */
class DelimitedOption: MultipleOptionType

/**
 * Type of option with multiple values that can be both provided several times in command line and using delimiter.
 */
class RepeatedDelimitedOption: MultipleOptionType

/**
 * Option with multiple values.
 */
interface MultipleOption<T : Any, TResult : Collection<T>, OptionType: MultipleOptionType> : Option<T, TResult>

internal abstract class OptionImpl<T : Any, TResult>(owner: CLIEntityWrapper): CLIEntityImpl<T, TResult>(owner),
        Option<T, TResult>

internal abstract class AbstractSingleOptionImpl<T: Any, TResult>(owner: CLIEntityWrapper): OptionImpl<T, TResult>(owner),
        AbstractSingleOption<T, TResult> {
    /**
     * Check descriptor for this kind of option.
     */
    fun checkDescriptor(descriptor: OptionDescriptor<*, *>) {
        if (descriptor.multiple || descriptor.delimiter != null) {
            error("Option with single value can't be initialized with descriptor for multiple values.")
        }
    }
}

internal class SingleOptionImpl<T : Any>(descriptor: OptionDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleOptionImpl<T, T>(owner),
        SingleOption<T> {
    init {
        checkDescriptor(descriptor)
        cliElement = ArgumentSingleValue(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
        (cliElement as ParsingValue<T, T>).provideName(prop.name)
        return cliElement
    }
}

internal class SingleNullableOptionImpl<T : Any>(descriptor: OptionDescriptor<T, T>, owner: CLIEntityWrapper):
        AbstractSingleOptionImpl<T, T?>(owner),
        SingleNullableOption<T> {
    init {
        checkDescriptor(descriptor)
        cliElement = ArgumentSingleNullableValue(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
        (cliElement as ParsingValue<T, T>).provideName(prop.name)
        return cliElement
    }
}

internal class MultipleOptionImpl<T : Any, OptionType: MultipleOptionType>(descriptor: OptionDescriptor<T, List<T>>, owner: CLIEntityWrapper):
        OptionImpl<T, List<T>>(owner), MultipleOption<T, List<T>, OptionType> {
    init {
        if (!descriptor.multiple && descriptor.delimiter == null) {
            error("Option with multiple values can't be initialized with descriptor for single one.")
        }
        cliElement = ArgumentMultipleValues(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
        (cliElement as ParsingValue<T, List<T>>).provideName(prop.name)
        return cliElement
    }
}

/**
 * Allow option have several values.
 */
fun <T : Any, TResult> AbstractSingleOption<T, TResult>.multiple(): MultipleOption<T, List<T>, RepeatedOption> {
    this as AbstractSingleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        MultipleOptionImpl<T, RepeatedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, true, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow option have several values.
 */
fun <T : Any, TResult: Collection<T>> MultipleOption<T, TResult, DelimitedOption>.multiple(): MultipleOption<T, List<T>, RepeatedDelimitedOption> {
    this as MultipleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, Collection<T>>).descriptor as OptionDescriptor) {
        if (multiple) {
            error("Try to use modifier multiple() twice on option ${fullName ?: ""}")
        }
        MultipleOptionImpl<T, RepeatedDelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, true, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Set default option value.
 *
 * @param value default value.
 */
fun <T: Any, TResult> AbstractSingleOption<T, TResult>.default(value: T): SingleOption<T> {
    this as AbstractSingleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        if (required) {
            printWarning("required() is unneeded, because option with default value is defined.")
        }
        SingleOptionImpl(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, value, required, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Set default option value.
 *
 * @param value default value.
 */
fun <T: Any, TResult: Collection<T>, OptionType: MultipleOptionType>
        MultipleOption<T, TResult, OptionType>.default(value: TResult): MultipleOption<T, List<T>, OptionType> {
    this as MultipleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, TResult>).descriptor as OptionDescriptor) {
        if (required) {
            printWarning("required() is unneeded, because option with default value is defined.")
        }
        MultipleOptionImpl<T, OptionType>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, value.toList(),
                required, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Require option to be always provided in command line.
 */
fun <T: Any, TResult> AbstractSingleOption<T, TResult>.required(): SingleOption<T> {
    this as AbstractSingleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        defaultValue?.let {
            printWarning("required() is unneeded, because option with default value is defined.")
        }
        SingleOptionImpl(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName,
                shortName, description, defaultValue,
                true, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Require option to be always provided in command line.
 */
fun <T: Any, TResult: Collection<T>, OptionType: MultipleOptionType>
        MultipleOption<T, TResult, OptionType>.required(): MultipleOption<T, List<T>, OptionType> {
    this as MultipleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, TResult>).descriptor as OptionDescriptor) {
        if (required) {
            printWarning("required() is unneeded, because option with default value is defined.")
        }
        MultipleOptionImpl<T, OptionType>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                true, multiple, delimiter, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow provide several options using [delimiter].
 *
 * @param delimiterValue delimiter used to separate string value to option values.
 */
fun <T : Any, TResult> AbstractSingleOption<T, TResult>.delimiter(delimiterValue: String): MultipleOption<T, List<T>, DelimitedOption> {
    this as AbstractSingleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, T>).descriptor as OptionDescriptor) {
        MultipleOptionImpl<T, DelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, listOfNotNull(defaultValue),
                required, multiple, delimiterValue, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}

/**
 * Allow provide several options using [delimiter].
 *
 * @param delimiterValue delimiter used to separate string value to option values.
 */
fun <T : Any, TResult: Collection<T>> MultipleOption<T, TResult, RepeatedOption>.delimiter(delimiterValue: String): MultipleOption<T, List<T>, RepeatedDelimitedOption> {
    this as MultipleOptionImpl
    val newOption = with((cliElement as ParsingValue<T, Collection<T>>).descriptor as OptionDescriptor) {
        MultipleOptionImpl<T, RepeatedDelimitedOption>(OptionDescriptor(optionFullFormPrefix, optionShortFromPrefix, type, fullName, shortName,
                description, defaultValue?.toList() ?: listOf(),
                required, multiple, delimiterValue, deprecatedWarning), owner)
    }
    owner.entity = newOption
    return newOption
}