package konan.internal

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.NativePointed
import kotlinx.cinterop.NativePtr

@Intrinsic external fun areEqualByValue(first: NativePtr, second: NativePtr): Boolean
@Intrinsic external fun areEqualByValue(first: NativePointed?, second: NativePointed?): Boolean
@Intrinsic external fun areEqualByValue(first: CPointer<*>?, second: CPointer<*>?): Boolean

