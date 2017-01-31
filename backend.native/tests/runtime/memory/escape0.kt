/* fun foo1(arg: String) : String = arg + " foo1"

fun foo2(arg: String) : String = arg + " foo2"


fun bar(arg1: String, arg2: String) : String  = arg1 + " bar " + arg2

fun zoo1() : String {
    var x = foo1("")
    var y = 4
    return x
}

fun zoo2() : String {
    val x = foo1("")
    var y = 5
    return x
}

class Node(var previous: Node?)

fun zoo3() : Node {
    var current = Node(null)
    for (i in 1 .. 5) {
        current = Node(current)
    }
    return current
} */

fun zoo4() : Any {
    var a = Any()
    var b = Any()
    a = b
    return a
}

fun main(args : Array<String>) {
    //println(bar(foo1(foo2("")), foo2(foo1(""))))
}
