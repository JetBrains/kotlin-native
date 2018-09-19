[![official project](http://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Kotlin/Native  #

_Kotlin/Native_ is an LLVM backend for the Kotlin compiler, runtime
implementation, and native code generation facility using the LLVM toolchain.

 _Kotlin/Native_ is primarily designed to allow compilation for platforms where
virtual machines are not desirable or possible (such as iOS or embedded targets),
or where a developer is willing to produce a reasonably-sized self-contained program
without the need to ship an additional execution runtime.

Prerequisites:
*   install JDK for your platform, instead of JRE. The build requires ```tools.jar```, which is not included in JRE;
*   on macOS install Xcode 10.0
*   on Fedora 26+ ```yum install ncurses-compat-libs``` may be needed

To compile from sources use following steps:

First, download dependencies:

	./gradlew dependencies:update

Then, build the compiler and libraries:

	./gradlew bundle

The build can take about an hour on a Macbook Pro.
To run a shorter build with only the host compiler and libraries, run:

    ./gradlew dist distPlatformLibs

To include Kotlin compiler in [composite build](https://docs.gradle.org/current/userguide/composite_builds.html) and build
against it, use the `kotlinProjectPath` project property:

    ./gradlew dist -PkotlinProjectPath=path/to/kotlin/project

After that, you should be able to compile your programs like this:

    export PATH=./dist/bin:$PATH
	kotlinc hello.kt -o hello

For an optimized compilation, use `-opt`:

	kotlinc hello.kt -o hello -opt

For tests, use:

	./gradlew backend.native:tests:run

To generate interoperability stubs, create a library definition file
(refer to [`samples/tetris/.../sdl.def`](https://github.com/JetBrains/kotlin-native/blob/master/samples/tetris/src/main/c_interop/sdl.def)), and run the `cinterop` tool like this:

    cinterop -def lib.def

See the provided [samples](https://github.com/JetBrains/kotlin-native/tree/master/samples) and [`INTEROP.md`](https://github.com/JetBrains/kotlin-native/blob/master/INTEROP.md) for more details.

The Interop tool generates a library in the `.klib` library format. See [`LIBRARIES.md`](https://github.com/JetBrains/kotlin-native/blob/master/LIBRARIES.md)
for more details on this file format.
