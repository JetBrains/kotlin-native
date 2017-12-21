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

package org.jetbrains.kotlin.backend.konan.optimizations

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import sun.misc.Unsafe

internal class ExternalModulesDFG(val allTypes: List<DataFlowIR.Type.Declared>,
                                  val publicTypes: Map<Long, DataFlowIR.Type.Public>,
                                  val publicFunctions: Map<Long, DataFlowIR.FunctionSymbol.Public>,
                                  val functionDFGs: Map<DataFlowIR.FunctionSymbol, DataFlowIR.Function>)

private val theUnsafe = Unsafe::class.java.getDeclaredField("theUnsafe").let {
    it.isAccessible = true
    it.get(null) as Unsafe
}

private val byteArrayDataOffset = theUnsafe.arrayBaseOffset(ByteArray::class.java).toLong()
private val intArrayDataOffset  = theUnsafe.arrayBaseOffset(IntArray::class.java).toLong()
private val charArrayDataOffset = theUnsafe.arrayBaseOffset(CharArray::class.java).toLong()
private val stringValueOffset   = theUnsafe.objectFieldOffset(String::class.java.getDeclaredField("value"))

private val VERSION = 1

internal object DFGSerializer {

    class ArraySlice(var array: ByteArray, var index: Int = 0) {

        //------------Write------------------------------------------------------------------//

        fun writeByte(value: Byte) {
            ensureSize(1)
            theUnsafe.putByte(array, byteArrayDataOffset + index, value)
            index++
        }

        fun writeInt(value: Int) {
            ensureSize(4)
            theUnsafe.putInt(array, byteArrayDataOffset + index, value)
            index += 4
        }

        fun writeLong(value: Long) {
            ensureSize(8)
            theUnsafe.putLong(array, byteArrayDataOffset + index, value)
            index += 8
        }

        fun writeBoolean(value: Boolean) {
            ensureSize(1)
            theUnsafe.putBoolean(array, byteArrayDataOffset + index, value)
            index++
        }

        inline fun <T> writeNullable(value: T?, valueWriter: ArraySlice.(T) -> Unit) {
            writeBoolean(value != null)
            if (value != null)
                this.valueWriter(value)
        }

        fun writeNullableInt(value: Int?) = writeNullable(value) { this.writeInt(it) }

        fun writeNullableString(s: String?) = writeNullable(s) { writeString(it) }

        //------------Read------------------------------------------------------------------//

        fun readByte(): Byte {
            checkSize(1)
            return theUnsafe.getByte(array, byteArrayDataOffset + index).also { index++ }
        }

        fun readInt(): Int {
            checkSize(4)
            return theUnsafe.getInt(array, byteArrayDataOffset + index).also { index += 4 }
        }

        fun readLong(): Long {
            checkSize(8)
            return theUnsafe.getLong(array, byteArrayDataOffset + index).also { index += 8 }
        }

        fun readBoolean(): Boolean {
            checkSize(1)
            return theUnsafe.getBoolean(array, byteArrayDataOffset + index).also { index++ }
        }

        inline fun <T> readNullable(valueReader: ArraySlice.() -> T) =
                if (readBoolean()) this.valueReader() else null

        fun readNullableInt() = readNullable { this.readInt() }

        fun readNullableString() = readNullable { readString() }

        //------------Write arrays------------------------------------------------------------------//

        fun writeIntArray(source: IntArray) {
            writeInt(source.size)
            val dataSize = source.size * 4
            ensureSize(dataSize)
            theUnsafe.copyMemory(source, intArrayDataOffset, array, byteArrayDataOffset + index, dataSize.toLong())
            index += dataSize
        }

        fun writeString(s: String) {
            val value = theUnsafe.getObject(s, stringValueOffset) as CharArray
            writeInt(value.size)
            val dataSize = value.size * 2
            ensureSize(dataSize)
            theUnsafe.copyMemory(value, charArrayDataOffset, array, byteArrayDataOffset + index, dataSize.toLong())
            index += dataSize
        }

        inline fun <reified T> writeArray(array: Array<T>, itemWriter: ArraySlice.(T) -> Unit) {
            writeInt(array.size)
            array.forEach { this.itemWriter(it) }
        }

        //------------Read arrays------------------------------------------------------------------//

        fun readIntArray(): IntArray {
            val size = readInt()
            val result = IntArray(size)
            val dataSize = size * 4
            checkSize(dataSize)
            theUnsafe.copyMemory(array, byteArrayDataOffset + index, result, intArrayDataOffset, dataSize.toLong())
            index += dataSize
            return result
        }

