package kotlin
import konan.internal.ExportForCompiler

@kotlin.internal.InlineOnly
public inline operator fun <T> Array<T>.plus(elements: Array<T>): Array<T> {
    val result = copyOfUninitializedElements(this.size + elements.size)
    elements.copyRangeTo(result, 0, elements.size, this.size)
    return result
}
