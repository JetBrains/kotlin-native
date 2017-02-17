/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.RuntimeAware
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.konan.llvm.isObjectType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isUnit

val DEBUG = 2

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

    fun remove(role: Role) = data.remove(role)

    fun has(role: Role) : Boolean = data[role] != null

    fun escapesSoft() = has(Role.CALL_ARGUMENT) || escapesHard()

    fun escapesHard()  =
            has(Role.WRITTEN_TO_FIELD) || has(Role.WRITTEN_TO_GLOBAL) || has(Role.THROW_VALUE)

    fun local() = (data.size == 0 ||
            (data.size == 1) && data[Role.CALL_RESULT] != null)

    fun localOrReturn() = local() ||
            ((data.size == 2) && has(Role.CALL_RESULT) && has(Role.RETURN_VALUE))

    fun info(role: Role) : RoleInfo? = data[role]

    override fun toString() : String {
        val builder = StringBuilder()
        data.forEach { t, u ->
            builder.append(t.name)
            builder.append(": ")
            builder.append(u.entries.joinToString(", ") { it.data.toString() })
            builder.append("; ")
        }
        return builder.toString()
    }
}


interface UniqueVariable {
    val owner: CallableDescriptor
}

// Somewhat artificial wrapper around ParameterDescriptor allowing to maintain
// parameter uniqueness ('this' is shared amongst all members of the same class).
internal class FormalParameter(
        val descriptor: ParameterDescriptor, val function: CallableDescriptor) : UniqueVariable {

    override fun hashCode(): Int {
        return descriptor.hashCode() xor function.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is FormalParameter && function == other.function &&
                descriptor == other.descriptor
    }

    override fun toString() = "$descriptor in $function"

    override val owner
        get() = function
}

internal class LocalVariable(
        val descriptor: VariableDescriptor) : UniqueVariable {

    override fun hashCode(): Int {
        return descriptor.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is LocalVariable && descriptor == other.descriptor
    }

    override fun toString() = "$descriptor"

    override val owner
        get() = descriptor.containingDeclaration as CallableDescriptor
}

private fun unique(
        descriptor: ValueDescriptor, function: CallableDescriptor) : UniqueVariable {
    return when (descriptor) {
        is VariableDescriptor -> LocalVariable(descriptor)
        is ParameterDescriptor -> FormalParameter(descriptor, function)
        else -> TODO("unsupported $descriptor")
    }
}

internal class VariableValues {
    val elementData = HashMap<UniqueVariable, MutableSet<IrExpression>>()

    fun addEmpty(variable: UniqueVariable) =
            elementData.getOrPut(variable, { mutableSetOf<IrExpression>() })
    fun add(variable: UniqueVariable, element: IrExpression) =
            elementData.get(variable)?.add(element)
    fun add(variable: UniqueVariable, elements: Set<IrExpression>) =
            elementData.get(variable)?.addAll(elements)
    fun get(variable: UniqueVariable) : Set<IrExpression>? =
            elementData[variable]

    fun computeClosure() {
        elementData.forEach { key, _ ->
            add(key, computeValueClosure(key))
        }
    }

    // Computes closure of all possible values for given variable.
    fun computeValueClosure(value: UniqueVariable): Set<IrExpression> {
        val result = mutableSetOf<IrExpression>()
        val workset = mutableSetOf<UniqueVariable>(value)
        val seen = mutableSetOf<IrGetValue>()
        while (!workset.isEmpty()) {
            val value = workset.first()
            workset -= value
            val elements = elementData[value] ?: continue
            for (element in elements) {
                if (element is IrGetValue) {
                    if (!seen.contains(element)) {
                        seen.add(element)
                        workset.add(unique(element.descriptor, value.owner))
                    }
                } else {
                    result.add(element)
                }
            }
        }
        return result
    }
}

internal class ParameterRoles {
    val elementData = HashMap<FormalParameter, Roles>()
    val analyzed = HashSet<FormalParameter>()

    fun addParameter(parameter: FormalParameter) {
        elementData.getOrPut(parameter) { Roles() }
    }

    fun add(parameter: FormalParameter, role: Role, roleInfoEntry: RoleInfoEntry?) {
        val roles = elementData.getOrPut(parameter, { Roles() });
        roles.add(role, roleInfoEntry)
    }

