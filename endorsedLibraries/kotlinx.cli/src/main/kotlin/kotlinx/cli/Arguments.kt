/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package kotlinx.cli

import kotlin.reflect.KProperty

/**
 * Interface for CLI entity - argument/option.
 */
interface CLIEntity<T : Any, TResult> {
    /**
     * Argument value.
     */
    val value: ArgumentValueDelegate<TResult>
    /**
     * Origin of argument value.
     */
    val valueOrigin: ArgParser.ValueOrigin
        get() = (value as ParsingValue<*, *>).valueOrigin

    operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<TResult>
}

/**
 * Command line entity.
 *
 * @property owner parser which owns current entity.
 */
internal abstract class CLIEntityImpl<T : Any, TResult>(val owner: ArgParser): CLIEntity<T, TResult> {
    /**
     * Unique id.
     */
    var id: Int? = null
    /**
     * CLI element value.
     */
    internal lateinit var cliElement: ArgumentValueDelegate<TResult>
    /**
     * Wrapper  for element - read only property.
     * Needed to close set of variable [cliElement].
     */
    override val value: ArgumentValueDelegate<TResult>
        get() = cliElement
}

/**
 * Argument instance interface.
 */
interface Argument<T : Any, TResult>: CLIEntity<T, TResult>
/**
 * Common single argument instance interface.
 */
interface AbstractSingleArgument<T : Any, TResult> : Argument<T, TResult>
/**
 * Argument with single non-nullable value.
 */
interface SingleArgument<T : Any> : AbstractSingleArgument<T, T>
/**
 * Argument with single nullable value.
 */
interface SingleNullableArgument<T : Any> : AbstractSingleArgument<T, T?>
/**
 * Argument with multiple values.
 */
interface MultipleArgument<T : Any, TResult : Collection<T>> : Argument<T, TResult>

internal abstract class ArgumentImpl<T : Any, TResult>(owner: ArgParser): CLIEntityImpl<T, TResult>(owner), Argument<T, TResult> {
    fun replaceArgument(newArgument: ArgumentImpl<*, *>) {
        newArgument.id = id
        owner.addArgument(newArgument)
    }
}

internal abstract class AbstractSingleArgumentImpl<T: Any, TResult>(owner: ArgParser): ArgumentImpl<T, TResult>(owner),
        AbstractSingleArgument<T, TResult> {
    /**
     * Check descriptor for this kind of argument.
     */
    fun checkDescriptor(descriptor: ArgDescriptor<*, *>) {
        if (descriptor.number == null || descriptor.number > 1) {
            error("Argument with single value can't be initialized with descriptor for multiple values.")
        }
    }
}

internal class SingleArgumentImpl<T : Any>(descriptor: ArgDescriptor<T, T>, owner: ArgParser):
        AbstractSingleArgumentImpl<T, T>(owner),
        SingleArgument<T> {
    init {
        checkDescriptor(descriptor)
        cliElement = ArgumentSingleValue(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T> {
        (cliElement as ParsingValue<T, T>).provideName(prop.name)
        return cliElement
    }
}

internal class SingleNullableArgumentImpl<T : Any>(descriptor: ArgDescriptor<T, T>, owner: ArgParser):
        AbstractSingleArgumentImpl<T, T?>(owner),
        SingleNullableArgument<T> {
    init {
        checkDescriptor(descriptor)
        cliElement = ArgumentSingleNullableValue(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<T?> {
        (cliElement as ParsingValue<T, T>).provideName(prop.name)
        return cliElement
    }
}

internal class MultipleArgumentImpl<T : Any>(descriptor: ArgDescriptor<T, MutableList<T>>, owner: ArgParser):
        ArgumentImpl<T, List<T>>(owner), MultipleArgument<T, List<T>> {
    init {
        if (descriptor.number != null && descriptor.number == 1) {
            error("Argument with multiple values can't be initialized with descriptor for single one.")
        }
        cliElement = ArgumentMultipleValues(descriptor)
    }

    override operator fun provideDelegate(thisRef: Any?, prop: KProperty<*>): ArgumentValueDelegate<List<T>> {
        (cliElement as ParsingValue<T, List<T>>).provideName(prop.name)
        return cliElement
    }
}

/**
 * Allow argument have several values.
 *
 * @param number number of arguments are expected. In case of null value any number of arguments can be set.
 */
fun <T : Any, TResult> AbstractSingleArgument<T, TResult>.number(value: Int? = null): MultipleArgument<T, List<T>> {
    this as AbstractSingleArgumentImpl
    if (value != null && value == 1) {
        error("number() modifier with value 1 is unavailable. It's already set to 1.")
    }
    val newArgument = with((cliElement as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        MultipleArgumentImpl(ArgDescriptor(type, fullName, value, description, defaultValue?.let { mutableListOf(it) } ?: mutableListOf(),
                required, deprecatedWarning), owner)
    }
    replaceArgument(newArgument)
    return newArgument
}

/**
 * Set default value for argument.
 *
 * @param value default value.
 */
fun <T: Any, TResult> AbstractSingleArgument<T, TResult>.default(value: T): SingleArgument<T> {
    this as AbstractSingleArgumentImpl
    val newArgument = with((cliElement as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        if (required) {
            printWarning("You can use optional(), because option with default value is defined.")
        }
        SingleArgumentImpl(ArgDescriptor(type, fullName, number, description, value, required, deprecatedWarning), owner)
    }
    replaceArgument(newArgument)
    return newArgument
}

/**
 * Set default value for argument.
 *
 * @param value default value.
 */
fun <T: Any, TResult: Collection<T>> MultipleArgument<T, TResult>.default(value: TResult): MultipleArgument<T, List<T>> {
    this as MultipleArgumentImpl
    val newArgument = with((cliElement as ParsingValue<T, TResult>).descriptor as ArgDescriptor) {
        if (required) {
            printWarning("You can use optional(), because option with default value is defined.")
        }
        MultipleArgumentImpl(ArgDescriptor(type, fullName, number, description, value.toMutableList(),
                required, deprecatedWarning), owner)
    }
    replaceArgument(newArgument)
    return newArgument
}

/**
 * Allow argument be unprovided in command line.
 */
fun <T: Any, TResult> AbstractSingleArgument<T, TResult>.optional(): SingleNullableArgument<T> {
    this as AbstractSingleArgumentImpl
    val newArgument = with((cliElement as ParsingValue<T, T>).descriptor as ArgDescriptor) {
        SingleNullableArgumentImpl(ArgDescriptor(type, fullName, number, description, defaultValue,
                false, deprecatedWarning), owner)
    }
    replaceArgument(newArgument)
    return newArgument
}

/**
 * Allow argument be unprovided in command line.
 */
fun <T: Any, TResult: Collection<T>> MultipleArgument<T, TResult>.optional(): MultipleArgument<T, List<T>> {
    this as MultipleArgumentImpl
    val newArgument = with((cliElement as ParsingValue<T, TResult>).descriptor as ArgDescriptor) {
        MultipleArgumentImpl(ArgDescriptor(type, fullName, number, description,
                defaultValue?.toMutableList() ?: mutableListOf(), false, deprecatedWarning), owner)
    }
    replaceArgument(newArgument)
    return newArgument
}