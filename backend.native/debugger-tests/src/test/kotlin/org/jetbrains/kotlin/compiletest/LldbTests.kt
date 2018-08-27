/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.compiletest.lldbTest
import org.junit.Test

class LldbTests {
    @Test
    fun `can step through code`() = lldbTest("""
        fun main(args: Array<String>) {
            var x = 1
            var y = 2
            var z = x + y
            println(z)
        }
    """, """
        > b main.kt:2
        Breakpoint 1: [..]

        > r
        Process [..] stopped
        [..] stop reason = breakpoint 1.1
        [..] at main.kt:2

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:3

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:4

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:5
    """)

    @Test
    fun `can inspect values of primitive types`() = lldbTest("""
        fun main(args: Array<String>) {
            var a: Byte =  1
            var b: Int  =  2
            var c: Long = -3
            var d: Char = 'c'
            var e: Boolean = true
            return
        }
    """, """
            > b main.kt:7
            > r
            > fr var
            (char) a = '\x01'
            (int) b = 2
            (long) c = -3
            (unsigned char) d = 'c'
            (bool) e = true
    """)

    @Test
    fun `can inspect classes`() = lldbTest("""
        fun main(args: Array<String>) {
            val point = Point(1, 2)
            val person = Person()
            return
        }

        data class Point(val x: Int, val y: Int)
        class Person {
            override fun toString() = "John Doe"
        }
    """, """
        > b main.kt:4
        > r
        > fr var
        (ObjHeader *) args = []
        (ObjHeader *) point = {'y': 2, 'x': 1}
        (ObjHeader *) person = {}
    """)

    @Test
    fun `can inspect arrays`() = lldbTest("""
        fun main(args: Array<String>) {
            val xs = IntArray(3)
            xs[0] = 1
            xs[1] = 2
            xs[2] = 3
            val ys: Array<Any?> = arrayOfNulls(2)
            ys[0] = Point(1, 2)
            return
        }

        data class Point(val x: Int, val y: Int)
    """, """
        > b main.kt:8
        > r
        > fr var
        (ObjHeader *) args = []
        (ObjHeader *) xs = [1, 2, 3]
        (ObjHeader *) ys = [{'y': 2, 'x': 1}, 'null']
    """)

    @Test
    fun `can inspect array children`() = lldbTest("""
        fun main(args: Array<String>) {
            val xs = intArrayOf(3, 5, 8)
            return
        }

        data class Point(val x: Int, val y: Int)
    """, """
        > b main.kt:3
        > r
        > fr var xs
        (ObjHeader *) xs = [3, 5, 8]
    """)
}