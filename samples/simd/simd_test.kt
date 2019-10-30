import kotlinx.cinterop.*
import kotlin.test.*

import simd_wrapper.*

//import kotlin.math.*
import platform.Accelerate.*
//
//fun test_dist() {
//    var v1 = vectorOf(-1f, 0f, 0f, -7f)
//    var v2 = vectorOf(1f, 4f, 3f, 7f);
//    val len = my_simd_distance(v1, v2)
//    println(len)
//}

fun test_Accelerate() {
    var v1 = vectorOf(1f, 2f, 4f, 9f)
    printVFloat(vsqrtf(v1))
}

class Box<T>(t: T) {
//    var value = t
}

//fun runTest() {
//    val dBox: Box<Double> = Box<Double>(21.0)
////    println(dBox.value)
//    val v = vectorOf(42f, 1f, 2f, 3f)
//    val box: Box<NativeVector> = Box<NativeVector>(v)
////    println(box.value)
////    printVFloat(box.value)
//}

fun testOOB() {
    val f = vectorOf(1f, 3.162f, 10f, 31f)
//    println(f.getFloat(-1))
    println("getByte(15) is OK: ${f.getByte(15)}")
    var result = "PASSED"
    try {
        println("getFloat(4) should fail: ${f.getFloat(4)}")
        result = "FAILED"
    } catch (e: IndexOutOfBoundsException) {}

    try {
        println(f.getInt(-1))
        result = "FAILED"
    } catch (e: IndexOutOfBoundsException) {}

    try {
        println(f.getFloat(4))
        result = "FAILED"
    } catch (e: IndexOutOfBoundsException) {}
    println("Exception test $result")
}

fun main() {

    val v = vectorOf(42f, 1f, 2f, 3f)
    val box: Box<NativeVector> = Box<NativeVector>(v)

//    printVFloat(v)
//
//    test_dist()
//
////    val ln = kotlin.math.ln(2.718)
////    println("log(2.718) = $ln")
//
    val f2 = vectorOf(1f, 3.162f, 10f, 31f)
    println(f2.getFloat(1))
//    println(f2.getInt(0))
    println(f2.getByte(0))
    println(f2.getUByte(1))
    println(f2.getUByte(2))
    println(f2.getByte(3))

    val lg = vlog10f(f2)
    printVFloat(lg)
//    runTest()

    test_Accelerate()
    testOOB()

//    println(lg)
}
