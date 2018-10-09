package org.jetbrains.kotlin.backend.konan.lower.loops

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.irasdescriptors.fqNameSafe
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.name.FqName

internal class IrMatcher(val context: Context) {

    private val parameterRestrictins = mutableListOf<Pair<Int, (IrValueParameter) -> Boolean>>()
    private val extensionReceiverRestrictions = mutableListOf<(IrValueParameter) -> Boolean>()

    private val nameRestrictions = mutableListOf<(FqName) -> Boolean>()

    private val paramCountRestrictions = mutableListOf<(Int) -> Boolean>()

    fun addNameRestriction(restriction: (FqName) -> Boolean) {
        nameRestrictions += restriction
    }

    fun addParameterRestriction(idx: Int, restriction: (IrValueParameter) -> Boolean) {
        parameterRestrictins += idx to restriction
    }

    fun addExtensionReceiverRestriction(restriction: (IrValueParameter) -> Boolean) {
        extensionReceiverRestrictions += restriction
    }

    fun addParamCountRestrictions(restriction: (Int) -> Boolean) {
        paramCountRestrictions += restriction
    }

    fun match(function: IrFunction): Boolean {
        val params = function.valueParameters

        nameRestrictions.forEach {
            if (!it(function.fqNameSafe)) {
                return false
            }
        }

        paramCountRestrictions.forEach {
            if (!it(params.size)) {
                return false
            }
        }

        parameterRestrictins.forEach { (idx, resrt) ->
            if (params.size <= idx || !resrt(params[idx])){
                return false
            }
        }
        if (function.extensionReceiverParameter != null) {
            extensionReceiverRestrictions.forEach {
                if (!it(function.extensionReceiverParameter!!)) {
                    return false
                }
            }
        }
        return true
    }
}