        fun readString(): String {
            val size = readInt()
            val data = CharArray(size)
            val dataSize = size * 2
            checkSize(dataSize)
            theUnsafe.copyMemory(array, byteArrayDataOffset + index, data, charArrayDataOffset, dataSize.toLong())
            index += dataSize
            val str = theUnsafe.allocateInstance(String::class.java) as String
            theUnsafe.putObject(str, stringValueOffset, data)
            return str
        }

        inline fun <reified T> readArray(itemReader: ArraySlice.() -> T) =
                Array(readInt()) { this.itemReader() }

        //------------Resizing------------------------------------------------------------------//

        fun trim() {
            if (array.size > index) {
                val newArray = ByteArray(index)
                theUnsafe.copyMemory(array, byteArrayDataOffset, newArray, byteArrayDataOffset, index.toLong())
                array = newArray
            }
        }

        private fun ensureSize(size: Int) {
            if (index + size > array.size) {
                var newSize = array.size
                while (newSize < index + size)
                    newSize *= 2
                val newArray = ByteArray(newSize)
                theUnsafe.copyMemory(array, byteArrayDataOffset, newArray, byteArrayDataOffset, array.size.toLong())
                array = newArray
            }
        }

        private fun checkSize(size: Int) {
            if (index + size > array.size)
                error("Unexpected end of data")
        }
    }

    class ExternalType(val hash: Long, val name: String?) {

        constructor(data: ArraySlice) : this(data.readLong(), data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeLong(hash)
            result.writeNullableString(name)
        }
    }

    class ItableSlot(val hash: Long, val impl: Int) {

        constructor(data: ArraySlice) : this(data.readLong(), data.readInt())

        fun write(result: ArraySlice) {
            result.writeLong(hash)
            result.writeInt(impl)
        }
    }

    class DeclaredType(val isFinal: Boolean, val isAbstract: Boolean, val superTypes: IntArray,
                       val vtable: IntArray, val itable: Array<ItableSlot>) {

        constructor(data: ArraySlice) : this(data.readBoolean(), data.readBoolean(), data.readIntArray(),
                data.readIntArray(), data.readArray { ItableSlot(this) })

        fun write(result: ArraySlice) {
            result.writeBoolean(isFinal)
            result.writeBoolean(isAbstract)
            result.writeIntArray(superTypes)
            result.writeIntArray(vtable)
            result.writeArray(itable) { it.write(this) }
        }
    }

    class PublicType(val hash: Long, val intestines: DeclaredType, val name: String?) {

        constructor(data: ArraySlice) : this(data.readLong(), DeclaredType(data), data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeLong(hash)
            intestines.write(result)
            result.writeNullableString(name)
        }
    }

    class PrivateType(val index: Int, val intestines: DeclaredType, val name: String?) {

        constructor(data: ArraySlice) : this(data.readInt(), DeclaredType(data), data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeInt(index)
            intestines.write(result)
            result.writeNullableString(name)
        }
    }

    class Type(val external: ExternalType?, val public: PublicType?, val private: PrivateType?, val virtual: Boolean) {

        fun write(result: ArraySlice) {
            result.writeByte(
                    when {
                        external != null -> 1
                        public   != null -> 2
                        private  != null -> 3
                        else             -> 0
                    }.toByte()
            )
            external?.write(result)
            public?.write(result)
            private?.write(result)
        }

        companion object {
            fun external(hash: Long, name: String?) = Type(ExternalType(hash, name), null, null, false)

            fun public(hash: Long, intestines: DeclaredType, name: String?) = Type(null, PublicType(hash, intestines, name), null, false)

            fun private(index: Int, intestines: DeclaredType, name: String?) = Type(null, null, PrivateType(index, intestines, name), false)

            fun virtual() = Type(null, null, null, true)

            fun read(data: ArraySlice): Type {
                val tag = data.readByte().toInt()
                return when (tag) {
                    1    -> Type(ExternalType(data), null, null, false)
                    2    -> Type(null, PublicType(data), null, false)
                    3    -> Type(null, null, PrivateType(data), false)
                    else -> Type(null, null, null, true)
                }
            }
        }
    }

    class ExternalFunctionSymbol(val hash: Long, val numberOfParameters: Int, val escapes: Int?, val pointsTo: IntArray?, val name: String?) {

        constructor(data: ArraySlice) : this(data.readLong(), data.readInt(), data.readNullableInt(),
                data.readNullable { readIntArray() }, data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeLong(hash)
            result.writeInt(numberOfParameters)
            result.writeNullableInt(escapes)
            result.writeNullable(pointsTo) { writeIntArray(it) }
            result.writeNullableString(name)
        }
    }

    class PublicFunctionSymbol(val hash: Long, val numberOfParameters: Int, val index: Int, val name: String?) {