    fun get(parameter: FormalParameter) : Roles? {
        if (!elementData.contains(parameter))
            return null
        if (!analyzed.contains(parameter))
            analyzeParameterRoles(parameter)
        return elementData[parameter]
    }

    // Returns true if role propagation cannot prove value never escapes.
    fun analyzeParameterRoles(parameter: FormalParameter): Boolean {
        if (analyzed.contains(parameter))
            return elementData[parameter]?.escapesSoft() ?: true
        if (DEBUG > 1) println("analyze roles of $parameter")
        analyzed.add(parameter)
        // If we don't have information for this parameter - it's an argument to function
        // outside of module.
        val roles = elementData[parameter] ?: return true
        if (roles.escapesHard()) return true
        val asArgument = roles.data[Role.CALL_ARGUMENT]
        if (asArgument != null) {
            for (entry in asArgument.entries) {
                val info = entry.data as Pair<FormalParameter, CallableDescriptor>
                if (analyzeParameterRoles(info.first)) return true
            }
            roles.data.remove(Role.CALL_ARGUMENT)
        }
        return false
    }

    fun canEliminateCallArgument(info: RoleInfo): Boolean {
        for (call in info.entries) {
            val entry = call.data as Pair<FormalParameter, CallableDescriptor>
            val argument = entry.first
            val callee = entry.second
            if (callee is FunctionDescriptor) {
                // Virtual dispatch.
                if (callee.isOverridable) return false
                // Calling external function.
                // TODO: add list of pure external functions in stdlib.
                if (callee.isExternal) return false
            }
            if (DEBUG > 1) println("passed as ${argument}")
            val rolesInCallee = get(argument) ?: return false
            if (DEBUG > 1) println("In callee is: $rolesInCallee")
            // TODO: make recursive computation here.
            if (rolesInCallee.escapesSoft()) {
                if (rolesInCallee.escapesHard()) {
                    if (DEBUG > 0) println("escapes hard as $rolesInCallee")
                    return false
                }
                if (analyzeParameterRoles(argument)) return false
            }
        }
        return true
    }

    fun propagateCallArguments(roles: Roles): Boolean {
        val info = roles.info(Role.CALL_ARGUMENT) ?: return false
        if (canEliminateCallArgument(info)) {
            roles.remove(Role.CALL_ARGUMENT)
            return true
        }
        return false
    }

    fun propagateCallArguments(expressionToRoles: MutableMap<IrExpression, Roles>) {
        expressionToRoles.forEach {
            if (propagateCallArguments(it.value) && DEBUG > 0)
                println("unescaped expression ${it.key}")
        }
        elementData.forEach {
            if (propagateCallArguments(it.value) && DEBUG > 0)
                println("unescaped paramater ${it.key}")
        }
    }
}

internal fun rolesToLifetime(roles: Roles) : Lifetime {
   return when {
        // If reference is stored to global or generic field - it must be global.
        roles.escapesHard() -> Lifetime.GLOBAL
        // If reference is only obtained as call result and never used - it can be local.
        roles.local() -> Lifetime.LOCAL
        // If reference is obtained as some call result and returned - we can use return.
        roles.localOrReturn() -> Lifetime.RETURN_VALUE
        // Otherwise, say it is global.
        else -> Lifetime.GLOBAL
    }
}

