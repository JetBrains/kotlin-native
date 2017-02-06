package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.RuntimeAware
import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.backend.konan.ir.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.getLLVMType
import org.jetbrains.kotlin.backend.konan.llvm.isObjectType
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

val DEBUG = false

// Roles in which particular object reference is being used. Lifetime is computed from
// all roles reference.
internal enum class Role {
    // If reference is created as call result.
    CALL_RESULT,
    // If reference is created as allocation call result.
    ALLOC_RESULT,
    // If reference is being returned.
    RETURN_VALUE,
    // If reference is being thrown.
    THROW_VALUE,
    // If reference's field is being read.
    FIELD_READ,
    // If reference's field is being written to.
    FIELD_WRITTEN,
    // If reference is being read from the field.
    READ_FROM_FIELD,
    // If reference is being written to the field.
    WRITTEN_TO_FIELD,
    // If reference is being read from the gloabl.
    READ_FROM_GLOBAL,
    // If reference is being written to the global.
    WRITTEN_TO_GLOBAL,
    // Outgoing call argument.
    CALL_ARGUMENT
}

internal class RoleInfoEntry(val data: Any? = null)

internal open class RoleInfo {
    val entries = mutableListOf<RoleInfoEntry>()
    open fun add(entry: RoleInfoEntry) = entries.add(entry)
}

private fun RuntimeAware.isInteresting(type: KotlinType) : Boolean =
        !type.isUnit() && !type.isNothing() && isObjectType(type)

internal class Roles {
    val data = HashMap<Role, RoleInfo>()

    fun add(role: Role, info: RoleInfoEntry?) {
        val entry = data.getOrPut(role, { RoleInfo() })
        if (info != null) entry.add(info)
    }

    fun add(roles: Roles) {
        roles.data.forEach { role, info ->
            info.entries.forEach { entry ->
                add(role, entry)
            }
        }
    }

    override fun toString() : String {
        val builder = StringBuilder()
        data.forEach { t, u ->
            builder.append(t.name)
            builder.append(": ")
            builder.append(u.entries.joinToString(", "))
            builder.append("\n")
        }
        return builder.toString()
    }
}

internal class VarValues {
    val elementData = HashMap<ValueDescriptor, MutableSet<IrElement>>()

    fun addEmpty(variable: ValueDescriptor) =
            elementData.getOrPut(variable, { mutableSetOf<IrElement>() })
    fun add(variable: ValueDescriptor, element: IrElement) =
            elementData.get(variable)?.add(element)
    fun add(variable: ValueDescriptor, elements: Set<IrElement>) =
            elementData.get(variable)?.addAll(elements)
    fun get(variable: ValueDescriptor) : Set<IrElement>? =
            elementData[variable]

    fun computeClosure() {
        elementData.forEach { key, _ ->
            add(key, computeValueClosure(key))
        }
    }

    // Computes closure of all possible values for given variable.
    fun computeValueClosure(value: ValueDescriptor): Set<IrElement> {
        val result = mutableSetOf<IrElement>()
        val workset = mutableSetOf<ValueDescriptor>(value)
        val seen = mutableSetOf<IrGetValue>()
        while (!workset.isEmpty()) {
            val value = workset.first()
            workset -= value
            val elements = elementData[value] ?: continue
            for (element in elements) {
                if (element is IrGetValue) {
                    if (!seen.contains(element)) {
                        seen.add(element)
                        workset.add(element.descriptor)
                    }
                } else {
                    result.add(element)
                }
            }
        }
        return result
    }
}

