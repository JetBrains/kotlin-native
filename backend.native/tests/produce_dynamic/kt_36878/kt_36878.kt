fun takeByte(arr: UByteArray) {
	arr.forEach{println(it.toString(16))}
}

fun createByteArray(a:UByte, b:UByte, c:UByte, d:UByte) = ubyteArrayOf(a, b, c, d)