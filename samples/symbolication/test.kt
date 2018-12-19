fun f(p: Int) {
  if (p == 0) throw Error()
  f(p - 1)
}

fun main() = f(10)