        constructor(data: ArraySlice) : this(data.readLong(), data.readInt(), data.readInt(), data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeLong(hash)
            result.writeInt(numberOfParameters)
            result.writeInt(index)
            result.writeNullableString(name)
        }
    }

    class PrivateFunctionSymbol(val index: Int, val numberOfParameters: Int, val name: String?) {

        constructor(data: ArraySlice) : this(data.readInt(), data.readInt(), data.readNullableString())

        fun write(result: ArraySlice) {
            result.writeInt(index)
            result.writeInt(numberOfParameters)
            result.writeNullableString(name)
        }
    }

    class FunctionSymbol(val external: ExternalFunctionSymbol?, val public: PublicFunctionSymbol?, val private: PrivateFunctionSymbol?) {

        fun write(result: ArraySlice) {
            result.writeByte(
                    when {
                        external != null -> 1
                        public   != null -> 2
                        private  != null -> 3
                        else             -> 0
                    }.toByte()
            )
            external?.write(result)
            public?.write(result)
            private?.write(result)
        }

        companion object {
            fun external(hash: Long, numberOfParameters: Int, escapes: Int?, pointsTo: IntArray?, name: String?) =
                    FunctionSymbol(ExternalFunctionSymbol(hash, numberOfParameters, escapes, pointsTo, name), null, null)

            fun public(hash: Long, numberOfParameters: Int, index: Int, name: String?) =
                    FunctionSymbol(null, PublicFunctionSymbol(hash, numberOfParameters, index, name), null)

            fun private(index: Int, numberOfParameters: Int, name: String?) =
                    FunctionSymbol(null, null, PrivateFunctionSymbol(index, numberOfParameters, name))

            fun read(data: ArraySlice): FunctionSymbol {
                val tag = data.readByte().toInt()
                return when (tag) {
                    1    -> FunctionSymbol(ExternalFunctionSymbol(data), null, null)
                    2    -> FunctionSymbol(null, PublicFunctionSymbol(data), null)
                    3    -> FunctionSymbol(null, null, PrivateFunctionSymbol(data))
                    else -> FunctionSymbol(null, null, null)
                }
            }
        }
    }

    class SymbolTable(val types: Array<Type>, val functionSymbols: Array<FunctionSymbol>) {

        constructor(data: ArraySlice) : this(data.readArray { Type.read(this) }, data.readArray { FunctionSymbol.read(this) })

        fun write(result: ArraySlice) {
            result.writeArray(types) { it.write(this) }
            result.writeArray(functionSymbols) { it.write(this) }
        }
    }

    class Field(val type: Int?, val hash: Long, val name: String?) {

        constructor(data: ArraySlice) : this(data.readNullableInt(), data.readLong(), data.readNullable { readString() })

        fun write(result: ArraySlice) {
            result.writeNullableInt(type)
            result.writeLong(hash)
            result.writeNullable(name) { writeString(it) }
        }
    }

    class Edge(val node: Int, val castToType: Int?) {

        constructor(data: ArraySlice) : this(data.readInt(), data.readNullableInt())

        fun write(result: ArraySlice) {
            result.writeInt(node)
            result.writeNullableInt(castToType)
        }
    }

    class Parameter(val index: Int) {

        constructor(data: ArraySlice) : this(data.readInt())

        fun write(result: ArraySlice) {
            result.writeInt(index)
        }
    }

    class Const(val type: Int) {

        constructor(data: ArraySlice) : this(data.readInt())

        fun write(result: ArraySlice) {
            result.writeInt(type)
        }
    }

    class Call(val callee: Int, val arguments: Array<Edge>, val returnType: Int) {

        constructor(data: ArraySlice) : this(data.readInt(), data.readArray { Edge(this) }, data.readInt())

        fun write(result: ArraySlice) {
            result.writeInt(callee)
            result.writeArray(arguments) { it.write(this) }
            result.writeInt(returnType)
        }
    }

    class StaticCall(val call: Call, val receiverType: Int?) {

        constructor(data: ArraySlice) : this(Call(data), data.readNullableInt())

        fun write(result: ArraySlice) {
            call.write(result)
            result.writeNullableInt(receiverType)
        }
    }

    class NewObject(val call: Call) {

        constructor(data: ArraySlice) : this(Call(data))

        fun write(result: ArraySlice) {
            call.write(result)
        }
    }

    class VirtualCall(val call: Call, val receiverType: Int) {

        constructor(data: ArraySlice) : this(Call(data), data.readInt())

        fun write(result: ArraySlice) {
            call.write(result)
            result.writeInt(receiverType)
        }
    }

    class VtableCall(val virtualCall: VirtualCall, val calleeVtableIndex: Int) {

