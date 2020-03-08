package org.jetbrains.kotlin.backend.konan.boxing

import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Contains mappings from origins to related specializations,
 * like Box<Int> -> BoxInt.
 */
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

    fun forClass(origin: IrType) = classes[origin]
    fun forFunction(origin: IrFunction, types: List<IrType?>) = functions[origin to types]
    fun forConstructor(origin: IrConstructor, classSpec: IrType) = constructors[origin to classSpec]
    fun forMemberFunction(origin: IrFunction, dispatchSpec: IrType) = members[origin to dispatchSpec]?.let { it as IrFunction }

    operator fun contains(type: IrType) = type in classes
}