package org.jetbrains.kotlin.konan.util

import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


/// we use the fake delimiter to support the feature
/// of the [@Argument] parser to merge several arguments
/// into the same argument (it works only for [Array<String>]
/// type, but we do not need the separator
///
/// the right fix is to update the [@Argument] to support paths separation
const val ARGUMENT_NO_DELIMITER = "\\n\\t\\t\\n\\t\\t\\n\\ue000\\ue001\\ue002\\n\\t\\t\\t\\t\\n"

private fun parseCommandLineString(it: String) : List<String> {
  val arguments = mutableListOf<String>()
  val buf = StringBuilder()

  var isEscape = false
  for (ch in it) {
      when {
        ch != ' ' && ch != '\\' -> {
          if(isEscape) {
            buf.append('\\')
            isEscape = false
          }
          buf.append(ch)
        }

        ch == '\\' -> when {
          isEscape -> {
            buf.append('\\')
            isEscape = false
          }

          else -> {
            isEscape = true
          }
        }

        ch == ' ' -> when {
          isEscape -> {
            buf.append(' ')
            isEscape = false
          }

          else -> {
            if (buf.isNotEmpty()) {
              arguments += buf.toString()
              buf.setLength(0)
            }
          }
        }
      }
  }
  if (isEscape) buf.append('\\')

  if (buf.isNotEmpty()) {
    arguments += buf.toString()
  }

  return arguments
}

fun escapeToCommandLineString(args: Iterable<String>) : String {
  return args.joinToString(separator = " ", transform = {
    it.replace("\\", "\\\\").replace(" ", "\\ ")
  })
}

fun Properties.getParsedCommandLineString(name: String): List<String> {
  val s = getProperty(name) ?: return listOf()
  return parseCommandLineString(s)
}


class CommandLineOpts : ReadOnlyProperty<Any, List<String>> {
  private val arguments = mutableListOf<String>()

  override fun getValue(thisRef: Any, property: KProperty<*>) = arguments.toList()

  private inline fun setterProperty(crossinline setter: (String) -> Unit) = object : ReadWriteProperty<Any, Array<String>> {
    override fun getValue(thisRef: Any, property: KProperty<*>): Array<String> {
      //we return an empty array to avoid concatenation of values from
      //org.jetbrains.kotlin.cli.common.arguments.ParseCommandLineArgumentsKt.updateField
      return emptyArray()
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Array<String>) {
      if (value.size != 1) throw IllegalArgumentException("One argument was expected")
      setter(value[0])
    }
  }

  val singletonOpt = setterProperty { arguments += it }
  val multipleOpts = setterProperty { arguments += parseCommandLineString(it) }
}


fun main() {
  //A substitute for unit tests. There is no infrastructure for unit tests right now

  fun testEscape(input: String, vararg args: String) {
    val actual = parseCommandLineString(input)
    if (actual != listOf(*args)) error("invalid value, but was: $actual")

    val str = escapeToCommandLineString(listOf(*args))
    val strActual = parseCommandLineString(str)
    if (strActual != listOf(*args)) error("invalid value, but was: $strActual")
  }

  testEscape("")
  testEscape("a", "a")
  testEscape("a\\", "a\\")
  testEscape("a\\\\", "a\\")
  testEscape("a\\\\ ", "a\\")

  testEscape("\\a", "\\a")
  testEscape("\\\\a", "\\a")
  testEscape("\\\\ a", "\\", "a")

  testEscape("a b", "a", "b")
  testEscape(" a b ", "a", "b")
  testEscape(" a\\ b ", "a b")
  testEscape(" a\\\\ b ", "a\\", "b")
  testEscape(" a\\\\\\ b ", "a\\ b")


  class TestArguments {
    private val linkerOptsHolder = CommandLineOpts()
    val linkerOpts: List<String> by linkerOptsHolder
    var linkerOptInternal by linkerOptsHolder.singletonOpt
    var linkerOptsInternal by linkerOptsHolder.multipleOpts
  }

  lateinit var x : TestArguments
  fun reset() { x = TestArguments() }
  fun assertArgs(vararg args: String) {
    if (x.linkerOpts != listOf(*args)) error("invalid value, but was: ${x.linkerOpts}")
  }

  reset()
  assertArgs()

  reset()
  x.linkerOptsInternal = arrayOf("a b\\ c \\d\\")
  assertArgs("a", "b c", "\\d\\")

  reset()
  x.linkerOptInternal = arrayOf("a b c")
  assertArgs("a b c")

  reset()
  x.linkerOptInternal = arrayOf("a b c")
  x.linkerOptsInternal = arrayOf("d   e")
  x.linkerOptInternal = arrayOf("g")
  x.linkerOptInternal = arrayOf(" h ")
  x.linkerOptsInternal = arrayOf(" i j\\ k ")
  assertArgs("a b c", "d", "e", "g", " h ", "i", "j k")

  reset()
  x.linkerOptsInternal = arrayOf("d\\\\ e")
  assertArgs("d\\", "e")
}
