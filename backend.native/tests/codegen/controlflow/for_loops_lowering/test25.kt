package codegen.controlflow.for_loops_lowering.test25

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(1, 2, 3)
	for (i in 0 .. 10) {
		for (j in arr) {
	    	println(j)
	    }
	}   
}