internal fun rolesToLifetime(roles: Roles) : Lifetime {
    val roleSet = roles.data.keys.toSet()
    // If reference is stored to global or generic field - it must be global.
    if (roleSet.contains(Role.WRITTEN_TO_GLOBAL) || roleSet.contains(Role.WRITTEN_TO_FIELD))
            return Lifetime.GLOBAL
    // If we pass it to unknown methods or throw - it must be global.
    if (roleSet.contains(Role.CALL_ARGUMENT) || roleSet.contains(Role.THROW_VALUE))
        return Lifetime.GLOBAL
    // If reference is only obtained as call result and never used - it can be local.
    if (roleSet.size == 1 && roleSet.contains(Role.CALL_RESULT))
        // return Lifetime.LOCAL
        throw Error()
    // If reference is obtained as some call result and returned - we can use return.
    if (roleSet.size == 2 && roleSet.contains(Role.CALL_RESULT) && roleSet.contains(Role.RETURN_VALUE))
        return Lifetime.RETURN_VALUE
    // Otherwise, say it is global.
    return Lifetime.GLOBAL
}

internal class ElementFinderVisitor(
        val context: RuntimeAware,
        val elementToRoles: MutableMap<IrElement, Roles>,
        val varValues: VarValues)
    : IrElementVisitorVoid {

    fun isInteresting(element: IrElement) : Boolean {
        return element is IrMemberAccessExpression && context.isInteresting(element.type)
    }

    fun isInteresting(variable: ValueDescriptor) : Boolean {
        return context.isInteresting(variable.type)
    }

    override fun visitElement(element: IrElement) {
        if (isInteresting(element)) {
            elementToRoles[element] = Roles()
        }
        element.acceptChildrenVoid(this)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            elementToRoles.remove(expression.argument)
        }
    }

    override fun visitVariable(expression: IrVariable) {
        if (isInteresting(expression.descriptor))
            varValues.addEmpty(expression.descriptor)
        super.visitVariable(expression)
    }
}

