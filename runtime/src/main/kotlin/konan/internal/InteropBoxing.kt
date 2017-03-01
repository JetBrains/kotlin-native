package konan.internal

import kotlinx.cinterop.*

public class NativePtrBox(val value: NativePtr) {
    override fun equals(other: Any?): Boolean {
        if (other !is NativePtrBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

public fun boxNativePtr(value: NativePtr) = NativePtrBox(value)

public class NativePointedBox(val value: NativePointed) {
    override fun equals(other: Any?): Boolean {
        if (other !is NativePointedBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

public fun boxNativePointed(value: NativePointed?) = if (value != null) NativePointedBox(value) else null
public fun unboxNativePointed(box: NativePointedBox?) = box?.value

public class CPointerBox(val value: CPointer<*>) {
    override fun equals(other: Any?): Boolean {
        if (other !is CPointerBox) {
            return false
        }

        return this.value == other.value
    }

    override fun hashCode() = value.hashCode()

    override fun toString() = value.toString()
}

public fun boxCPointer(value: CPointer<*>?) = if (value != null) CPointerBox(value) else null
public fun unboxCPointer(box: CPointerBox?) = box?.value
