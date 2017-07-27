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

import org.jetbrains.kotlin.backend.common.IrElementVisitorVoidWithContext
import org.jetbrains.kotlin.backend.common.descriptors.allParameters
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.backend.common.ir.ir2string
import org.jetbrains.kotlin.backend.common.ir.ir2stringWhole
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.ir.IrReturnableBlock
import org.jetbrains.kotlin.backend.konan.ir.IrSuspendableExpression
import org.jetbrains.kotlin.backend.konan.ir.IrSuspensionPoint
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.*
import java.nio.charset.StandardCharsets

internal object EscapeAnalysis {

    private val DEBUG = 0

    // Roles in which particular object reference is being used. Lifetime is computed from all roles reference.
    private enum class Role {
        // If reference is being returned.
        RETURN_VALUE,
        // If reference is being thrown.
        THROW_VALUE,
        // If reference's field is being written to.
        FIELD_WRITTEN,
        // If reference is being written to the global.
        WRITTEN_TO_GLOBAL
    }

    private class RoleInfoEntry(val data: Any? = null)

    private open class RoleInfo {
        val entries = mutableListOf<RoleInfoEntry>()

        open fun add(entry: RoleInfoEntry) = entries.add(entry)
    }

    private fun RuntimeAware.isInteresting(type: KotlinType?): Boolean =
            type != null && !type.isUnit() && !type.isNothing()// && isObjectType(type) // TODO: primitive types can be boxed.

    private class Roles {
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

        fun has(role: Role): Boolean = data[role] != null

        fun escapes() = has(Role.WRITTEN_TO_GLOBAL) || has(Role.THROW_VALUE)
    }

    private class VariableValues {
        val elementData = HashMap<VariableDescriptor, MutableSet<IrExpression>>()

        fun addEmpty(variable: VariableDescriptor) =
                elementData.getOrPut(variable, { mutableSetOf<IrExpression>() })

        fun add(variable: VariableDescriptor, element: IrExpression) =
                elementData.get(variable)?.add(element)

        fun add(variable: VariableDescriptor, elements: Set<IrExpression>) =
                elementData.get(variable)?.addAll(elements)

        fun get(variable: VariableDescriptor): Set<IrExpression>? =
                elementData[variable]

        fun computeClosure() {
            elementData.forEach { key, _ ->
                add(key, computeValueClosure(key))
            }
        }

        // Computes closure of all possible values for given variable.
        private fun computeValueClosure(value: VariableDescriptor): Set<IrExpression> {
            val result = mutableSetOf<IrExpression>()
            val seen = mutableSetOf<VariableDescriptor>()
            dfs(value, seen, result)
            return result
        }

        private fun dfs(value: VariableDescriptor, seen: MutableSet<VariableDescriptor>, result: MutableSet<IrExpression>) {
            seen += value
            val elements = elementData[value]
                    ?: return
            for (element in elements) {
                if (element !is IrGetValue)
                    result += element
                else {
                    val descriptor = element.descriptor
                    if (descriptor is VariableDescriptor && !seen.contains(descriptor))
                        dfs(descriptor, seen, result)
                }
            }
        }
    }

    private class ParameterRoles {
        val elementData = HashMap<ParameterDescriptor, Roles>()

        fun addParameter(parameter: ParameterDescriptor) {
            elementData.getOrPut(parameter) { Roles() }
        }

        fun add(parameter: ParameterDescriptor, role: Role, roleInfoEntry: RoleInfoEntry?) {
            val roles = elementData.getOrPut(parameter, { Roles() })
            roles.add(role, roleInfoEntry)
        }
    }

