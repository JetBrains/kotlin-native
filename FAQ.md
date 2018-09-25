### Q: How do I run my program?

A: Define a top level function `fun main(args: Array<String>)` or just  `fun main()` if you are not interested
in passed arguments, please ensure it's not in a package.
Also compiler switch `-entry` could be used to make any function taking `Array<String>` or no arguments
and return `Unit` as an entry point.


### Q: What is Kotlin/Native memory management model?

A: Kotlin/Native provides an automated memory management scheme, similar to what Java or Swift provides.
The current implementation includes an automated reference counter with a cycle collector to collect cyclical
garbage.


### Q: How do I create a shared library?

A: Use the `-produce dynamic` compiler switch, or `konanArtifacts { dynamic('foo') {} }` in Gradle.
It will produce a platform-specific shared object (.so on Linux, .dylib on macOS, and .dll on Windows targets) and a
C language header, allowing the use of all public APIs available in your Kotlin/Native program from C/C++ code.
See `samples/python_extension` for an example of using such a shared object to provide a bridge between Python and
Kotlin/Native.


### Q: How do I create a static library or an object file?

A: Use the `-produce static` compiler switch, or `konanArtifacts { static('foo') {} }` in Gradle.
It will produce a platform-specific static object (.a library format) and a C language header, allowing you to
use all the public APIs available in your Kotlin/Native program from C/C++ code.


### Q: How do I run Kotlin/Native behind a corporate proxy?

A: As Kotlin/Native needs to download a platform specific toolchain, you need to specify
`-Dhttp.proxyHost=xxx -Dhttp.proxyPort=xxx` as the compiler's or `gradlew` arguments,
or set it via the `JAVA_OPTS` environment variable.


### Q: How do I specify a custom Objective-C prefix/name for my Kotlin framework?

A: Use the `-module_name` compiler option or matching Gradle DSL statement, i.e.

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
framework("MyCustomFramework") {
    extraOpts '-module_name', 'TheName'
}
```

</div>

### Q: Why do I see `InvalidMutabilityException`?

A: It likely happens, because you are trying to mutate a frozen object. An object can transfer to the
frozen state either explicitly, as objects reachable from objects on which the `kotlin.native.concurrent.freeze` is called,
or implicitly (i.e. reachable from `enum` or global singleton object - see the next question).


### Q: How do I make a singleton object mutable?

A: Currently, singleton objects are immutable (i.e. frozen after creation), and it's generally considered
good practise to have the global state immutable. If for some reason you need a mutable state inside such an
object, use the `@konan.ThreadLocal` annotation on the object. Also the `kotlin.native.concurrent.AtomicReference` class could be
used to store different pointers to frozen objects in a frozen object and automatically update them.

### Q: How can I compile my project against the Kotlin/Native master?

A: We release dev builds frequently, usually at least once a week. You can check the [list of available versions](https://bintray.com/jetbrains/kotlin-native-dependencies/kotlin-native-gradle-plugin). But if we recently fixed an issue and you want to check it before a release is done, you can do:

<details>
    
<summary>For the CLI, you can compile using gradle as stated in the README (and if you get errors, you can try to do a <code>./gradlew clean</code>):</summary>

<div class="sample" markdown="1" theme="idea" mode="shell">

```bash
./gradlew dependencies:update
./gradlew dist distPlatformLibs
```

</div>


You can then set the `KONAN_HOME` env variable to the generated `dist` folder in the git repository.

</details>

<details>
<summary>For Gradle, you can use <a href="https://docs.gradle.org/current/userguide/composite_builds.html">Gradle composite builds</a> like this:</summary>

```
# Set with the path of your kotlin-native clone
export KONAN_REPO=$PWD/../kotlin-native

# Run this once since it is costly, you can remove the `clean` task if not big changes were made from the last time you did this
pushd $KONAN_REPO && git pull && ./gradlew clean dependencies:update dist distPlatformLibs && popd

# In your project, you set have to the konan.home property, and include as composite the shared and gradle-plugin builds
./gradlew check -Pkonan.home=$KONAN_REPO/dist --include-build $KONAN_REPO/shared --include-build $KONAN_REPO/tools/kotlin-native-gradle-plugin
```

</details>