        constructor(data: ArraySlice) : this(VirtualCall(data), data.readInt())

        fun write(result: ArraySlice) {
            virtualCall.write(result)
            result.writeInt(calleeVtableIndex)
        }
    }

    class ItableCall(val virtualCall: VirtualCall, val calleeHash: Long) {

        constructor(data: ArraySlice) : this(VirtualCall(data), data.readLong())

        fun write(result: ArraySlice) {
            virtualCall.write(result)
            result.writeLong(calleeHash)
        }
    }

    class Singleton(val type: Int, val constructor: Int?) {

        constructor(data: ArraySlice) : this(data.readInt(), data.readNullableInt())

        fun write(result: ArraySlice) {
            result.writeInt(type)
            result.writeNullableInt(constructor)
        }
    }

    class FieldRead(val receiver: Edge?, val field: Field) {

        constructor(data: ArraySlice) : this(data.readNullable { Edge(this) }, Field(data))

        fun write(result: ArraySlice) {
            result.writeNullable(receiver) { it.write(this) }
            field.write(result)
        }
    }

    class FieldWrite(val receiver: Edge?, val field: Field, val value: Edge) {

        constructor(data: ArraySlice) : this(data.readNullable { Edge(this) }, Field(data), Edge(data))

        fun write(result: ArraySlice) {
            result.writeNullable(receiver) { it.write(this) }
            field.write(result)
            value.write(result)
        }
    }

    class Variable(val values: Array<Edge>, val temp: Boolean) {

        constructor(data: ArraySlice) : this(data.readArray { Edge(this) }, data.readBoolean())

        fun write(result: ArraySlice) {
            result.writeArray(values) { it.write(this) }
            result.writeBoolean(temp)
        }
    }

    enum class NodeType {
        UNKNOWN,
        PARAMETER,
        CONST,
        STATIC_CALL,
        NEW_OBJECT,
        VTABLE_CALL,
        ITABLE_CALL,
        SINGLETON,
        FIELD_READ,
        FIELD_WRITE,
        VARIABLE
    }

    class Node {
        var parameter  : Parameter?  = null
        var const      : Const?      = null
        var staticCall : StaticCall? = null
        var newObject  : NewObject?  = null
        var vtableCall : VtableCall? = null
        var itableCall : ItableCall? = null
        var singleton  : Singleton?  = null
        var fieldRead  : FieldRead?  = null
        var fieldWrite : FieldWrite? = null
        var variable   : Variable?   = null

        val type get() = when {
            parameter  != null -> NodeType.PARAMETER
            const      != null -> NodeType.CONST
            staticCall != null -> NodeType.STATIC_CALL
            newObject  != null -> NodeType.NEW_OBJECT
            vtableCall != null -> NodeType.VTABLE_CALL
            itableCall != null -> NodeType.ITABLE_CALL
            singleton  != null -> NodeType.SINGLETON
            fieldRead  != null -> NodeType.FIELD_READ
            fieldWrite != null -> NodeType.FIELD_WRITE
            variable   != null -> NodeType.VARIABLE
            else               -> NodeType.UNKNOWN
        }

        fun write(result: ArraySlice) {
            result.writeByte(type.ordinal.toByte())
            parameter ?.write(result)
            const     ?.write(result)
            staticCall?.write(result)
            newObject ?.write(result)
            vtableCall?.write(result)
            itableCall?.write(result)
            singleton ?.write(result)
            fieldRead ?.write(result)
            fieldWrite?.write(result)
            variable  ?.write(result)
        }