    private class ExpressionValuesExtractor(val returnableBlockValues: Map<IrReturnableBlock, List<IrExpression>>,
                                            val suspendableExpressionValues: Map<IrSuspendableExpression, List<IrSuspensionPoint>>) {

        fun forEachValue(expression: IrExpression, block: (IrExpression) -> Unit) {
            if (expression.type.isUnit() || expression.type.isNothing()) return
            when (expression) {
                is IrReturnableBlock -> returnableBlockValues[expression]!!.forEach { forEachValue(it, block) }

                is IrSuspendableExpression ->
                    (suspendableExpressionValues[expression]!! + expression.result).forEach { forEachValue(it, block) }

                is IrSuspensionPoint -> {
                    forEachValue(expression.result, block)
                    forEachValue(expression.resumeResult, block)
                }

                is IrContainerExpression -> forEachValue(expression.statements.last() as IrExpression, block)

                is IrWhen -> expression.branches.forEach { forEachValue(it.result, block) }

                is IrMemberAccessExpression -> block(expression)

                is IrGetValue -> block(expression)

                is IrGetField -> block(expression)

                is IrVararg -> /* Sometimes, we keep vararg till codegen phase (for constant arrays). */
                    block(expression)

            // If constant plays certain role - this information is useless.
                is IrConst<*> -> {
                }

                is IrTypeOperatorCall -> {
                    when (expression.operator) {
                        IrTypeOperator.IMPLICIT_CAST, IrTypeOperator.CAST ->
                            forEachValue(expression.argument, block)
                    // No info from those ones.
                        IrTypeOperator.INSTANCEOF, IrTypeOperator.NOT_INSTANCEOF,
                        IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                        }
                        else -> TODO(ir2string(expression))
                    }
                }

                is IrTry -> {
                    forEachValue(expression.tryResult, block)
                    expression.catches.forEach { forEachValue(it.result, block) }
                }

                is IrGetObjectValue -> { /* Shall we do anything here? */
                }

                else -> TODO(ir2string(expression))
            }
        }
    }

    private fun ExpressionValuesExtractor.extractNodesUsingVariableValues(expression: IrExpression,
                                                                          variableValues: VariableValues?): List<Any> {
        val values = mutableListOf<Any>()
        forEachValue(expression) {
            if (it !is IrGetValue)
                values += it
            else {
                val descriptor = it.descriptor
                if (descriptor is ParameterDescriptor)
                    values += it.descriptor
                else {
                    descriptor as VariableDescriptor
                    variableValues?.get(descriptor)?.forEach {
                        if (it !is IrGetValue)
                            values += it
                        else if (it.descriptor is ParameterDescriptor) {
                            values += it.descriptor
                        }
                    }
                }
            }
        }
        return values
    }

    private class FunctionAnalysisResult(val function: IrFunction,
                                         val expressionToRoles: Map<IrExpression, Roles>,
                                         val variableValues: VariableValues,
                                         val parameterRoles: ParameterRoles)

    private class IntraproceduralAnalysisResult(val functionAnalysisResults: Map<FunctionDescriptor, FunctionAnalysisResult>,
                                                val expressionValuesExtractor: ExpressionValuesExtractor)

    private class IntraproceduralAnalysis(val context: RuntimeAware) {

        // Possible values of a returnable block.
        private val returnableBlockValues = mutableMapOf<IrReturnableBlock, MutableList<IrExpression>>()

        // All suspension points within specified suspendable expression.
        private val suspendableExpressionValues = mutableMapOf<IrSuspendableExpression, MutableList<IrSuspensionPoint>>()

        private val expressionValuesExtractor = ExpressionValuesExtractor(returnableBlockValues, suspendableExpressionValues)

        private fun isInteresting(expression: IrExpression) =
                (expression is IrMemberAccessExpression && context.isInteresting(expression.type))
                        || (expression is IrGetValue && context.isInteresting(expression.type))
                        || (expression is IrGetField && context.isInteresting(expression.type))

        private fun isInteresting(variable: ValueDescriptor) =
                context.isInteresting(variable.type)

        fun analyze(irModule: IrModuleFragment): IntraproceduralAnalysisResult {
            val result = mutableMapOf<FunctionDescriptor, FunctionAnalysisResult>()
            irModule.accept(object : IrElementVisitorVoid {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    val body = declaration.body
                            ?: return

                    if (DEBUG > 1)
                        println("Analysing function ${declaration.descriptor}")

                    // Find all interesting expressions, variables and functions.
                    val parameterRoles = ParameterRoles()
                    declaration.descriptor.allParameters.forEach {
                        if (isInteresting(it))
                            parameterRoles.addParameter(it)
                    }
                    val visitor = ElementFinderVisitor()
                    declaration.acceptVoid(visitor)
                    val functionAnalysisResult = FunctionAnalysisResult(declaration, visitor.expressionToRoles,
                            visitor.variableValues, parameterRoles)
                    result.put(declaration.descriptor, functionAnalysisResult)

                    // On this pass, we collect all possible variable values and assign roles to expressions.
                    body.acceptVoid(RoleAssignerVisitor(declaration.descriptor, functionAnalysisResult, false))

                    if (DEBUG > 1) {
                        println("FIRST PHASE")
                        functionAnalysisResult.parameterRoles.elementData.forEach { t, u ->
                            println("PARAM $t: $u")
                        }
                        functionAnalysisResult.variableValues.elementData.forEach { t, u ->
                            println("VAR $t:")
                            u.forEach {
                                println("    ${ir2stringWhole(it)}")
                            }
                        }
                        functionAnalysisResult.expressionToRoles.forEach { t, u ->
                            println("EXP ${ir2stringWhole(t)}")
                            println("    :$u")
                        }
                    }

                    // Compute transitive closure of possible values for variables.
                    functionAnalysisResult.variableValues.computeClosure()

                    if (DEBUG > 1) {
                        println("SECOND PHASE")
                        functionAnalysisResult.parameterRoles.elementData.forEach { t, u ->
                            println("PARAM $t: $u")
                        }
                        functionAnalysisResult.variableValues.elementData.forEach { t, u ->
                            println("VAR $t:")
                            u.forEach {
                                println("    ${ir2stringWhole(it)}")
                            }
                        }
                    }

                    // On this pass, we use possible variable values to assign roles to expressions.
                    body.acceptVoid(RoleAssignerVisitor(declaration.descriptor, functionAnalysisResult, true))

                    if (DEBUG > 1) {
                        println("THIRD PHASE")
                        functionAnalysisResult.parameterRoles.elementData.forEach { t, u ->
                            println("PARAM $t: $u")
                        }
                        functionAnalysisResult.expressionToRoles.forEach { t, u ->
                            println("EXP ${ir2stringWhole(t)}")
                            println("    :$u")
                        }
                    }

                }
            }, data = null)

            return IntraproceduralAnalysisResult(result, expressionValuesExtractor)
        }

        private inner class ElementFinderVisitor : IrElementVisitorVoid {

            val expressionToRoles = mutableMapOf<IrExpression, Roles>()
            val variableValues = VariableValues()

            private val returnableBlocks = mutableMapOf<FunctionDescriptor, IrReturnableBlock>()
            private val suspendableExpressionStack = mutableListOf<IrSuspendableExpression>()

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitExpression(expression: IrExpression) {
                if (isInteresting(expression)) {
                    expressionToRoles[expression] = Roles()
                }
                if (expression is IrReturnableBlock) {
                    returnableBlocks.put(expression.descriptor, expression)
                    returnableBlockValues.put(expression, mutableListOf())
                }
                if (expression is IrSuspendableExpression) {
                    suspendableExpressionStack.push(expression)
                    suspendableExpressionValues.put(expression, mutableListOf())
                }
                if (expression is IrSuspensionPoint)
                    suspendableExpressionValues[suspendableExpressionStack.peek()!!]!!.add(expression)
                super.visitExpression(expression)
                if (expression is IrReturnableBlock)
                    returnableBlocks.remove(expression.descriptor)
                if (expression is IrSuspendableExpression)
                    suspendableExpressionStack.pop()
            }

            override fun visitReturn(expression: IrReturn) {
                val returnableBlock = returnableBlocks[expression.returnTarget]
                if (returnableBlock != null) {
                    returnableBlockValues[returnableBlock]!!.add(expression.value)
                }
                super.visitReturn(expression)
            }

            override fun visitVariable(declaration: IrVariable) {
                if (isInteresting(declaration.descriptor))
                    variableValues.addEmpty(declaration.descriptor)
                super.visitVariable(declaration)
            }
        }

        //
        // elementToRoles is filled with all possible roles given element can play.
        // varValues is filled with all possible elements that could be stored in a variable.
        //
        private inner class RoleAssignerVisitor(val functionDescriptor: FunctionDescriptor,
                                                functionAnalysisResult: FunctionAnalysisResult,
                                                val useVarValues: Boolean) : IrElementVisitorVoid {

            val expressionRoles = functionAnalysisResult.expressionToRoles
            val variableValues = functionAnalysisResult.variableValues
            val parameterRoles = functionAnalysisResult.parameterRoles

            private fun assignExpressionRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
                if (!useVarValues)
                    expressionRoles[expression]?.add(role, infoEntry)
            }

            private fun assignValueRole(value: ValueDescriptor, role: Role, infoEntry: RoleInfoEntry?) {
                if (!useVarValues) return
                // Whenever we see variable use in certain role - we propagate this role
                // to all possible expressions this variable can be assigned from.
                when (value) {
                    is ParameterDescriptor -> {
                        if (isInteresting(value))
                            parameterRoles.add(value, role, infoEntry)
                    }

                    is VariableDescriptor -> {
                        val possibleValues = variableValues.get(value)
                        if (possibleValues != null) {
                            for (possibleValue in possibleValues) {
                                expressionRoles[possibleValue]?.add(role, infoEntry)
                            }
                        }
                    }
                }
            }

            // Here we handle variable assignment.
            private fun assignVariable(variable: VariableDescriptor, value: IrExpression) {
                if (useVarValues) return
                expressionValuesExtractor.forEachValue(value) {
                    variableValues.add(variable, it)
                }
            }

            // Here we assign a role to expression's value.
            private fun assignRole(expression: IrExpression, role: Role, infoEntry: RoleInfoEntry?) {
                expressionValuesExtractor.forEachValue(expression) {
                    if (it is IrGetValue)
                        assignValueRole(it.descriptor, role, infoEntry)
                    else assignExpressionRole(it, role, infoEntry)
                }
            }

            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitSetField(expression: IrSetField) {
                if (expression.receiver == null)
                    assignRole(expression.value, Role.WRITTEN_TO_GLOBAL, RoleInfoEntry(expression))
                else {
                    val nodes = expressionValuesExtractor.extractNodesUsingVariableValues(
                            expression     = expression.value,
                            variableValues = if (useVarValues) variableValues else null
                    )
                    nodes.forEach { assignRole(expression.receiver!!, Role.FIELD_WRITTEN, RoleInfoEntry(it)) }
                }
                super.visitSetField(expression)
            }

            override fun visitGetField(expression: IrGetField) {
                expression.receiver?.let {
                    // Receiver holds reference to all its fields.
                    assignRole(it, Role.FIELD_WRITTEN, RoleInfoEntry(expression))

                    /*
                     * The opposite (a field points to its receiver) is also kind of true.
                     * Here is an example why we need these edges:
                     *
                     * class B
                     * class A { val b = B() }
                     * fun foo(): B {
                     *     val a = A() <- here [a] is created and so does [a.b], therefore they have the same lifetime.
                     *     return a.b  <- a.b escapes to return value. If there were no edge from [a.b] to [a],
                     *                    then [a] would've been considered local and since [a.b] has the same lifetime as [a],
                     *                    [a.b] would be local as well.
                     * }
                     *
                     */
                    val nodes = expressionValuesExtractor.extractNodesUsingVariableValues(
                            expression     = it,
                            variableValues = if (useVarValues) variableValues else null
                    )
                    nodes.forEach { assignRole(expression, Role.FIELD_WRITTEN, RoleInfoEntry(it)) }
                }
                super.visitGetField(expression)
            }

            override fun visitField(declaration: IrField) {
                val initializer = declaration.initializer
                if (initializer != null) {
                    assert(declaration.descriptor.dispatchReceiverParameter == null,
                            { "Instance field initializers should've been lowered" })
                    assignRole(initializer.expression, Role.WRITTEN_TO_GLOBAL, RoleInfoEntry(declaration))
                }
                super.visitField(declaration)
            }

            override fun visitSetVariable(expression: IrSetVariable) {
                assignVariable(expression.descriptor, expression.value)
                super.visitSetVariable(expression)
            }

            override fun visitVariable(declaration: IrVariable) {
                declaration.initializer?.let { assignVariable(declaration.descriptor, it) }
                super.visitVariable(declaration)
            }

            // TODO: hack to overcome bad code in InlineConstructorsTransformation.
            private val FQ_NAME_INLINE_CONSTRUCTOR = FqName("konan.internal.InlineConstructor")

            override fun visitReturn(expression: IrReturn) {
                if (expression.returnTarget == functionDescriptor // Non-local return.
                        && !functionDescriptor.annotations.hasAnnotation(FQ_NAME_INLINE_CONSTRUCTOR)) // Not inline constructor.
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
                        is IrExpression -> {
                            val nodes = expressionValuesExtractor.extractNodesUsingVariableValues(
                                    expression     = it,
                                    variableValues = if (useVarValues) variableValues else null
                            )
                            nodes.forEach { assignRole(expression, Role.FIELD_WRITTEN, RoleInfoEntry(it)) }
                        }
                        is IrSpreadElement -> {
                            val nodes = expressionValuesExtractor.extractNodesUsingVariableValues(
                                    expression     = it.expression,
                                    variableValues = if (useVarValues) variableValues else null
                            )
                            nodes.forEach { assignRole(expression, Role.FIELD_WRITTEN, RoleInfoEntry(it)) }
                        }
                        else -> TODO("Unsupported vararg element")
                    }
                }
                super.visitVararg(expression)
            }
        }
    }

    private class ParameterEscapeAnalysisResult(val escapes: Boolean, val pointsTo: IntArray) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as ParameterEscapeAnalysisResult

            if (escapes != other.escapes) return false
            if (pointsTo.size != other.pointsTo.size) return false
            return pointsTo.indices.all { pointsTo[it] == other.pointsTo[it] }
        }

        override fun hashCode(): Int {
            var result = escapes.hashCode()
            pointsTo.forEach { result = 31 * result + it.hashCode() }
            return result
        }

        override fun toString(): String {
            return "${if (escapes) "ESCAPES" else "LOCAL"}, POINTS TO: ${pointsTo.contentToString()}"
        }
    }

    private class FunctionEscapeAnalysisResult(val parameters: Array<ParameterEscapeAnalysisResult>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as FunctionEscapeAnalysisResult

            if (parameters.size != other.parameters.size) return false
            return parameters.indices.all { parameters[it] == other.parameters[it] }
        }

        override fun hashCode(): Int {
            var result = 0
            parameters.forEach { result = 31 * result + it.hashCode() }
            return result
        }

        override fun toString(): String {
            return parameters.withIndex().joinToString("\n") {
                if (it.index < parameters.size - 1)
                    "PARAM#${it.index}: ${it.value}"
                else "RETURN: ${it.value}"
            }
        }

        val isTrivial get() = parameters.all { !it.escapes && it.pointsTo.isEmpty() }

        companion object {
            fun fromBits(escapesMask: Int, pointsToMasks: IntArray) = FunctionEscapeAnalysisResult(
                    pointsToMasks.indices.map { parameterIndex ->
                        val escapes = escapesMask and (1 shl parameterIndex) != 0
                        val curPointsToMask = pointsToMasks[parameterIndex]
                        val pointsTo = (0..31).filter { curPointsToMask and (1 shl it) != 0 }.toIntArray()
                        ParameterEscapeAnalysisResult(escapes, pointsTo)
                    }.toTypedArray()
            )
        }
    }

    private class InterproceduralAnalysisResult(val functionEscapeAnalysisResults: Map<FunctionDescriptor, FunctionEscapeAnalysisResult>)

    private class InterproceduralAnalysis(val context: Context,
                                          val runtime: RuntimeAware,
                                          val externalFunctionEscapeAnalysisResults: Map<String, FunctionEscapeAnalysisResult>,
                                          val intraproceduralAnalysisResult: IntraproceduralAnalysisResult,
                                          val lifetimes: MutableMap<IrElement, Lifetime>) {

        private val expressionValuesExtractor = intraproceduralAnalysisResult.expressionValuesExtractor

        fun analyze(irModule: IrModuleFragment): InterproceduralAnalysisResult {
            val callGraph = buildCallGraph(irModule)
            if (DEBUG > 0) {
                println("CALL GRAPH")
                callGraph.directEdges.forEach { t, u ->
                    println("    FUN $t")
                    u.callSites.forEach {
                        println("        CALLS ${if (callGraph.directEdges.containsKey(it.actualCallee)) "LOCAL" else "EXTERNAL"} ${it.actualCallee}")
                    }
                    callGraph.reversedEdges[t]!!.forEach {
                        println("        CALLED BY $it")
                    }
                }
            }

            val condensation = callGraph.buildCondensation()
            if (DEBUG > 0) {
                println("CONDENSATION")
                condensation.topologicalOrder.forEach { multiNode ->
                    println("    MULTI-NODE")
                    multiNode.nodes.forEach {
                        println("        $it")
                        callGraph.directEdges[it]!!.callSites
                                .filter { callGraph.directEdges.containsKey(it.actualCallee) }
                                .forEach { println("            CALLS ${it.actualCallee}") }
                        callGraph.reversedEdges[it]!!.forEach {
                            println("            CALLED BY $it")
                        }
                    }
                }
            }

            callGraph.directEdges.forEach { function, node ->
                val parameters = function.allParameters
                node.escapeAnalysisResult = FunctionEscapeAnalysisResult(
                        // Assume no edges at the beginning.
                        // Then iteratively add needed.
                        (parameters.map { ParameterEscapeAnalysisResult(false, IntArray(0)) }
                                + ParameterEscapeAnalysisResult(false, IntArray(0))
                                ).toTypedArray()
                )
            }

            for (multiNode in condensation.topologicalOrder)
                analyze(callGraph, multiNode)

            return InterproceduralAnalysisResult(callGraph.directEdges.entries.associateBy({ it.key }, { it.value.escapeAnalysisResult }))
        }

        private class CallSite(val expression: IrMemberAccessExpression, val actualCallee: FunctionDescriptor)

        private class CallGraphNode {
            val callSites = mutableListOf<CallSite>()
            lateinit var escapeAnalysisResult: FunctionEscapeAnalysisResult
        }

        private class MultiNode(val nodes: Set<FunctionDescriptor>)

        private class CallGraphCondensation(val topologicalOrder: List<MultiNode>)

        private class CallGraph(val directEdges: Map<FunctionDescriptor, CallGraphNode>,
                                val reversedEdges: Map<FunctionDescriptor, MutableList<FunctionDescriptor>>) {

            fun addEdge(caller: FunctionDescriptor, callSite: CallSite) {
                directEdges[caller]!!.callSites += callSite
                reversedEdges[callSite.actualCallee]?.add(caller)
            }

            private inner class CondensationBuilder {
                private val visited = mutableSetOf<FunctionDescriptor>()
                private val order = mutableListOf<FunctionDescriptor>()
                private val nodeToMultiNodeMap = mutableMapOf<FunctionDescriptor, MultiNode>()
                private val multiNodesOrder = mutableListOf<MultiNode>()

                fun build(): CallGraphCondensation {
                    // First phase.
                    directEdges.keys.forEach {
                        if (!visited.contains(it))
                            findOrder(it)
                    }

                    // Second phase.
                    visited.clear()
                    val multiNodes = mutableListOf<MultiNode>()
                    order.reversed().forEach {
                        if (!visited.contains(it)) {
                            val nodes = mutableSetOf<FunctionDescriptor>()
                            paint(it, nodes)
                            multiNodes += MultiNode(nodes)
                        }
                    }

                    // Topsort of built condensation.
                    multiNodes.forEach { multiNode ->
                        multiNode.nodes.forEach { nodeToMultiNodeMap.put(it, multiNode) }
                    }
                    visited.clear()
                    multiNodes.forEach {
                        if (!visited.contains(it.nodes.first()))
                            findMultiNodesOrder(it)
                    }

                    return CallGraphCondensation(multiNodesOrder)
                }

                private fun findOrder(node: FunctionDescriptor) {
                    visited += node
                    directEdges[node]!!.callSites.forEach {
                        val callee = it.actualCallee
                        if (directEdges.containsKey(callee) && !visited.contains(callee))
                            findOrder(callee)
                    }
                    order += node
                }

                private fun paint(node: FunctionDescriptor, multiNode: MutableSet<FunctionDescriptor>) {
                    visited += node
                    multiNode += node
                    reversedEdges[node]!!.forEach {
                        if (!visited.contains(it))
                            paint(it, multiNode)
                    }
                }

                private fun findMultiNodesOrder(node: MultiNode) {
                    visited.addAll(node.nodes)
                    node.nodes.forEach {
                        directEdges[it]!!.callSites.forEach {
                            val callee = it.actualCallee
                            if (directEdges.containsKey(callee) && !visited.contains(callee))
                                findMultiNodesOrder(nodeToMultiNodeMap[callee]!!)
                        }
                    }
                    multiNodesOrder += node
                }
            }

            fun buildCondensation() = CondensationBuilder().build()
        }

        private fun buildCallGraph(element: IrElement): CallGraph {
            val directEdges = mutableMapOf<FunctionDescriptor, CallGraphNode>()
            val reversedEdges = mutableMapOf<FunctionDescriptor, MutableList<FunctionDescriptor>>()
            intraproceduralAnalysisResult.functionAnalysisResults.keys.forEach {
                directEdges.put(it, CallGraphNode())
                reversedEdges.put(it, mutableListOf())
            }
            val callGraph = CallGraph(directEdges, reversedEdges)
            element.acceptVoid(object : IrElementVisitorVoidWithContext() {

                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitCall(expression: IrCall) {
                    addEdge(expression)
                    super.visitCall(expression)
                }

                override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
                    addEdge(expression)
                    super.visitDelegatingConstructorCall(expression)
                }

                private fun addEdge(expression: IrMemberAccessExpression) {
                    val caller = currentFunction?.scope?.scopeOwner
                    if (caller != null) {
                        caller as FunctionDescriptor
                        val callee = expression.descriptor.original as FunctionDescriptor

                        if (!callee.isOverridable || expression !is IrCall || expression.superQualifier != null) {
                            val superQualifier = (expression as? IrCall)?.superQualifier
                            if (superQualifier == null)
                                callGraph.addEdge(caller, CallSite(expression, callee))
                            else {
                                val actualCallee = superQualifier.unsubstitutedMemberScope.getOverridingOf(callee) ?: callee
                                callGraph.addEdge(caller, CallSite(expression, actualCallee))
                            }
                        } else {
                            if (DEBUG > 0)
                                println("A virtual call")

                            // Try devirtualize. TODO: Move to separate analysis.
                            val possibleReceivers = expressionValuesExtractor.extractNodesUsingVariableValues(
                                    expression     = expression.dispatchReceiver!!,
                                    variableValues = intraproceduralAnalysisResult.functionAnalysisResults[caller]!!.variableValues
                            )
                            val possibleReceiverTypes = possibleReceivers
                                    .map { (it as? ParameterDescriptor)?.type ?: (it as IrExpression).type }
                                    .map { it.erasure() }
                                    .map { if (!it.isMarkedNullable) it else it.makeNotNullable() }
                            val actualCallees = possibleReceiverTypes.map {
                                if (DEBUG > 0)
                                    println("Actual receiver type: $it")
                                val receiverScope = (it.constructor.declarationDescriptor as ClassDescriptor).unsubstitutedMemberScope
                                val actualCallee = receiverScope.getOverridingOf(callee) ?: callee
                                if (DEBUG > 0)
                                    println("Actual callee: $actualCallee")
                                actualCallee
                            }
                            if (actualCallees.any { it.isOverridable }) {
                                if (DEBUG > 0)
                                    println("An actual virtual call")
                                // An actual virtual call.
                                callGraph.addEdge(caller, CallSite(expression, callee))
                            } else {
                                actualCallees.forEach { actualCallee ->
                                    callGraph.addEdge(caller, CallSite(expression, actualCallee))
                                }
                            }
                        }
                    }
                }

                private fun MemberScope.getOverridingOf(function: FunctionDescriptor) = when (function) {
                    is PropertyGetterDescriptor ->
                        this.getContributedVariables(function.correspondingProperty.name, NoLookupLocation.FROM_BACKEND)
                                .firstOrNull { OverridingUtil.overrides(it, function.correspondingProperty) }?.getter

                    is PropertySetterDescriptor ->
                        this.getContributedVariables(function.correspondingProperty.name, NoLookupLocation.FROM_BACKEND)
                                .firstOrNull { OverridingUtil.overrides(it, function.correspondingProperty) }?.setter

                    else -> this.getContributedFunctions(function.name, NoLookupLocation.FROM_BACKEND)
                            .firstOrNull { OverridingUtil.overrides(it, function) }
                }
            })
            return callGraph
        }

        private fun analyze(callGraph: CallGraph, multiNode: MultiNode) {
            if (DEBUG > 0) {
                println("Analyzing multiNode:\n    ${multiNode.nodes.joinToString("\n   ") { it.toString() }}")
                multiNode.nodes.forEach { from ->
                    println("IR")
                    println(ir2stringWhole(intraproceduralAnalysisResult.functionAnalysisResults[from]!!.function))
                    callGraph.directEdges[from]!!.callSites.forEach { to ->
                        println("CALL")
                        println("   from $from")
                        println("   to ${ir2stringWhole(to.expression)}")
                    }
                }
            }
            val pointsToGraphs = multiNode.nodes.associateBy({ it }, { PointsToGraph(it) })
            val toAnalyze = mutableSetOf<FunctionDescriptor>()
            toAnalyze.addAll(multiNode.nodes)
            while (toAnalyze.isNotEmpty()) {
                val function = toAnalyze.first()
                toAnalyze.remove(function)
                if (DEBUG > 0) {
                    println("Processing function $function")
                }
                val startResult = callGraph.directEdges[function]!!.escapeAnalysisResult
                if (DEBUG > 0)
                    println("Start escape analysis result:\n$startResult")
                analyze(callGraph, pointsToGraphs[function]!!, function)
                val endResult = callGraph.directEdges[function]!!.escapeAnalysisResult
                if (startResult == endResult) {
                    if (DEBUG > 0)
                        println("Escape analysis is not changed")
                } else {
                    if (DEBUG > 0)
                        println("Escape analysis was refined:\n$endResult")
                    callGraph.reversedEdges[function]?.forEach {
                        if (multiNode.nodes.contains(it))
                            toAnalyze.add(it)
                    }
                }
            }
            pointsToGraphs.values.forEach { graph ->
                graph.nodes.keys
                        .filterIsInstance<IrExpression>()
                        .forEach { lifetimes.put(it, graph.lifetimeOf(it)) }
            }
        }

        private fun analyze(callGraph: CallGraph, pointsToGraph: PointsToGraph, function: FunctionDescriptor) {
            if (DEBUG > 0) {
                println("Before calls analysis")
                pointsToGraph.print()
            }
            callGraph.directEdges[function]!!.callSites.forEach {
                val callee = it.actualCallee
                val calleeEAResult = callGraph.directEdges[callee]?.escapeAnalysisResult ?: getExternalFunctionEAResult(it)
                pointsToGraph.processCall(it, calleeEAResult)
            }
            if (DEBUG > 0) {
                println("After calls analysis")
                pointsToGraph.print()
            }
            // Build transitive closure.
            val eaResult = pointsToGraph.buildClosure()
            if (DEBUG > 0) {
                println("After closure building")
                pointsToGraph.print()
            }
            callGraph.directEdges[function]!!.escapeAnalysisResult = eaResult
        }

        private val NAME_ESCAPES = Name.identifier("Escapes")
        private val NAME_POINTS_TO = Name.identifier("PointsTo")
        private val FQ_NAME_KONAN = FqName.fromSegments(listOf("konan"))

        private val FQ_NAME_ESCAPES = FQ_NAME_KONAN.child(NAME_ESCAPES)
        private val FQ_NAME_POINTS_TO = FQ_NAME_KONAN.child(NAME_POINTS_TO)

        private val konanPackage = context.builtIns.builtInsModule.getPackage(FQ_NAME_KONAN).memberScope
        private val escapesAnnotationDescriptor = konanPackage.getContributedClassifier(
                NAME_ESCAPES, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
        private val escapesWhoDescriptor = escapesAnnotationDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters.single()
        private val pointsToAnnotationDescriptor = konanPackage.getContributedClassifier(
                NAME_POINTS_TO, NoLookupLocation.FROM_BACKEND) as ClassDescriptor
        private val pointsToOnWhomDescriptor = pointsToAnnotationDescriptor.unsubstitutedPrimaryConstructor!!.valueParameters.single()

        private fun KotlinType.erasure(): KotlinType {
            val descriptor = this.constructor.declarationDescriptor
            return when (descriptor) {
                is ClassDescriptor -> this
                is TypeParameterDescriptor -> {
                    val upperBound = descriptor.upperBounds.singleOrNull() ?:
                            TODO("$descriptor : ${descriptor.upperBounds}")

                    if (this.isMarkedNullable) {
                        // `T?`
                        upperBound.erasure().makeNullable()
                    } else {
                        upperBound.erasure()
                    }
                }
                else -> TODO(this.toString())
            }
        }

        private fun getExternalFunctionEAResult(callSite: CallSite): FunctionEscapeAnalysisResult {
            val callee = callSite.actualCallee
            if (DEBUG > 0)
                println("External callee: $callee")
            val parameters = callee.allParameters
            val calleeEAResult =
                    if (!callee.isOverridable || callSite.expression !is IrCall || callSite.expression.superQualifier != null) {
                        getExternalFunctionEAResult(callee)
                    } else {
                        if (DEBUG > 0)
                            println("A virtual call")
                        FunctionEscapeAnalysisResult((0..parameters.size).map {
                            val type = if (it < parameters.size) parameters[it].type else callee.returnType
                            ParameterEscapeAnalysisResult(
                                    escapes  = runtime.isInteresting(type), // Conservatively assume all references escape.
                                    pointsTo = IntArray(0)
                            )
                        }.toTypedArray())
                    }
            if (DEBUG > 0) {
                println("Escape analysis result")
                println(calleeEAResult.toString())
            }
            return calleeEAResult
        }

        private fun parseEAResultFromAnnotations(function: FunctionDescriptor): FunctionEscapeAnalysisResult {
            if (DEBUG > 0)
                println("Parsing from annotations, function: $function")
            val escapesAnnotation = function.annotations.findAnnotation(FQ_NAME_ESCAPES)
            val pointsToAnnotation = function.annotations.findAnnotation(FQ_NAME_POINTS_TO)
            val escapesBitMask = (escapesAnnotation?.allValueArguments?.get(escapesWhoDescriptor) as? ConstantValue<Int>)?.value
            val pointsToBitMask = (pointsToAnnotation?.allValueArguments?.get(pointsToOnWhomDescriptor) as? ConstantValue<List<IntValue>>)?.value
            return FunctionEscapeAnalysisResult.fromBits(
                    escapesBitMask ?: 0,
                    (0..function.allParameters.size).map { pointsToBitMask?.elementAtOrNull(it)?.value ?: 0 }.toIntArray()
            )
        }

        private fun tryGetFromExternalEAResults(function: FunctionDescriptor): FunctionEscapeAnalysisResult? {
            if (!function.isExported()) return null
            val symbolName = function.symbolName
            if (DEBUG > 0)
                println("Trying get external results for function: $symbolName")
            return externalFunctionEscapeAnalysisResults[symbolName]
        }

        private fun getExternalFunctionEAResult(function: FunctionDescriptor): FunctionEscapeAnalysisResult {
            if (DEBUG > 0)
                println("External function: $function")
            val functionEAResult = tryGetFromExternalEAResults(function) ?: parseEAResultFromAnnotations(function)
            if (DEBUG > 0) {
                println("Escape analysis result")
                println(functionEAResult.toString())
            }
            return functionEAResult
        }

        private enum class PointsToGraphNodeKind(val weight: Int) {
            LOCAL(0),
            RETURN_VALUE(1),
            ESCAPES(2)
        }

        private class PointsToGraphNode(roles: Roles) {
            // TODO: replace Any with sealed class.
            val edges = mutableSetOf<Any>()

            var kind = when {
                roles.escapes() -> PointsToGraphNodeKind.ESCAPES
                roles.has(Role.RETURN_VALUE) -> PointsToGraphNodeKind.RETURN_VALUE
                else -> PointsToGraphNodeKind.LOCAL
            }

            var parameterPointingOnUs: Int? = null

            fun addIncomingParameter(parameter: Int) {
                if (kind == PointsToGraphNodeKind.ESCAPES)
                    return
                if (parameterPointingOnUs == null)
                    parameterPointingOnUs = parameter
                else {
                    parameterPointingOnUs = null
                    kind = PointsToGraphNodeKind.ESCAPES
                }
            }
        }

        private inner class PointsToGraph(val function: FunctionDescriptor) {

            val nodes = mutableMapOf<Any, PointsToGraphNode>()

            fun lifetimeOf(node: Any?) = nodes[node]!!.let {
                when (it.kind) {
                    PointsToGraphNodeKind.ESCAPES -> Lifetime.GLOBAL

                    PointsToGraphNodeKind.LOCAL -> {
                        val parameterPointingOnUs = it.parameterPointingOnUs
                        if (parameterPointingOnUs != null)
                        // A value is stored into a parameter field.
                            Lifetime.PARAMETER_FIELD(parameterPointingOnUs)
                        else
                        // A value is neither stored into a global nor into any parameter nor into the return value -
                        // it can be allocated locally.
                            Lifetime.LOCAL
                    }

                    PointsToGraphNodeKind.RETURN_VALUE -> {
                        when {
                        // If a value is stored into a parameter field and into the return value - consider it escapes.
                            it.parameterPointingOnUs != null -> Lifetime.GLOBAL
                        // If a value is explicitly returned.
                            returnValues.contains(node) -> Lifetime.RETURN_VALUE
                        // A value is stored into a field of the return value.
                            else -> Lifetime.INDIRECT_RETURN_VALUE
                        }
                    }
                }
            }

            init {
                val functionAnalysisResult = intraproceduralAnalysisResult.functionAnalysisResults[function]!!
                if (DEBUG > 0) {
                    println("Building points-to graph for function $function")
                    println("Results of preliminary function analysis")
                }
                functionAnalysisResult.parameterRoles.elementData.forEach { parameter, roles ->
                    if (DEBUG > 0)
                        println("PARAM $parameter: $roles")
                    nodes.put(parameter, PointsToGraphNode(roles))
                }
                functionAnalysisResult.expressionToRoles.forEach { expression, roles ->
                    if (DEBUG > 0)
                        println("EXPRESSION ${ir2stringWhole(expression)}: $roles")
                    nodes.put(expression, PointsToGraphNode(roles))
                }
                functionAnalysisResult.expressionToRoles.forEach { expression, roles ->
                    addEdges(expression, roles)
                }
                functionAnalysisResult.parameterRoles.elementData.forEach { parameter, roles ->
                    addEdges(parameter, roles)
                }
            }

            val returnValues = nodes.filter { it.value.kind == PointsToGraphNodeKind.RETURN_VALUE }.map { it.key }.toSet()

            private fun addEdges(from: Any, roles: Roles) {
                val pointsToEdge = roles.data[Role.FIELD_WRITTEN]
                        ?: return
                pointsToEdge.entries.forEach {
                    val to = it.data!!
                    if (nodes.containsKey(to)) {
                        nodes[from]!!.edges.add(it.data)
                        if (DEBUG > 0) {
                            println("EDGE: ")
                            println("    FROM: ${nodeToString(from)}")
                            println("    TO: ${nodeToString(it.data)}")
                        }
                    }
                }
            }

            private fun nodeToString(node: Any): String {
                return if (node is IrExpression) ir2stringWhole(node) else node.toString()
            }

            fun print() {
                println("POINTS-TO GRAPH")
                println("NODES")
                nodes.forEach { t, u ->
                    println("    ${lifetimeOf(t)} ${nodeToString(t)}")
                }
                println("EDGES")
                nodes.forEach { t, u ->
                    u.edges.forEach {
                        println("    FROM ${nodeToString(t)}")
                        println("    TO ${nodeToString(it)}")
                    }
                }
            }

            fun processCall(callSite: CallSite, calleeEscapeAnalysisResult: FunctionEscapeAnalysisResult) {
                if (DEBUG > 0) {
                    println("Processing callSite: ${ir2stringWhole(callSite.expression)}")
                    println("Callee: ${callSite.actualCallee}")
                    println("Callee escape analysis result:")
                    println(calleeEscapeAnalysisResult.toString())
                }
                val callee = callSite.actualCallee
                val callResult = callSite.expression
                val arguments = callResult.getArguments()
                val possibleArgumentValues = if (callee is ConstructorDescriptor) {
                    if (callResult is IrDelegatingConstructorCall) {
                        (0..arguments.size).map {
                            val thiz = IrGetValueImpl(UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                                    (function as ConstructorDescriptor).constructedClass.thisAsReceiverParameter)
                            expressionValuesExtractor.extractNodesUsingVariableValues(
                                    expression     = if (it == 0) thiz else arguments[it - 1].second,
                                    variableValues = intraproceduralAnalysisResult.functionAnalysisResults[function]!!.variableValues
                            )
                        }
                    } else {
                        (0..arguments.size).map {
                            expressionValuesExtractor.extractNodesUsingVariableValues(
                                    expression     = if (it == 0) callResult else arguments[it - 1].second,
                                    variableValues = intraproceduralAnalysisResult.functionAnalysisResults[function]!!.variableValues
                            )
                        }
                    }
                } else {
                    (0..arguments.size).map {
                        expressionValuesExtractor.extractNodesUsingVariableValues(
                                expression     = if (it < arguments.size) arguments[it].second else callResult,
                                variableValues = intraproceduralAnalysisResult.functionAnalysisResults[function]!!.variableValues
                        )
                    }
                }
                for (index in 0..callee.allParameters.size) {
                    val parameterEAResult = calleeEscapeAnalysisResult.parameters[index]
                    if (parameterEAResult.escapes) {
                        possibleArgumentValues[index].forEach {
                            nodes[it]?.kind = PointsToGraphNodeKind.ESCAPES
                            if (DEBUG > 0)
                                nodes[it]?.let { _ -> println("Node ${nodeToString(it)} escapes") }
                        }
                    }
                    parameterEAResult.pointsTo.forEach { toIndex ->
                        possibleArgumentValues[index].forEach { from ->
                            val nodeFrom = nodes[from]
                            if (nodeFrom == null) {
                                if (DEBUG > 0) {
                                    println("There is no node")
                                    println("    FROM ${nodeToString(from)}")
                                }
                            } else {
                                possibleArgumentValues[toIndex].forEach { to ->
                                    val nodeTo = nodes[to]
                                    if (nodeTo == null) {
                                        if (DEBUG > 0) {
                                            println("There is no node")
                                            println("    TO ${nodeToString(to)}")
                                        }
                                    } else {
                                        if (DEBUG > 0) {
                                            println("Adding edge")
                                            println("    FROM ${nodeToString(from)}")
                                            println("    TO ${nodeToString(to)}")
                                        }
                                        nodeFrom.edges.add(to)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            fun buildClosure(): FunctionEscapeAnalysisResult {
                val parameters = function.allParameters.withIndex().toList()
                val reachabilities = mutableListOf<IntArray>()
                if (DEBUG > 0) {
                    println("BUILDING CLOSURE")
                }
                parameters.forEach {
                    val visited = mutableSetOf<Any>()
                    if (nodes[it.value] != null)
                        findReachable(it.value, visited)
                    visited -= it.value
                    if (DEBUG > 0) {
                        println("Reachable from ${it.value}")
                        visited.forEach {
                            println("    ${nodeToString(it)}")
                        }
                    }
                    val reachable = mutableListOf<Int>()
                    parameters.forEach { (index, parameter) ->
                        if (visited.contains(parameter))
                            reachable += index
                    }
                    if (returnValues.any { visited.contains(it) })
                        reachable += parameters.size
                    reachabilities.add(reachable.toIntArray())
                    visited.forEach { node ->
                        if (node !is ParameterDescriptor)
                            nodes[node]!!.addIncomingParameter(it.index)
                    }
                }
                val visitedFromReturnValues = mutableSetOf<Any>()
                returnValues.forEach {
                    if (!visitedFromReturnValues.contains(it)) {
                        findReachable(it, visitedFromReturnValues)
                    }
                }
                reachabilities.add(
                        parameters.filter { visitedFromReturnValues.contains(it.value) }
                                .map { it.index }.toIntArray()
                )

                propagate(PointsToGraphNodeKind.ESCAPES)
                propagate(PointsToGraphNodeKind.RETURN_VALUE)

                return FunctionEscapeAnalysisResult(reachabilities.withIndex().map { (index, reachability) ->
                    val escapes =
                            if (index == parameters.size) // Return value.
                                returnValues.any { nodes[it]!!.kind == PointsToGraphNodeKind.ESCAPES }
                            else {
                                runtime.isInteresting(parameters[index].value.type)
                                        && nodes[parameters[index].value]!!.kind == PointsToGraphNodeKind.ESCAPES
                            }
                    ParameterEscapeAnalysisResult(escapes, reachability)
                }.toTypedArray())
            }

            private fun findReachable(node: Any, visited: MutableSet<Any>) {
                visited += node
                nodes[node]!!.edges.forEach {
                    if (!visited.contains(it)) {
                        findReachable(it, visited)
                    }
                }
            }

            private fun propagate(kind: PointsToGraphNodeKind) {
                val visited = mutableSetOf<Any>()
                nodes.filter { it.value.kind == kind }
                        .forEach { node, _ -> propagate(node, kind, visited) }
            }

            private fun propagate(node: Any, kind: PointsToGraphNodeKind, visited: MutableSet<Any>) {
                if (visited.contains(node)) return
                visited.add(node)
                val nodeInfo = nodes[node]!!
                if (nodeInfo.kind.weight < kind.weight)
                    nodeInfo.kind = kind
                nodeInfo.edges.forEach { propagate(it, kind, visited) }
            }
        }
    }

    internal fun computeLifetimes(irModule: IrModuleFragment, context: Context, runtime: RuntimeAware,
                                  lifetimes: MutableMap<IrElement, Lifetime>) {
        assert(lifetimes.isEmpty())

        val isStdlib = context.config.configuration[KonanConfigKeys.NOSTDLIB] == true

        val externalFunctionEAResults = mutableMapOf<String, FunctionEscapeAnalysisResult>()
        // TODO: serialize/deserialize in binary.
        context.config.libraries.forEach { library ->
            val libraryEscapeAnalysis = library.escapeAnalysis
            if (libraryEscapeAnalysis != null) {
                if (DEBUG > 0)
                    println("Escape analysis size for lib '${library.libraryName}': ${libraryEscapeAnalysis.size}")
                val lines = libraryEscapeAnalysis.toString(StandardCharsets.US_ASCII).split('\r', '\n')
                for (i in lines.indices step 2) {
                    val functionName = lines[i]
                    if (functionName == "") break
                    val functionEAResult = lines[i + 1].split(';')
                    val escapesMask = functionEAResult[0]
                    val pointsToMasks = functionEAResult.drop(1)
                    externalFunctionEAResults.put(functionName,
                            FunctionEscapeAnalysisResult.fromBits(escapesMask.toInt(), pointsToMasks.map { it.toInt() }.toIntArray()))
                }
            }
        }
        val intraproceduralAnalysisResult = IntraproceduralAnalysis(runtime).analyze(irModule)
        val interproceduralAnalysisResult = InterproceduralAnalysis(context, runtime,
                externalFunctionEAResults, intraproceduralAnalysisResult, lifetimes).analyze(irModule)
        if (isStdlib) { // Save only for stdlib for now.
            val output = StringBuilder()
            interproceduralAnalysisResult.functionEscapeAnalysisResults
                    .filterNot { it.value.isTrivial }
                    .filter { it.key.isExported() }
                    .forEach { functionDescriptor, functionEAResult ->
                        output.appendln(functionDescriptor.symbolName)
                        var escapes = 0
                        val pointsTo = IntArray(functionEAResult.parameters.size)
                        functionEAResult.parameters.forEachIndexed { index, parameterEAResult ->
                            if (parameterEAResult.escapes)
                                escapes = escapes or (1 shl index)
                            var pointsToMask = 0
                            parameterEAResult.pointsTo.forEach {
                                pointsToMask = pointsToMask or (1 shl it)
                            }
                            pointsTo[index] = pointsToMask
                        }
                        output.append(escapes)
                        pointsTo.forEach {
                            output.append(';')
                            output.append(it)
                        }
                        output.appendln()
                    }
            val outputBytes = output.toString().toByteArray(StandardCharsets.US_ASCII)
            if (context.escapeAnalysisResults == null)
                context.escapeAnalysisResults = outputBytes
            else
                context.escapeAnalysisResults = ((context.escapeAnalysisResults!!.asList() + outputBytes) as List<Byte>).toByteArray()
        }
    }
}