internal class ElementFinderVisitor(
        val context: RuntimeAware,
        val expressionToRoles: MutableMap<IrExpression, Roles>,
        val variableValues: VariableValues,
        val parameterRoles: ParameterRoles)
    : IrElementVisitorVoid {

    fun isInteresting(element: IrExpression) : Boolean {
        return (element is IrMemberAccessExpression && context.isInteresting(element.type)) ||
                (element is IrGetValue && context.isInteresting(element.type))
    }

    fun isInteresting(variable: ValueDescriptor) : Boolean {
        return context.isInteresting(variable.type)
    }

    override fun visitExpression(expression: IrExpression) {
        if (isInteresting(expression)) {
            expressionToRoles[expression] = Roles()
        }
        super.visitExpression(expression)
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitTypeOperator(expression: IrTypeOperatorCall) {
        super.visitTypeOperator(expression)
        if (expression.operator == IrTypeOperator.IMPLICIT_COERCION_TO_UNIT) {
            println("removing ${expression.argument} due to Unit coercion")
            expressionToRoles.remove(expression.argument)
        }
    }

    override fun visitFunction(declaration: IrFunction) {
        declaration.descriptor.allParameters.forEach {
            if (isInteresting(it))
                parameterRoles.addParameter(FormalParameter(it, declaration.descriptor))
        }
        super.visitFunction(declaration)
    }

    override fun visitVariable(expression: IrVariable) {
        if (isInteresting(expression.descriptor))
            variableValues.addEmpty(LocalVariable(expression.descriptor))
        super.visitVariable(expression)
    }
}

//
// elementToRoles is filled with all possible roles given element can play.
// varValues is filled with all possible elements that could be stored in a variable.
//
internal class RoleAssignerVisitor(
        val expressionRoles: MutableMap<IrExpression, Roles>,
        val variableValues: VariableValues,
        val parameterRoles: ParameterRoles,
        val useVarValues: Boolean) : IrElementVisitorVoid {

    fun assignExpressionRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues)
            expressionRoles[expression]?.add(role, infoEntry)
    }

    fun assignValueRole(value: UniqueVariable, role: Role, infoEntry: RoleInfoEntry?) {
        if (!useVarValues) return
        // Whenever we see variable use in certain role - we propagate this role
        // to all possible expression this variable can be assigned to.
        val possibleValues = variableValues.get(value)
        if (possibleValues != null) {
            for (possibleValue in possibleValues) {
                expressionRoles[possibleValue]?.add(role, infoEntry)
            }
        }
        if (value is ParameterDescriptor) {
            parameterRoles.add(FormalParameter(value, currentFunction!!), role, infoEntry)
        }
    }

    // Here we handle variable assignment.
    fun assignVariable(variable: VariableDescriptor, value: IrExpression) {
        if (useVarValues) return
        // Sometimes we can assign value to Unit variable (unit4.kt) - we don't care.
        if (value.type.isUnit()) return
        when (value) {
            is IrContainerExpression -> assignVariable(variable, value.statements.last() as IrExpression)
            is IrWhen -> value.branches.forEach { assignVariable(variable, it.result) }
            is IrMemberAccessExpression -> variableValues.add(LocalVariable(variable), value)
            is IrGetValue -> variableValues.add(LocalVariable(variable), value)
            is IrGetField -> variableValues.add(LocalVariable(variable), value)
            is IrVararg -> /* Sometimes, we keep vararg till codegen phase (for constant arrays). */
                variableValues.add(LocalVariable(variable), value)
            is IrConst<*> -> { }
            is IrTypeOperatorCall -> {
                when (value.operator) {
                    IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                        assignVariable(variable, value.argument)
                    else -> TODO(ir2string(value))
                }
            }
            is IrTry -> (value.catches.map { it.result } + value.tryResult).forEach {
                assignVariable(variable, it)
            }
            is IrThrow -> { /* Do nothing, error path in an assignment. */ }
            is IrGetObjectValue -> { }
            // TODO: is it correct?
            is IrReturn -> { /* Do nothing, return path in an assignment. */ }
            else -> TODO(ir2string(value))
        }
    }

    // Here we assign a role to expression's value.
    fun assignRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
        if (expression.type.isUnit()) return
        when (expression) {
            is IrContainerExpression -> assignRole(expression.statements.last() as IrExpression, role, infoEntry)
            is IrWhen -> expression.branches.forEach { assignRole(it.result, role, infoEntry) }
            is IrMemberAccessExpression -> assignExpressionRole(expression, role, infoEntry)
            is IrGetValue -> assignValueRole(
                    unique(expression.descriptor, currentFunction!!), role, infoEntry)
            // If field plays certain role - we cannot use this info now.
            is IrGetField -> {}
            is IrVararg -> /* Sometimes, we keep vararg till codegen phase (for constant arrays). */
                assignExpressionRole(expression, role, infoEntry)
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
            is IrTry -> (expression.catches.map { it.result } + expression.tryResult).forEach {
                assignRole(it, role, infoEntry)
            }
            is IrThrow -> { /* Do nothing, error path in an assignment. */ }
            is IrGetObjectValue -> { /* Shall we do anything here? */ }
            // TODO: is it correct, especially for inlines?
            is IrReturn -> { /* Do nothing, return path in an assignment. */ }
            else -> TODO(ir2string(expression))
        }
    }

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        assignRole(expression, Role.CALL_RESULT, RoleInfoEntry(expression))
        for (argument in expression.getArguments()) {
            assignRole(argument.second, Role.CALL_ARGUMENT,
                    RoleInfoEntry(
                            FormalParameter(
                                    argument.first, expression.descriptor) to expression.descriptor))
        }
        super.visitCall(expression)
    }

    override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
        for (argument in expression.getArguments()) {
            assignRole(argument.second, Role.CALL_ARGUMENT,
                    RoleInfoEntry(
                            FormalParameter(
                                    argument.first, expression.descriptor) to expression.descriptor))
        }
        super.visitDelegatingConstructorCall(expression)
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

    override fun visitFunction(declaration: IrFunction) {
        this.currentFunction = declaration.descriptor
        super.visitFunction(declaration)
    }

    override fun visitConstructor(declaration: IrConstructor) {
        this.currentFunction = declaration.descriptor
        super.visitConstructor(declaration)
    }

    override fun visitReturn(expression: IrReturn) {
        assignRole(expression.value, Role.RETURN_VALUE, RoleInfoEntry(expression))
        super.visitReturn(expression)
    }

    override fun visitThrow(expression: IrThrow) {
        assignRole(expression.value, Role.THROW_VALUE, RoleInfoEntry(expression))
        super.visitThrow(expression)
    }

    override fun visitVararg(expression: IrVararg) {
        expression.elements.forEach {
            when (it) {
                is IrExpression ->
                    assignExpressionRole(it, Role.WRITTEN_TO_FIELD, RoleInfoEntry(expression))
                is IrSpreadElement ->
                    assignExpressionRole(it.expression, Role.WRITTEN_TO_FIELD, RoleInfoEntry(expression))
                else -> TODO("Unsupported vararg element")
            }
        }
        super.visitVararg(expression)
    }

    private var currentFunction: CallableDescriptor? = null
}

