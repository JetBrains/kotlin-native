package codegen.controlflow.for_loops_lowering.test28

import kotlin.test.*

@Test fun runTest() {
	val arr = arrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
	val arr1 = arrayOf("a", "hello world", "aaa")
	for (i in 0 .. 10 step 2) {
		for (j in arr) {
			for (k in arrayOf(10.0)) {
				for (t in arr1) {
					println(j)
					println(k)
					println(t)
				}			
			}	    	
	    }
	}   
}
