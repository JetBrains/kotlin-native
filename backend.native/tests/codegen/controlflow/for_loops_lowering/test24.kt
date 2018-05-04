package codegen.controlflow.for_loops_lowering.test24

import kotlin.test.*

@Test fun runTest() {
	for (i in 0 .. 10) {
		for (j in arrayOf(1, 2, 3)) {
	    	println(j)
	    }
	}   
}