internal class ContextFinderVisitor(val needle: IrElement) : IrElementVisitorVoid {
    val stack = mutableListOf<IrElement>()
    var found = false

    override fun visitElement(element: IrElement) {
        if (found) return
        stack.add(element)
        if (element == needle) {
            found = true
        }
        element.acceptChildrenVoid(this)
        if (found) return
        stack.removeAt(stack.size - 1)
    }

    override fun visitModuleFragment(declaration: IrModuleFragment) {
        declaration.acceptChildrenVoid(this)
    }
}

private fun findContext(irModule: IrModuleFragment, element: IrElement) : String {
    val visitor = ContextFinderVisitor(element)
    irModule.acceptVoid(visitor)
    val result = StringBuilder()
    if (visitor.found) {
        visitor.stack.forEach {
            result.append(ir2string(it))
            result.append(" -> ")
        }
    } else {
        result.append("<not found>")
    }
    return result.toString()
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

    val expressionToRoles = mutableMapOf<IrExpression, Roles>()
    val variableValues = VariableValues()
    val parameterRoles = ParameterRoles()
    irModule.acceptVoid(ElementFinderVisitor(
            context, expressionToRoles, variableValues, parameterRoles))
    // On this pass, we collect all possible variable values and assign roles
    // to expressions.
    irModule.acceptVoid(RoleAssignerVisitor(
            expressionToRoles, variableValues, parameterRoles, false))
    // Compute transitive closure of possible values for variables.
    variableValues.computeClosure()
    // On this pass, we use possible variable values to assign roles to expression.
    irModule.acceptVoid(RoleAssignerVisitor(
            expressionToRoles, variableValues, parameterRoles, true))
    parameterRoles.propagateCallArguments(expressionToRoles)

    expressionToRoles.forEach { expression, roles ->
        if (DEBUG > 1)
            println("for ${ir2string(expression)} roles are ${roles.data.keys.joinToString(", ")}")
        var role = rolesToLifetime(roles)
        if (role == Lifetime.LOCAL) {
            if (DEBUG > 0 && expression !is IrGetValue)
                println("arena alloc for ${ir2string(expression)} in ${findContext(irModule, expression)}")
        }
        lifetimes[expression] = role
    }
    if (DEBUG > 1) {
        variableValues.elementData.forEach { variable, expressions ->
            println("variable $variable could be ${expressions.joinToString(", "){ ir2string(it)}}") }
        parameterRoles.elementData.forEach { parameter, roles ->
            println("parameter $parameter could be $roles") }
    }
}