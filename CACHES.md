# Library caching
Compiler caches, introduced in the 1.3.70 version of K/N, is an internal feature that aims to improve compilation time.


#### 
Currently, the Gradle plugin compilation of native targets consists of two phases:
* Compilation task, defined by default as `compileKotlin<Test><target-name>`.
* Native artifact production task, named as `link<Release | Debug><artifact-type><target-name>`. 

The first phase gets all Kotlin source files and dependency libraries and compiles it
to an intermediate representation. It is a Kotlin library, produced by a kotlinc-native compiler with `-p library` flag set.
This representation does not determine the final binary kind,
 so this stage and its product are shared between an executable and static library, for example.
 
The second phase utilizes that library and produces a native binary artifact, which kind was 
specified in the build.gradle<.kts> script.

The problem is that whenever the code gets changed, the whole result of the first phase has to be recompiled.
This is an excessive move because presumably none of the dependencies will actually get changed.
Then, it would be smart to redistribute the load between phases,
 so the dependencies will participate only in the second phase, being linked with the final artifact. 

This approach should make the whole compilation pipeline faster, as soon as libraries should be cached only once.

#### Cache

To achieve this, a KLIB (Kotlin library) that should be cached gets a native library with all contents compiled.
Kind of this native library can be chosen as Kotlin/Native compiler can generate both dynamic and static libraries
by its nature.

One should keep in mind, that dynamic caching follows the rules of any custom dynamic library use, such as the necessity to distribute it across with the binary.
#### To use
Gradle multiplatform plugin version 1.3.70 and later provides this feature by default. 
For now, caching is used only for the standard library. In its current state, the plugin uses static caches. 
To adjust this setting, use ```kotlin.native.cacheKind = <dynamic | static | none>``` Gradle property.
