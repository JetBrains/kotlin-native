/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kliopt

import kotlin.test.*

class SubcommandsTests {
    @Test
    fun testSubcommand() {
        val argParser = ArgParser("testParser")
        val output by argParser.option(ArgType.String(), "output", "o", "Output file")
        class Summary: Subcommand("summary") {
            val invert by option(ArgType.Boolean(), "invert", "i", "Invert results")
            val addendums by arguments(ArgType.Int(), "addendums", description = "Addendums")
            var result: Int = 0

            override fun execute() {
                result = addendums!!.sum()
                result = if (invert!!) -1 * result else result
            }
        }
        val action = Summary()
        argParser.subcommands(action)
        argParser.parse(arrayOf("-o", "out.txt", "summary", "-i", "2", "3", "5"))
        assertEquals("out.txt", output)
        assertEquals(-10, action.result)
    }

    @Test
    fun testCommonOptions() {
        abstract class CommonOptions(name: String): Subcommand(name) {
            val numbers by arguments(ArgType.Int(), "numbers", description = "Numbers")
        }
        class Summary: CommonOptions("summary") {
            val invert by option(ArgType.Boolean(), "invert", "i", "Invert results")
            var result: Int = 0

            override fun execute() {
                result = numbers!!.sum()
                result = if (invert!!) -1 * result else result
            }
        }

        class Subtraction : CommonOptions("sub") {
            var result: Int = 0

            override fun execute() {
                result = numbers!!.map { -it }.sum()
            }
        }

        val summaryAction = Summary()
        val subtractionAction = Subtraction()
        val argParser = ArgParser("testParser")
        argParser.subcommands(summaryAction, subtractionAction)
        argParser.parse(arrayOf("summary", "2", "3", "5"))
        assertEquals(10, summaryAction.result)

        val argParserSubtraction = ArgParser("testParser")
        argParserSubtraction.subcommands(summaryAction, subtractionAction)
        argParserSubtraction.parse(arrayOf("sub", "8", "-2", "3"))
        assertEquals(-9, subtractionAction.result)
    }

    @Test
    fun testRecursiveSubcommands() {
        val argParser = ArgParser("testParser")

        class Summary: Subcommand("summary") {
            val addendums by arguments(ArgType.Int(), "addendums", description = "Addendums")
            var result: Int = 0

            override fun execute() {
                result = addendums!!.sum()
            }
        }

        class Calculation: Subcommand("calc") {
            init {
                subcommands(Summary())
            }
            val invert by option(ArgType.Boolean(), "invert", "i", "Invert results")
            var result: Int = 0

            override fun execute() {
                result = (subcommands["summary"] as Summary).result
                result = if (invert!!) -1 * result else result
            }
        }

        val action = Calculation()
        argParser.subcommands(action)
        argParser.parse(arrayOf("calc", "-i", "summary", "2", "3", "5"))
        assertEquals(-10, action.result)
    }
}
