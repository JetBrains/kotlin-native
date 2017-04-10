import kotlinx.cinterop.*
import tensorflow.*

typealias Status = CPointer<TF_Status>
typealias Operation = CPointer<TF_Operation>
typealias OperationDescription = CPointer<TF_OperationDescription>
typealias Graph = CPointer<TF_Graph>
typealias Tensor = CPointer<TF_Tensor>

val Status.isOk: Boolean get() = TF_GetCode(this) == TF_OK
val Status.errorMessage: String get() = TF_Message(this)!!.toKString()
fun Status.delete() = TF_DeleteStatus(this)
fun Status.validate() {
    if (!isOk) {
        throw Error("Status is not ok: $errorMessage")
    }

    delete()
}

inline fun <T> validateStatus(body: (Status) -> T): T {
    val status = TF_NewStatus()!!
    val result = body(status)
    status.validate()
    return result
}

fun intTensor(value: Int): Tensor {
    val data = nativeHeap.allocArray<IntVar>(1)
    data[0] = value

    fun intTensorDeallocator(data: COpaquePointer?, len: size_t, arg: COpaquePointer?) {
        nativeHeap.free(data!!.reinterpret<IntVar>())
    }

    return TF_NewTensor(TF_INT32,
            dims = null, num_dims = 0,
            data = data, len = IntVar.size,
            deallocator = staticCFunction(::intTensorDeallocator), deallocator_arg = null)!!
}

inline fun Graph.operation(type: String, name: String, initDescription: (OperationDescription) -> Unit) : Operation {
    val description = TF_NewOperation(this, type, name)!!
    initDescription(description)
    return validateStatus { TF_FinishOperation(description, it)!! }
}

fun Graph.intConst(value: Int, name: String = "scalarConst") = operation("Const", name) { description ->
    validateStatus { TF_SetAttrTensor(description, "value", intTensor(value), it) }
    TF_SetAttrType(description, "dtype", TF_INT32)
}

fun Graph.intPlaceholder(name: String = "input") = operation("Placeholder", name) { description ->
    TF_SetAttrType(description, "dtype", TF_INT32)
}

fun Graph.add(left: Operation, right: Operation, name: String = "add"): Operation {
    // TODO Is there a better way?
    val inputs = nativeHeap.allocArray<TF_Output>(2)
    inputs[0].apply { oper = left; index = 0 }
    inputs[1].apply { oper = right; index = 0 }

    val result = operation("AddN", name) { description ->
        TF_AddInputList(description, inputs, 2)
    }

    nativeHeap.free(inputs)

    return result
}

fun main(args: Array<String>) {
    println("Hello, TensorFlow ${TF_Version()!!.toKString()}!")

    val graph = TF_NewGraph()!!
    val input = graph.intPlaceholder()
    val two = graph.intConst(2)
    val inputPlusTwo = graph.add(input, two)
}