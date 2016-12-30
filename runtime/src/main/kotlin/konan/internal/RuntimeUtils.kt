package konan.internal

@ExportForCppRuntime
internal fun ThrowNullPointerException(): Nothing {
    throw NullPointerException()
}

@ExportForCppRuntime
internal fun ThrowArrayIndexOutOfBoundsException(): Nothing {
    throw IndexOutOfBoundsException()
}

@ExportForCppRuntime
internal fun ThrowClassCastException(): Nothing {
    throw ClassCastException()
}

internal fun ThrowNoWhenBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

@ExportForCppRuntime
internal fun TheEmptyString() = ""