        companion object {
            fun parameter(index: Int) =
                    Node().also { it.parameter = Parameter(index) }

            fun const(type: Int) =
                    Node().also { it.const = Const(type) }

            fun staticCall(call: Call, receiverType: Int?) =
                    Node().also { it.staticCall = StaticCall(call, receiverType) }

            fun newObject(call: Call) =
                    Node().also { it.newObject = NewObject(call) }

            fun vtableCall(virtualCall: VirtualCall, calleeVtableIndex: Int) =
                    Node().also { it.vtableCall = VtableCall(virtualCall, calleeVtableIndex) }

            fun itableCall(virtualCall: VirtualCall, calleeHash: Long) =
                    Node().also { it.itableCall = ItableCall(virtualCall, calleeHash) }

            fun singleton(type: Int, constructor: Int?) =
                    Node().also { it.singleton = Singleton(type, constructor) }

            fun fieldRead(receiver: Edge?, field: Field) =
                    Node().also { it.fieldRead = FieldRead(receiver, field) }

            fun fieldWrite(receiver: Edge?, field: Field, value: Edge) =
                    Node().also { it.fieldWrite = FieldWrite(receiver, field, value) }

            fun variable(values: Array<Edge>, temp: Boolean) =
                    Node().also { it.variable = Variable(values, temp) }

            fun read(data: ArraySlice): Node {
                val type = enumValues<NodeType>()[data.readByte().toInt()]
                val result = Node()
                when (type) {
                    NodeType.PARAMETER   -> result.parameter  = Parameter (data)
                    NodeType.CONST       -> result.const      = Const     (data)
                    NodeType.STATIC_CALL -> result.staticCall = StaticCall(data)
                    NodeType.NEW_OBJECT  -> result.newObject  = NewObject (data)
                    NodeType.VTABLE_CALL -> result.vtableCall = VtableCall(data)
                    NodeType.ITABLE_CALL -> result.itableCall = ItableCall(data)
                    NodeType.SINGLETON   -> result.singleton  = Singleton (data)
                    NodeType.FIELD_READ  -> result.fieldRead  = FieldRead (data)
                    NodeType.FIELD_WRITE -> result.fieldWrite = FieldWrite(data)
                    NodeType.VARIABLE    -> result.variable   = Variable  (data)
                    else                 -> { }
                }
                return result
            }
        }
    }

    class FunctionBody(val nodes: Array<Node>, val returns: Int, val throws: Int) {

        constructor(data: ArraySlice) : this(data.readArray { Node.read(this) }, data.readInt(), data.readInt())

        fun write(result: ArraySlice) {
            result.writeArray(nodes) { it.write(this) }
            result.writeInt(returns)
            result.writeInt(throws)
        }
    }

    class Function(val symbol: Int, val isGlobalInitializer: Boolean,
                   val numberOfParameters: Int, val body: FunctionBody) {

        constructor(data: ArraySlice) : this(data.readInt(), data.readBoolean(), data.readInt(), FunctionBody(data))

        fun write(result: ArraySlice) {
            result.writeInt(symbol)
            result.writeBoolean(isGlobalInitializer)
            result.writeInt(numberOfParameters)
            body.write(result)
        }
    }

    class Module(val symbolTable: SymbolTable, val functions: Array<Function>) {

        constructor(data: ArraySlice) : this(SymbolTable(data), data.readArray { Function(this) })

        fun write(result: ArraySlice) {
            symbolTable.write(result)
            result.writeArray(functions) { it.write(this) }
        }
    }

    private val DEBUG = 0

    private inline fun DEBUG_OUTPUT(severity: Int, block: () -> Unit) {
        if (DEBUG > severity) block()
    }

