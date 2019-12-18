# v1.3.70 (Dec 2019)
  * Support compiler caches for debug mode (GH-3650)
  * Support running Kotlin/Native compiler from Gradle daemon (GH-3442)
  * Support multiple independent Kotlin frameworks in the same application (GH-3457)
  * Compile-time allocation for some singleton objects (GH-3645)
  * Native support for SIMD vector types in compiler and interop (GH-3498)
  * API for runtime detector of cyclic garbage (GH-3616)
  * Commonized StringBuilder (GH-3593) and Float.rangeTo (KT-35299)
  * Fix interop with localized strings (GH-3562)
  * Provide utility for user-side generation of platform libraries (GH-3538)
  * On-stack allocation using local escape analysis (GH-3625)
  * Code coverage support on Linux and Windows (GH-3403)
  * Debugging experience improvements (GH-3561, GH-3638, GH-3606)

# v1.3.60 (Oct 2019)
  * Support XCode 11
  * Switch to LLVM 8.0
  * New compiler targets:
    * watchOS targets, watchos_x86, watchos_arm64 and watchos_arm32 (GH-3323, GH-3404, GH-3344)
    * tvOS targets tvos_x64 and tvos_arm64 (GH-3303, GH-3363)
    * native Android targets android_x86 and android_x64 (GH-3306, GH-3314)
  * Standard CLI library kotlinx.cli is shipped with the compiler distribution (GH-3215)
  * Improved debug information for inline functions (KT-28929, GH-3292)
  * Improved runtime performance of interface calls, up to 5x faster (GH-3377)
  * Improved runtime performance of type checks, up to 50x faster (GH-3291)
  * Produce native binaries directly from klibs, speeds up large project compilation (GH-3246)
  * Supported arbitrary (up to 255 inclusive) function arity (GH-3253)
  * Supported callable references to suspend functions (GH-3197)
  * Implemented experimental -Xg0 switch, symbolication of release binaries for iOS (GH-3233, GH-3367)
  * Interop:
    * Allow passing untyped null as variadic function's parameter (GH-3312, KT-33525)
  * Standard library:
    * Allow scheduling jobs in arbitrary K/N context, not only Worker (GH-3316)
  * Important bug fixes:
    * Boxed negative values can lead to crashes on ios_arm64 (GH-3296)
    * Implemented thread-safe tracking of Objective-C references to Kotlin objects (GH-3267)

# v1.3.50 (Aug 2019)
  * Kotlin/Native versioning now aligned with Kotlin versioning
  * Exhaustive platform libraries on macOS (GH-3141)
  * Update to Gradle 5.5 (GH-3166)
  * Improved debug information correctness (GH-3130)
  * Major memory manager refactoring (GH-3129)
  * Embed actual bitcode in produced frameworks (GH-2974)
  * Compilation speed improvements
  * Interop:
    * Support kotlin.Deprecated when producing framework (GH-3114)
    * Ensure produced Objective-C header does not have warnings (GH-3101)
    * Speed up interop stub generator (GH-3082, GH-3050)
    * getOriginalKotlinClass() to get KClass for Kotlin classes in Objective-C (GH-3036)
    * Supported nullable primitive types in reverse C interop (GH-3198)
  * Standard library
    * API for delayed job execution on worker (GH-2971)
    * API for running via worker's job queue (GH-3078)
    * MonoClock and Duration support (GH-3028)
    * Support typeOf (KT-29917, KT-28625)
    * New zero-terminated utf8 to String conversion API (GH-3116)
    * Optimize StringBuilder for certain cases (GH-3202)
    * Implemented Array.fill API (GH-3244)

# v1.3.0 (Jun 2019)
  * CoreLocation platform library on macOS (GH-3041)
  * Converting Unit type to Void during producing framework for Objective-C/Swift (GH-2549, GH-1271)
  * Support linux/arm64 targets (GH-2917)
  * Performance improvements of memory manager (GH-2813)
  * FreezableAtomicReference prototype (GH-2776)
  * Logging and error messages enhancements 
  * Interop:
    * Support nullable String return type in reverse C interop (GH-2956)
    * Support setting custom exception hook in reverse C interop (GH-2941)
    * Experimental generics support for produced frameworks for Objective-C/Swift implemented by Kevin Galligan (GH-2850)
    * Improve support for Objective-C methods clashing with methods of Any (GH-2914)
    * Support variadic Objective-C functions (GH-2896)

