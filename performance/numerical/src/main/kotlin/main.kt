import pi.*

fun main(args: Array<String>) {
    print("3.")
    for (n in 1 .. 1000 step 9)
            print(pi_nth_digit(n).toString().padStart(9, '0'))
    println()
}