    fun serialize(context: Context, moduleDFG: ModuleDFG) {
        val symbolTable = moduleDFG.symbolTable
        val typeMap = (symbolTable.classMap.values + DataFlowIR.Type.Virtual).distinct().withIndex().associateBy({ it.value }, { it.index })
        val functionSymbolMap = symbolTable.functionMap.values.distinct().withIndex().associateBy({ it.value }, { it.index })
        DEBUG_OUTPUT(1) {
            println("TYPES: ${typeMap.size}, " +
                    "FUNCTIONS: ${functionSymbolMap.size}, " +
                    "PRIVATE FUNCTIONS: ${functionSymbolMap.keys.count { it is DataFlowIR.FunctionSymbol.Private }}, " +
                    "FUNCTION TABLE SIZE: ${symbolTable.couldBeCalledVirtuallyIndex}"
            )
        }
        val types = typeMap.entries
                .sortedBy { it.value }
                .map {
                    fun buildTypeIntestines(type: DataFlowIR.Type.Declared) =
                            DeclaredType(
                                    type.isFinal,
                                    type.isAbstract,
                                    type.superTypes.map { typeMap[it]!! }.toIntArray(),
                                    type.vtable.map { functionSymbolMap[it]!! }.toIntArray(),
                                    type.itable.map { (hash, symbol) -> ItableSlot(hash, functionSymbolMap[symbol]!!) }.toTypedArray()
                            )

                    val type = it.key
                    when (type) {
                        DataFlowIR.Type.Virtual -> Type.virtual()

                        is DataFlowIR.Type.External -> Type.external(type.hash, type.name)

                        is DataFlowIR.Type.Public -> Type.public(type.hash, buildTypeIntestines(type), type.name)

                        is DataFlowIR.Type.Private -> Type.private(type.index, buildTypeIntestines(type), type.name)

                        else -> error("Unknown type $type")
                    }
                }
                .toTypedArray()
        val functionSymbols = functionSymbolMap.entries
                .sortedBy { it.value }
                .map {
                    val functionSymbol = it.key
                    val numberOfParameters = functionSymbol.numberOfParameters
                    val name = functionSymbol.name
                    when (functionSymbol) {
                        is DataFlowIR.FunctionSymbol.External ->
                            FunctionSymbol.external(functionSymbol.hash, numberOfParameters, functionSymbol.escapes, functionSymbol.pointsTo, name)

                        is DataFlowIR.FunctionSymbol.Public ->
                            FunctionSymbol.public(functionSymbol.hash, numberOfParameters, functionSymbol.symbolTableIndex, name)

                        is DataFlowIR.FunctionSymbol.Private ->
                            FunctionSymbol.private(functionSymbol.symbolTableIndex, numberOfParameters, name)

                        else -> error("Unknown function symbol $functionSymbol")
                    }
                }
                .toTypedArray()
        val functions = moduleDFG.functions.values
                .map { function ->
                    val body = function.body
                    val nodeMap = body.nodes.withIndex().associateBy({ it.value }, { it.index })
                    val nodes = nodeMap.entries
                            .sortedBy { it.value }
                            .map {
                                val node = it.key

                                fun buildEdge(edge: DataFlowIR.Edge) =
                                        Edge(nodeMap[edge.node]!!, edge.castToType?.let { typeMap[it]!! })

                                fun buildCall(call: DataFlowIR.Node.Call) =
                                        Call(
                                                functionSymbolMap[call.callee]!!,
                                                call.arguments.map { buildEdge(it) }.toTypedArray(),
                                                typeMap[call.returnType]!!
                                        )

                                fun buildVirtualCall(virtualCall: DataFlowIR.Node.VirtualCall) =
                                        VirtualCall(buildCall(virtualCall), typeMap[virtualCall.receiverType]!!)

                                fun buildField(field: DataFlowIR.Field) =
                                        Field(field.type?.let { typeMap[it]!! }, field.hash, field.name)

                                when (node) {
                                    is DataFlowIR.Node.Parameter -> Node.parameter(node.index)

                                    is DataFlowIR.Node.Const -> Node.const(typeMap[node.type]!!)

                                    is DataFlowIR.Node.StaticCall ->
                                        Node.staticCall(buildCall(node), node.receiverType?.let { typeMap[it]!! })

                                    is DataFlowIR.Node.NewObject -> Node.newObject(buildCall(node))

                                    is DataFlowIR.Node.VtableCall ->
                                        Node.vtableCall(buildVirtualCall(node), node.calleeVtableIndex)

                                    is DataFlowIR.Node.ItableCall ->
                                        Node.itableCall(buildVirtualCall(node), node.calleeHash)

                                    is DataFlowIR.Node.Singleton ->
                                        Node.singleton(typeMap[node.type]!!, node.constructor?.let { functionSymbolMap[it]!! })

                                    is DataFlowIR.Node.FieldRead ->
                                        Node.fieldRead(node.receiver?.let { buildEdge(it) }, buildField(node.field))

                                    is DataFlowIR.Node.FieldWrite ->
                                        Node.fieldWrite(node.receiver?.let { buildEdge(it) }, buildField(node.field), buildEdge(node.value))

                                    is DataFlowIR.Node.Variable ->
                                        Node.variable(node.values.map { buildEdge(it) }.toTypedArray(), node.temp)

                                    else -> error("Unknown node $node")
                                }
                            }
                            .toTypedArray()
                    Function(
                            functionSymbolMap[function.symbol]!!,
                            function.isGlobalInitializer,
                            function.numberOfParameters,
                            FunctionBody(nodes, nodeMap[body.returns]!!, nodeMap[body.throws]!!)
                    )
                }
                .toTypedArray()
        val module = Module(SymbolTable(types, functionSymbols), functions)
        val writer = ArraySlice(ByteArray(1024))
        writer.writeInt(VERSION)
        module.write(writer)
        writer.trim()
        context.dataFlowGraph = writer.array
    }

