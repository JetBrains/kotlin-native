package codegen.controlflow.for_loops_lowering.test27

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
	for (i in 0 .. 10 step 2) {
		for (j in arr) {
			for (k in arrayOf("a", "b", "helolo world", "fff")) {
				println(j)
				println(k)
			}	    	
	    }
	}   
}