# v1.2.1 (Apr 2019)
  * Fix Objective-C interop with React (GH-2872)
  * Fix “not in vtable” compiler crash when generating frameworks (GH-2865)
  * Implement some optimizations (GH-2854)
  * Fix release build for 32-bit Windows (GH-2848)
  * Fix casts to type parameters with multiple bounds (GH-2888)
  * Fix “could not get descriptor uniq id for deserialized class FlagEnum” compiler crash when generating framework (GH-2874)

# v1.2.0 (Apr 2019)
  * New intermediate representation based library format allowing global optimizations
  * Exception backtraces in debug mode on macOS and iOS targets contains symbolic information
  * Support for 32-bit Windows targets (target mingw_x86)
  * Support for cross-compilation to Linux (x86-64 and arm32) from macOS and Windows hosts
  * Static Apple frameworks can be produced
  * Support Gradle 5.1
  * Fix alignment-related issues on ARM32 and MIPS platforms
  * Write unhandled exceptions stacktrace on device to iOS crash log
  * Fix undefined behavior in some arithmetic operations
  * Interop:
    * Get rid of libffi dependency
    * Support returning struct from C callbacks
    * Support passing Kotlin strings to C interop functions accepting UTF-32 arguments
    * Fix bool conversion
    * Support variable length arrays
    * Provide Kotlin access to C compiler intrinsics via platform.builtins package
    * Support clang modules (for Objective-C only)
    * Experimental integration with CocoaPods
  * IDE
    * Kotlin/Native plugin is supported in CLion 2018.3 and AppCode/CLion 2019.1
    * Basic highlighting support for .def files
    * Navigation to source files from exception backtrace

## v1.1.0 (Dec 2018)
  * Performance optimizations:
    * runtime: optimization of queue of finalization
    * compiler: loop generation optimization
	* compiler: reduce RTTI size
	* runtime: reduce size of the object header
  * Contracts support
  * Regex engine: fix quantifier processing

## v0.9.3 (Sep 2018)
  * Bugfixes

## v0.9.2 (Sep 2018)
  * Support Xcode 10.0
  * iOS 9.0 is the minimal supported version for all targets
  * Swift interop improvements
  * Support shared top level values of some immutable types (i.e. String and atomic references)
  * Support release Kotlin 1.3.0

## v0.9.1 (Sep 2018)
  * Improve naming in produced Objective-C frameworks. Use ‘Kotlin’ prefix instead of ‘Stdlib’ prefix.
  * Improvements in KLIB: Library versioning, IDEA-friendly internal format.

# v0.9 (Sep 2018)
  * Support Kotlin 1.3M2
    * Note: Common modules of multiplatform projects also should use Kotlin 1.3
  * Major standard library (native parts) rework and rename
  * New Gradle plugin with multiplatform integration and reworked DSL
  * Support unsigned types in Kotlin and interop
  * Support non-experimental coroutines API (kotlin.coroutines)
  * Top level object var/val can only be accessed from the main thread
  * Support lazy properties in singleton objects
  * Update LLVM to 6.0.1

## v0.8 (Jul 2018)
  * Singleton objects are frozen after creation, and shared between threads
  * String and primitives types are frozen by default
  * Common stdlib with Kotlin/JVM and Kotlin/JS
  * Implemented `kotlin.random.*` and `Collection.shuffle`
  * Implemented atomic integers and atomic references
  * Multiple bugfixes in compiler (coroutines, inliner)
  * Support 32-bit iOS (target `ios_arm32`)
  * New experimental Gradle plugin
  * Support Xcode 9.4.1
  * Optimizations (switch by enum, memory management)

## v0.7.1 (Jun 2018)
  * Bugfixes in the runtime (indexOf, GC for kotlin.Array, enum equality) and the compiler
  * Fix NSBlock problem, preventing upload of binaries to the AppStore
  * Create primitive type boxes and kotlin.String as frozen by default
  * Support Gradle 4.7, provide separate run task for each executable
  * Support Xcode 9.4 and CoreML and ClassKit frameworks on Apple platforms
  * Improved runtime Kotlin variable examination
  * Minor performance optimizations in compiled code and runtime
  * Add `disableDesignatedInitializerChecks` definition file support