    // TODO: Deserialize functions bodies lazily.
    fun deserialize(context: Context, startPrivateTypeIndex: Int, startPrivateFunIndex: Int): ExternalModulesDFG {
        var privateTypeIndex = startPrivateTypeIndex
        var privateFunIndex = startPrivateFunIndex
        val publicTypesMap = mutableMapOf<Long, DataFlowIR.Type.Public>()
        val allTypes = mutableListOf<DataFlowIR.Type.Declared>()
        val publicFunctionsMap = mutableMapOf<Long, DataFlowIR.FunctionSymbol.Public>()
        val functions = mutableMapOf<DataFlowIR.FunctionSymbol, DataFlowIR.Function>()
        val specifics = context.config.configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS)!!
        context.librariesWithDependencies.forEach { library ->
            val libraryDataFlowGraph = library.dataFlowGraph

            DEBUG_OUTPUT(1) {
                println("Data flow graph size for lib '${library.libraryName}': ${libraryDataFlowGraph?.size ?: 0}")
            }

            if (libraryDataFlowGraph != null) {
                val module = DataFlowIR.Module(library.moduleDescriptor(specifics))
                val reader = ArraySlice(libraryDataFlowGraph)
                val version = reader.readInt()
                if (version != VERSION)
                    error("Expected version $VERSION but actual is $version")
                val moduleDataFlowGraph = Module(reader)

                val symbolTable = moduleDataFlowGraph.symbolTable
                val types = symbolTable.types.map {
                    if (it.virtual)
                        DataFlowIR.Type.Virtual
                    else {
                        val external = it.external
                        val public = it.public
                        val private = it.private
                        when {
                            external != null -> DataFlowIR.Type.External(external.hash, external.name)

                            public != null ->
                                DataFlowIR.Type.Public(public.hash, public.intestines.isFinal,
                                        public.intestines.isAbstract, public.name).also {
                                    publicTypesMap.put(it.hash, it)
                                    allTypes += it
                                }

                            else ->
                                DataFlowIR.Type.Private(privateTypeIndex++, private!!.intestines.isFinal,
                                        private.intestines.isAbstract, private.name).also {
                                    allTypes += it
                                }
                        }
                    }
                }

                val functionSymbols = symbolTable.functionSymbols.map {
                    val external = it.external
                    val public = it.public
                    val private = it.private
                    when {
                        external != null ->
                            DataFlowIR.FunctionSymbol.External(external.hash, external.numberOfParameters, external.escapes, external.pointsTo, external.name)

                        public != null -> {
                            val symbolTableIndex = public.index
                            if (symbolTableIndex >= 0)
                                ++module.numberOfFunctions
                            DataFlowIR.FunctionSymbol.Public(public.hash, public.numberOfParameters, module, symbolTableIndex, public.name).also {
                                publicFunctionsMap.put(it.hash, it)
                            }
                        }

                        else -> {
                            val symbolTableIndex = private!!.index
                            if (symbolTableIndex >= 0)
                                ++module.numberOfFunctions
                            DataFlowIR.FunctionSymbol.Private(privateFunIndex++, private.numberOfParameters, module, symbolTableIndex, private.name)
                        }
                    }
                }

                DEBUG_OUTPUT(1) {
                    println("Lib: ${library.libraryName}, types: ${types.size}, functions: ${functionSymbols.size}")
                }

                symbolTable.types.forEachIndexed { index, type ->
                    val deserializedType = types[index] as? DataFlowIR.Type.Declared
                            ?: return@forEachIndexed
                    if (deserializedType == DataFlowIR.Type.Virtual)
                        return@forEachIndexed
                    val intestines = if (deserializedType is DataFlowIR.Type.Public)
                                         type.public!!.intestines
                                     else
                                         type.private!!.intestines
                    deserializedType.superTypes += intestines.superTypes.map { types[it] }
                    deserializedType.vtable += intestines.vtable.map { functionSymbols[it] }
                    intestines.itable.forEach {
                        deserializedType.itable.put(it.hash, functionSymbols[it.impl])
                    }
                }

                fun deserializeEdge(edge: Edge) =
                        DataFlowIR.Edge(edge.castToType?.let { types[it] })

                fun deserializeCall(call: Call) =
                        DataFlowIR.Node.Call(
                                functionSymbols[call.callee],
                                call.arguments.map { deserializeEdge(it) },
                                types[call.returnType],
                                null
                        )

                fun deserializeVirtualCall(virtualCall: VirtualCall): DataFlowIR.Node.VirtualCall {
                    val call = deserializeCall(virtualCall.call)
                    return DataFlowIR.Node.VirtualCall(
                            call.callee,
                            call.arguments,
                            call.returnType,
                            types[virtualCall.receiverType],
                            null
                    )
                }

                fun deserializeField(field: Field) =
                        DataFlowIR.Field(field.type?.let { types[it] }, field.hash, field.name)

                fun deserializeBody(body: FunctionBody): DataFlowIR.FunctionBody {
                    val nodes = body.nodes.map {
                        when (it.type) {
                            NodeType.PARAMETER ->
                                DataFlowIR.Node.Parameter(it.parameter!!.index)

                            NodeType.CONST ->
                                DataFlowIR.Node.Const(types[it.const!!.type])

                            NodeType.STATIC_CALL -> {
                                val staticCall = it.staticCall!!
                                val call = deserializeCall(staticCall.call)
                                val receiverType = staticCall.receiverType?.let { types[it] }
                                DataFlowIR.Node.StaticCall(call.callee, call.arguments, call.returnType, receiverType, null)
                            }

                            NodeType.NEW_OBJECT -> {
                                val call = deserializeCall(it.newObject!!.call)
                                DataFlowIR.Node.NewObject(call.callee, call.arguments, call.returnType, null)
                            }

                            NodeType.VTABLE_CALL -> {
                                val vtableCall = it.vtableCall!!
                                val virtualCall = deserializeVirtualCall(vtableCall.virtualCall)
                                DataFlowIR.Node.VtableCall(
                                        virtualCall.callee,
                                        virtualCall.receiverType,
                                        vtableCall.calleeVtableIndex,
                                        virtualCall.arguments,
                                        virtualCall.returnType,
                                        virtualCall.callSite
                                )
                            }

                            NodeType.ITABLE_CALL -> {
                                val itableCall = it.itableCall!!
                                val virtualCall = deserializeVirtualCall(itableCall.virtualCall)
                                DataFlowIR.Node.ItableCall(
                                        virtualCall.callee,
                                        virtualCall.receiverType,
                                        itableCall.calleeHash,
                                        virtualCall.arguments,
                                        virtualCall.returnType,
                                        virtualCall.callSite
                                )
                            }

                            NodeType.SINGLETON -> {
                                val singleton = it.singleton!!
                                DataFlowIR.Node.Singleton(types[singleton.type], singleton.constructor?.let { functionSymbols[it] })
                            }

                            NodeType.FIELD_READ -> {
                                val fieldRead = it.fieldRead!!
                                val receiver = fieldRead.receiver?.let { deserializeEdge(it) }
                                DataFlowIR.Node.FieldRead(receiver, deserializeField(fieldRead.field))
                            }

                            NodeType.FIELD_WRITE -> {
                                val fieldWrite = it.fieldWrite!!
                                val receiver = fieldWrite.receiver?.let { deserializeEdge(it) }
                                DataFlowIR.Node.FieldWrite(receiver, deserializeField(fieldWrite.field), deserializeEdge(fieldWrite.value))
                            }

                            NodeType.VARIABLE -> {
                                val variable = it.variable!!
                                DataFlowIR.Node.Variable(variable.values.map { deserializeEdge(it) }, variable.temp)
                            }

                            else -> error("Unknown node: $it")
                        }
                    }

                    body.nodes.forEachIndexed { index, node ->
                        val deserializedNode = nodes[index]
                        when (node.type) {
                            NodeType.STATIC_CALL ->
                                node.staticCall!!.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.StaticCall).arguments[i].node = nodes[arg.node]
                                }

                            NodeType.NEW_OBJECT ->
                                node.newObject!!.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.NewObject).arguments[i].node = nodes[arg.node]
                                }

                            NodeType.VTABLE_CALL ->
                                node.vtableCall!!.virtualCall.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.VtableCall).arguments[i].node = nodes[arg.node]
                                }

                            NodeType.ITABLE_CALL ->
                                node.itableCall!!.virtualCall.call.arguments.forEachIndexed { i, arg ->
                                    (deserializedNode as DataFlowIR.Node.ItableCall).arguments[i].node = nodes[arg.node]
                                }

                            NodeType.FIELD_READ ->
                                node.fieldRead!!.receiver?.let {
                                    (deserializedNode as DataFlowIR.Node.FieldRead).receiver!!.node = nodes[it.node]
                                }

                            NodeType.FIELD_WRITE -> {
                                val deserializedFieldWrite = deserializedNode as DataFlowIR.Node.FieldWrite
                                val fieldWrite = node.fieldWrite!!
                                fieldWrite.receiver?.let { deserializedFieldWrite.receiver!!.node = nodes[it.node] }
                                deserializedFieldWrite.value.node = nodes[fieldWrite.value.node]
                            }

                            NodeType.VARIABLE ->
                                node.variable!!.values.forEachIndexed { i, value ->
                                    (deserializedNode as DataFlowIR.Node.Variable).values[i].node = nodes[value.node]
                                }

                            else -> { }
                        }
                    }
                    return DataFlowIR.FunctionBody(nodes, nodes[body.returns] as DataFlowIR.Node.Variable, nodes[body.throws] as DataFlowIR.Node.Variable)
                }

                moduleDataFlowGraph.functions.forEach {
                    val symbol = functionSymbols[it.symbol]
                    functions.put(symbol, DataFlowIR.Function(symbol, it.isGlobalInitializer, it.numberOfParameters, deserializeBody(it.body)))
                }
            }
        }

        return ExternalModulesDFG(allTypes, publicTypesMap, publicFunctionsMap, functions)
    }
}