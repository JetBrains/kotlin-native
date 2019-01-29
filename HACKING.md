## Profiling the compiler

### Profiling with YourKit

Install the YourKit profiler for your platform from https://www.yourkit.com/java/profiler.
Set AGENT variable to the JVMTI agent provided by YourKit.

        export AGENT=/Applications/YourKit-Java-Profiler-2018.04.app/Contents/Resources/bin/mac/libyjpagent.jnilib

To profile standard library compilation:

        ./gradlew -PstdLibJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  dist

To profile platform libraries start build of proper target like this:

        ./gradlew -PplatformLibsJvmArgs="-agentpath:$AGENT=probe_disable=*,listen=all,tracing"  ios_arm64PlatformLibs

To profile standalone code compilation use:

        JAVA_OPTS="-agentpath:$AGENT=probe_disable=*,listen=all,tracing" ./dist/bin/konanc file.kt

Then attach to the desired application in YourKit GUI and use CPU tab to inspect CPU consuming methods.
Saving the trace may be needed for more analysis. Adjusting `-Xmx` in `$HOME/.yjp/ui.ini` could help
with the big traces.

To perform memory profiling follow the steps above, and after attachment to the running process
use "Start Object Allocation Recording" button. See https://www.yourkit.com/docs/java/help/allocations.jsp for more details.

 ## Compiler Gradle options

There are several gradle flags one can use for Konan build.

* **-Pbuild_flags** passes flags to the compiler used to build stdlib

        ./gradlew -Pbuild_flags="--disable lower_inline --print_ir" stdlib

* **-Pshims** compiles LLVM interface with tracing "shims". Allowing one 
    to trace the LLVM calls from the compiler.
    Make sure to rebuild the project.

        ./gradlew -Pshims=true dist

 ## Compiler environment variables

* **KONAN_DATA_DIR** changes `.konan` local data directory location (`$HOME/.konan` by default). Works both with cli compiler and gradle plugin

 ## Testing

To run blackbox compiler tests from JVM Kotlin use (takes time):

    ./gradlew run_external

To update the blackbox compiler tests set TeamCity build number in `gradle.properties`:

    testKotlinVersion=<build number>

and run `./gradlew update_external_tests`

* **-Pfilter** allows one to choose test files to run.

        ./gradlew -Pfilter=overflowLong.kt run_external

* **-Pprefix** allows one to choose external test directories to run. Only tests from directories with given prefix will be executed.

        ./gradlew -Pprefix=external_codegen_box_cast run_external

* **-Ptest_flags** passes flags to the compiler used to compile tests

        ./gradlew -Ptest_flags="--time" backend.native:tests:array0

* **-Ptest_target** specifies cross target for a test run. 

        ./gradlew -Ptest_target=raspberrypi backend.native:tests:array0

* **-Premote=user@host** sets remote test execution login/hostname. Good for cross compiled tests.

        ./gradlew -Premote=kotlin@111.22.33.444 backend.native:tests:run

* **-Ptest_verbose** enables printing compiler args and other helpful information during a test execution.

        ./gradlew -Ptest_verbose :backend.native:tests:mpp_optional_expectation