## v0.7 (May 2018)
  * Interop with Objective-C/Swift changes:
     * Uniform direct and reverse interops (values could be passed in both directions now)
     * Interop by exceptions
     * Type conversion and checks (`as`, `is`) for interop types
     * Seamless interop on numbers, strings, lists, maps and sets
     * Better interop on constructors and initializers
  * Switched to Xcode 9.3 on Apple platforms
  * Introduced object freezing API, frozen object could be used from multiple threads
  * Kotlin enums are frozen by default
  * Switch to Gradle 4.6
  * Use Gradle native dependency model, allowing to use `.klib` as Maven artifacts
  * Introduced typed arrays API
  * Introduced weak references API
  * Activated global devirtualization analysis
  * Performance improvements (box caching, bridge inlining, others)

## v0.6.2 (Mar 2018)
  * Support several `expectedBy`-dependencies in Gradle plugin.
  * Improved interaction between Gradle plugin and IDE.
  * Various bugfixes

## v0.6.1 (Mar 2018)
  * Various bugfixes
  * Support total ordering in FP comparisons
  * Interop generates string constants from string macrodefinitions
  * STM32 blinky demo in pure Kotlin/Native
  * Top level variables initialization redesign (proper dependency order)
  * Support kotlin.math on WebAssembly targets
  * Support embedded targets on Windows hosts

## v0.6 (Feb 2018)
  * Support multiplatform projects (expect/actual) in compiler and Gradle plugin
  * Support first embedded target (STM32 board)
  * Support Kotlin 1.2.20
  * Support Java 9
  * Support Gradle 4.5
  * Transparent Objective-C/Kotlin container classes interoperability
  * Produce optimized WebAssembly binaries (10x smaller than it used to be)
  * Improved APIs for object transfer between threads and workers
  * Allow exporting top level C function in reverse interop with @CName annotation
  * Supported debugging of code with inline functions
  * Multiple bugfixes and performance optimizations

## v0.5 (Dec 2017)
  * Reverse interop allowing to call Kotlin/Native code compiled as framework from Objective-C/Swift programs
  * Reverse interop allowing to call Kotlin/Native code compiled as shared object from C/C++ programs
  * Support generation of shared objects and DLLs by the compiler
  * Migration to LLVM 5.0
  * Support WebAssembly target on Linux and Windows hosts
  * Make string conversions more robust
  * Support kotlin.math package
  * Refine workers and string conversion APIs

## v0.4 (Nov 2017) ##
  * Objective-C frameworks interop for iOS and macOS targets
  * Platform API libraries for Linux, iOS, macOS and Windows
  * Kotlin 1.2 supported
  * `val` and function parameters can be inspected in debugger
  * Experimental support for WebAssembly (wasm32 target)
  * Linux MIPS support (little and big endian, mips and mipsel targets)
  * Gradle plugin DSL fully reworked
  * Support for unit testing annotations and automatic test runner generation
  * Final executable size reduced
  * Various interop improvements (forward declaration, better handling of unsupported types)
  * Workers object subgraph transfer checks implemented
  * Optimized low level memory management using more efficient cycle tracing algorithm

## v0.3.4 (Oct 2017) ##
  * Intermediate release

## v0.3.2 (Sep 2017) ##
  * Bug fixes

## v0.3.1 (Aug 2017) ##
  * Improvements in C interop tools (function pointers, bitfields, bugfixes)
  * Improvements to Gradle plugin and dependency downloader
  * Support for immutable data linked into an executable via ImmutableDataBlob class
  * Kotlin 1.1.4 supported
  * Basic variable inspection support in the debugger
  * Some performance improvements ("for" loops, memory management)
  * .klib improvements (keep options from .def file, faster inline handling)
  * experimental workers API added (see [`sample`](https://github.com/JetBrains/kotlin-native/blob/master/samples/workers))

## v0.3 (Jun 2017) ##
  * Preliminary support for x86-64 Windows hosts and targets
  * Support for producing native activities on 32- and 64-bit Android targets
  * Extended standard library (bitsets, character classification, regular expression)
  * Preliminary support for Kotlin/Native library format (.klib)
  * Preliminary source-level debugging support (stepping only, no variable inspection)
  * Compiler switch `-entry` to select entry point
  * Symbolic backtrace in runtime for unstripped binaries, for all supported targets

## v0.2 (May 2017) ##
  * Added support for coroutines
  * Fixed most stdlib incompatibilities
  * Improved memory management performance
  * Cross-module inline function support
  * Unicode support independent from installed system locales
  * Interoperability improvements
     * file-based filtering in definition file
     * stateless lambdas could be used as C callbacks
     * any Unicode string could be passed to C function
  * Very basic debugging support
  * Improve compilation and linking performance

## v0.1 (Mar 2017) ##
Initial technical preview of Kotlin/Native
