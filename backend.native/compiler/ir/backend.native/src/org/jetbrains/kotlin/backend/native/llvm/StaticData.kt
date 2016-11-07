package org.jetbrains.kotlin.backend.native.llvm

import llvm.*

/**
 * Provides utilities to create static data.
 */
internal class StaticData(override val context: Context): ContextUtils {

    /**
     * Represents the LLVM global variable.
     */
    class Global private constructor(val staticData: StaticData, val llvmGlobal: LLVMOpaqueValue) {
        companion object {
            fun create(staticData: StaticData, type: LLVMOpaqueType, name: String): Global {
                val module = staticData.context.llvmModule
                if (LLVMGetNamedGlobal(module, name) != null) {
                    throw IllegalArgumentException("Global '$name' already exists")
                }

                val llvmGlobal = LLVMAddGlobal(module, type, name)!!
                return Global(staticData, llvmGlobal)
            }
        }

        fun setInitializer(value: ConstValue) {
            LLVMSetInitializer(llvmGlobal, value.getLlvmValue())
        }

        fun setConstant(value: Boolean) {
            LLVMSetGlobalConstant(llvmGlobal, if (value) 1 else 0)
        }

        val pointer = Pointer.to(this)
    }

    /**
     * Represents the pointer to static data.
     * It can be a pointer to either a global or any its element.
     *
     * TODO: this class is probably should be implemented more optimally
     */
    class Pointer private constructor(val global: Global,
                                      private val delegate: ConstPointer,
                                      val offsetInGlobal: Long) : ConstPointer by delegate {

        companion object {
            fun to(global: Global) = Pointer(global, constPointer(global.llvmGlobal), 0L)
        }

        private fun getElementOffset(index: Int): Long {
            val llvmTargetData = global.staticData.llvmTargetData
            val type = LLVMGetElementType(delegate.getLlvmType())
            return when (LLVMGetTypeKind(type)) {
                LLVMTypeKind.LLVMStructTypeKind -> LLVMOffsetOfElement(llvmTargetData, type, index)
                LLVMTypeKind.LLVMArrayTypeKind -> LLVMABISizeOfType(llvmTargetData, LLVMGetElementType(type)) * index
                else -> TODO()
            }
        }

        override fun getElementPtr(index: Int): Pointer {
            return Pointer(global, delegate.getElementPtr(index), offsetInGlobal + this.getElementOffset(index))
        }

        /**
         * @return the distance from other pointer to this.
         *
         * @throws UnsupportedOperationException if it is not possible to represent the distance as [Int] value
         */
        fun sub(other: Pointer): Int {
            if (this.global != other.global) {
                throw UnsupportedOperationException("pointers must belong to the same global")
            }

            val res = this.offsetInGlobal - other.offsetInGlobal
            if (res.toInt().toLong() != res) {
                throw UnsupportedOperationException("result doesn't fit into Int")
            }

            return res.toInt()
        }
    }

    /**
     * Creates [Global] with given type and name.
     *
     * It is external until explicitly initialized with [Global.setInitializer].
     */
    fun createGlobal(type: LLVMOpaqueType, name: String): Global {
        return Global.create(this, type, name)
    }

    /**
     * Creates [Global] with given name and value.
     */
    fun placeGlobal(name: String, initializer: ConstValue): Global {
        val global = createGlobal(initializer.getLlvmType(), name)
        global.setInitializer(initializer)
        return global
    }

    /**
     * Creates array-typed global with given name and value.
     */
    fun placeGlobalArray(name: String, elemType: LLVMOpaqueType?, elements: List<ConstValue>): Global {
        val initializer = ConstArray(elemType, elements)
        val global = placeGlobal(name, initializer)

        return global
    }
}