//
// elementToRoles is filled with all possible roles given element can play.
// varValues is filled with all possible elements that could be stored in a variable.
//
internal class RoleAssignerVisitor(
        val elementToRoles: MutableMap<IrElement, Roles>,
        val varValues: VarValues,
        val useVarValues: Boolean) : IrElementVisitorVoid {

    fun assignElementRole(element: IrElement, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues)
            elementToRoles[element]?.add(role, infoEntry)

    }

    fun assignVariableRole(descriptor: ValueDescriptor, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues) return
        // Whenever we see variable use in certain role - we propagate this role
        // to all possible expression this variable can be assigned to.
        val possibleValues = varValues.get(descriptor)
        if (possibleValues != null) {
            for (possibleValue in possibleValues) {
                elementToRoles[possibleValue]?.add(role, infoEntry)
            }
        }
    }

    // Here we handle variable assignment.
    fun assignVariable(variable: VariableDescriptor, value: IrExpression) {
        if (useVarValues) return
        // Sometimes we can assign value to Unit variable (unit4.kt) - we don't care.
        if (value.type.isUnit()) return
        when (value) {
            is IrContainerExpression -> assignVariable(variable, value.statements.last() as IrExpression)
            is IrVararg -> value.elements.forEach { assignVariable(variable, it as IrExpression) }
            is IrWhen -> value.branches.forEach { assignVariable(variable, it.result) }
            is IrMemberAccessExpression -> varValues.add(variable, value)
            is IrGetValue -> varValues.add(variable, value)
            is IrGetField -> varValues.add(variable, value)
            is IrConst<*> -> { }
            is IrTypeOperatorCall -> {
                when (value.operator) {
                    IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                        assignVariable(variable, value.argument)
                    else -> TODO(ir2string(value))
                }
            }
            is IrTry -> listOfNotNull(value.tryResult, value.finallyExpression).forEach {
                assignVariable(variable, it)
            }
            is IrThrow -> { /* Do nothing, error path in an assignment. */ }
            is IrGetObjectValue -> { }
            // TODO: remove once lower will be there.
            is IrStringConcatenation -> value.arguments.forEach { assignVariable(variable, it) }
            // TODO: is it correct?
            is IrReturn -> assignVariable(variable, value.value)
            else -> TODO(ir2string(value))
        }
    }

    // Here we assign a role to expression's value.
    fun assignRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
        if (expression.type.isUnit()) return
        when (expression) {
            is IrContainerExpression -> assignRole(expression.statements.last() as IrExpression, role, infoEntry)
            is IrVararg -> expression.elements.forEach { assignRole(it as IrExpression, role, infoEntry) }
            is IrWhen -> expression.branches.forEach { assignRole(it.result, role, infoEntry) }
            is IrMemberAccessExpression -> assignElementRole(expression, role, infoEntry)
            is IrGetValue -> assignVariableRole(expression.descriptor, role, infoEntry)
            // If field plays certain role - we cannot use this info now.
            is IrGetField -> {}
            // If constant plays certain role - this information is useless.
            is IrConst<*> -> {}
            is IrTypeOperatorCall -> {
                when (expression.operator) {
                    IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                        assignRole(expression.argument, role, infoEntry)
                    // No info from those ones.
                    IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
                    IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {}
                    else -> TODO(ir2string(expression))
                }
            }
            is IrGetObjectValue -> { /* Shall we do anything here? */ }
            is IrThrow -> { /* Shall we do anything here? */}
            is IrExpressionBody -> assignElementRole(expression.expression, role, infoEntry)
            // TODO: remove once lower will be there.
            is IrStringConcatenation -> expression.arguments.forEach { assignRole(it, role, infoEntry) }
            // TODO: is it correct?
            is IrReturn -> assignElementRole(expression.value, role, infoEntry)
            else -> TODO(ir2string(expression))
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        assignRole(expression, Role.CALL_RESULT, RoleInfoEntry(expression))
        for (argument in expression.getArguments()) {
            assignRole(argument.second, Role.CALL_ARGUMENT, RoleInfoEntry(expression))
        }
        super.visitCall(expression)
    }

    override fun visitSetField(expression: IrSetField) {
        assignRole(expression.value, Role.WRITTEN_TO_FIELD, RoleInfoEntry(expression))
        super.visitSetField(expression)
    }

    override fun visitField(declaration: IrField) {
        val initializer = declaration.initializer
        if (initializer != null) {
            assignRole(initializer.expression,
                    Role.WRITTEN_TO_FIELD, RoleInfoEntry(declaration))
        }
        super.visitField(declaration)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        assignVariable(expression.descriptor, expression.value)
        super.visitSetVariable(expression)
    }

    override fun visitVariable(expression: IrVariable) {
        val initializer = expression.initializer
        if (initializer != null) {
            assignVariable(expression.descriptor, initializer)
        }
        super.visitVariable(expression)
    }

    override fun visitReturn(expression: IrReturn) {
        assignRole(expression.value, Role.RETURN_VALUE, RoleInfoEntry(expression))
        super.visitReturn(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        assignRole(expression.value, Role.THROW_VALUE, RoleInfoEntry(expression))
        super.visitThrow(expression)
    }
}
//
// Analysis we're implementing here is as following.
//  * compute roles IR value nodes can play
//  * merge role information with all methods being called
//  * squash set of roles to conservatively estimated lifetime
//
fun computeLifetimes(irModule: IrModuleFragment, context: RuntimeAware,
                     lifetimes: MutableMap<IrElement, Lifetime>) {
    assert(lifetimes.size == 0)

    val elementToRoles = mutableMapOf<IrElement, Roles>()
    val varValues = VarValues()
    irModule.acceptVoid(ElementFinderVisitor(context, elementToRoles, varValues))
    // On this pass, we collect all possible variable values and assign roles
    // to expressions.
    irModule.acceptVoid(RoleAssignerVisitor(elementToRoles, varValues, false))
    // Compute transitive closure of possible values for variables.
    varValues.computeClosure()
    // On this pass, we use possible variable values to assign roles to expression.
    irModule.acceptVoid(RoleAssignerVisitor(elementToRoles, varValues, true))

    elementToRoles.forEach { element, roles ->
        if (DEBUG)
            println("for ${element} roles are ${roles.data.keys.joinToString(", ")}")
        try {
            lifetimes[element] = rolesToLifetime(roles)
        } catch (e: Error) {
            println(ir2string(element))
        }
    }
    if (DEBUG) varValues.elementData.forEach { variable, expressions ->
            println("${variable} could be ${expressions.joinToString(", ")}") }
}