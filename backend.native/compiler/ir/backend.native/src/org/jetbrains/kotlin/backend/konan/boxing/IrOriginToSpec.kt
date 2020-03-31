package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull

/**
 * Contains mappings from origins to related specializations,
 * like Box<Int> -> BoxInt.
 */
// TODO remove useless functions later
object IrOriginToSpec {
    // Mappings like Box<Int> -> BoxInt
    private val classes = mutableMapOf<IrType, IrType>()

    // Replacing call to origin with call to specialization requires information
    // about what are the actual types passed to call
    // to determine correct specialized version (can be many, for example, one for type Int and one for type Byte).
    private val functions = mutableMapOf<Pair<IrFunction, List<IrType?>>, IrFunction>()

    // IrType as member of key is required to determine which specialized class the desired member belongs to.
    // Specialized classes can be many for the same origin.
    private val constructors = mutableMapOf<Pair<IrConstructor, IrType>, IrConstructor>()
    private val members = mutableMapOf<Pair<IrDeclaration, IrType>, IrDeclaration>()

    // This structure helps answer the following requests:
    //  - Find all specializations that were built for given declaration;
    //  - Which is the specialization of given declaration for given types, declared in given receiver
    //    (or which is effectively static).
    // TODO consider custom types, currently it is just a mishmash of different containers
    private val specializations = mutableMapOf<IrDeclaration, MutableMap<Pair<List<IrType?>, IrType?>, IrDeclaration>>()

    fun <T : IrDeclaration> newSpec(origin: T, types: List<IrType?>, specDispatchType: IrType? = null, spec: T) {
        specializations.putIfAbsent(origin, mutableMapOf())
        specializations[origin]!![types to specDispatchType] = spec
    }

    fun newClass(origin: IrType, spec: IrType) {
        classes[origin] = spec
    }
    fun newFunction(origin: IrFunction, types: List<IrType?>, spec: IrFunction) {
        functions[origin to types] = spec
    }
    fun newConstructor(origin: IrConstructor, classSpec: IrType, spec: IrConstructor) {
        constructors[origin to classSpec] = spec
    }
    fun newMember(origin: IrDeclaration, dispatchSpec: IrType, spec: IrDeclaration) {
        members[origin to dispatchSpec] = spec
    }

    fun <T : IrDeclaration> getAllSpecsFor(origin: T) = specializations[origin]

    inline fun <reified T : IrDeclaration> forSpec(origin: T, types: List<IrType?>, specDispatchType: IrType? = null): T? {
        return getAllSpecsFor(origin)?.get(types to specDispatchType)?.let { it as T }
    }

    fun forClass(origin: IrType): IrType? {
        val originClass = (origin as? IrSimpleType)?.classOrNull?.owner ?: return null
        val types = origin.arguments.map { it as? IrSimpleType }
        return forSpec(originClass, types, null)?.thisReceiver?.type
    }

    fun forFunction(origin: IrFunction, types: List<IrType?>) = functions[origin to types]
    fun forConstructor(origin: IrConstructor, classSpec: IrType) = constructors[origin to classSpec]
    fun forMemberFunction(origin: IrFunction, dispatchSpec: IrType) = members[origin to dispatchSpec]?.let { it as IrFunction }

    operator fun contains(type: IrType) = type.classOrNull?.owner?.let { getAllSpecsFor(it)?.isNotEmpty() } == true
}