package org.jetbrains.kotlin.backend.konan.llvm

import org.jetbrains.kotlin.backend.konan.ir.getArguments
import org.jetbrains.kotlin.descriptors.ValueDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

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
    // If reference is stored in variable.
    VAR_VALUE,
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

internal object EmptyRoleInfo : RoleInfo() {
    override fun add(entry: RoleInfoEntry) = throw Error("Adding to empty role info")
}

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
}

internal class VarValues {
    val data = HashMap<ValueDescriptor, MutableSet<IrElement>>()

    fun addEmpty(variable: ValueDescriptor) {
        data.getOrPut(variable, { mutableSetOf<IrElement>() })
    }

    fun add(variable: ValueDescriptor, element: IrElement) {
        data.get(variable)!!.add(element)
    }

    fun get(variable: ValueDescriptor) : Set<IrElement> = data.get(variable)!!
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
        return Lifetime.LOCAL
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
        return element is IrMemberAccessExpression && context.isObjectType(
                context.getLLVMType(element.type))
    }

    fun isInteresting(variable: ValueDescriptor) : Boolean {
        return context.isObjectType(context.getLLVMType(variable.type))
    }

    override fun visitElement(element: IrElement) {
        if (isInteresting(element)) {
            elementToRoles[element] = Roles()
        }
        element.acceptChildrenVoid(this)
    }

    override fun visitVariable(expression: IrVariable) {
        if (isInteresting(expression.descriptor))
            varValues.addEmpty(expression.descriptor)
        super.visitVariable(expression)
    }

    override fun visitModuleFragment(module: IrModuleFragment) {
        module.acceptChildrenVoid(this)
    }
}

//
// elementToRoles is filled with all possible roles given element can play.
// varToElements is filled with all possible elements that could be stored in a variable.
//
internal class RoleAssignerVisitor(
        val elementToRoles: MutableMap<IrElement, Roles>,
        val varValues: VarValues) : IrElementVisitorVoid {

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitCall(expression: IrCall) {
        elementToRoles.get(expression)?.add(
                Role.CALL_RESULT, RoleInfoEntry(expression))
        for (argument in expression.getArguments()) {
            elementToRoles.get(argument.second)?.add(
                    Role.CALL_ARGUMENT, RoleInfoEntry(expression))
        }
        super.visitCall(expression)
    }

    override fun visitSetField(expression: IrSetField) {
        elementToRoles.get(expression)?.add(
                Role.FIELD_WRITTEN, RoleInfoEntry(expression.receiver))
        super.visitSetField(expression)
    }

    override fun visitSetVariable(expression: IrSetVariable) {
        elementToRoles.get(expression.value)?.add(
                Role.VAR_VALUE, RoleInfoEntry(expression.descriptor))
        varValues.add(expression.descriptor, expression.value)
        super.visitSetVariable(expression)
    }

    override fun visitGetValue(expression: IrGetValue) {
        super.visitGetValue(expression)
    }

    override fun visitVariable(expression: IrVariable) {
        val initializer = expression.initializer
        if (initializer != null) {
            elementToRoles.get(initializer)?.add(
                    Role.VAR_VALUE, RoleInfoEntry(expression.descriptor))
            varValues.add(expression.descriptor, initializer)
        }
        super.visitVariable(expression)
    }

    override fun visitReturn(expression: IrReturn) {
        elementToRoles.get(expression.value)?.add(
                Role.RETURN_VALUE, RoleInfoEntry(expression))
        super.visitReturn(expression)
    }

    override fun visitModuleFragment(module: IrModuleFragment) {
        module.acceptChildrenVoid(this)
        // Now, for all variable values, add all roles coming from variable uses.
        module.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitGetValue(expression: IrGetValue) {
                super.visitGetValue(expression)
            }
        })
    }
}

//
// Analysis we're implementing here is as following.
//  * compute roles IR value nodes can play
//  * merge role information with all methods being called
//  * squash set of roles to conservatively estimated lifetime
//
internal fun computeLifetimes(irModule: IrModuleFragment, context: RuntimeAware,
                              lifetimes: MutableMap<IrElement, Lifetime>) {
    assert(lifetimes.size == 0)

    val elementToRoles = mutableMapOf<IrElement, Roles>()
    val varValues = VarValues()
    irModule.acceptVoid(ElementFinderVisitor(context, elementToRoles, varValues))
    irModule.acceptVoid(RoleAssignerVisitor(elementToRoles, varValues))
    elementToRoles.forEach { element, roles ->
        println("for ${element} roles are ${roles.data.keys.joinToString(", ")}")
        lifetimes[element] = rolesToLifetime(roles)
    }
    varValues.data.forEach { variable, expressions ->
        println("${variable} it may be assigned ${expressions.joinToString(", ")}")
    }
    //irModule.acceptVoid(EscapeAnalyzerVisitor(roles, lifetimes))
}