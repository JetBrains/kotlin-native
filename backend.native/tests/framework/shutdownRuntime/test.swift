import ShutdownRuntime

@_silgen_name("Kotlin_ShutdownRuntime")
func Kotlin_ShutdownRuntime()

func testShutdownRuntime() {
    KnlibraryKt.ensureInitialized()
    Kotlin_ShutdownRuntime()
    Kotlin_ShutdownRuntime()
}

// -------- Execution of the test --------

class TestTests : SimpleTestProvider {
    override init() {
        super.init()

        test("ShutdownRuntime", testShutdownRuntime)
    }
}
