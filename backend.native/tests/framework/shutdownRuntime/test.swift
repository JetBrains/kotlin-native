import ShutdownRuntime

@_silgen_name("Kotlin_shutdownRuntime")
func Kotlin_shutdownRuntime()

func testShutdownRuntime() {
    KnlibraryKt.leakMemory()
    Kotlin_shutdownRuntime()
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        test("ShutdownRuntime", testShutdownRuntime)
    }
}
