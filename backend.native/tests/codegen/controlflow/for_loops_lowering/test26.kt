package codegen.controlflow.for_loops_lowering.test26

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(1, 2, 3)
	for (i in 0 .. 10) {
		for (j in arr) {
			for (k in arrayOf(4, 5, 6)) {
				println(j)
				println(k)
			}	    	
	    }
	}   
}
