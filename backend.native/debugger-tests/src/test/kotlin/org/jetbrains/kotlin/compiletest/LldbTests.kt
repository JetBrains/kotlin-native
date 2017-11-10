import org.jetbrains.kotlin.compiletest.lldbTest
import org.junit.Test

class LldbTests {
    //FIXME: the last one should be main.kt.5
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
        [..] at main.kt:2, [..] stop reason = breakpoint 1.1

        > n
        Process [..] stopped
        [..] at main.kt:3, [..] stop reason = step over

        > n
        Process [..] stopped
        [..] at main.kt:4, [..] stop reason = step over

        > n
        Process [..] stopped
        [..] at main.kt:3, [..] stop reason = step over
    """)

    //FIXME: Boolean and Int are wrong
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
            (void) e = <Unable to determine byte size.>
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
        (ObjHeader *) point = Point(x=1, y=2)
        (ObjHeader *) person = John Doe
    """)
}