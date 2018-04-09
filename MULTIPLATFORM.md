# Kotlin/Native in multiplatform projects

While Kotlin/Native could be used as the only Kotlin compiler in the project, it is pretty common to combine Kotlin/Native with other Kotlin backends, such as Kotlin/JVM (for JVM or Android targets) or Kotlin/JS (for web and Node.js applications). This document describes recommended approaches and the best practices for such scenarios.

Kotlin as a language provides a notion of expect/actual declarations, and Gradle, as an official Kotlin build system augments it with the notion of multiplatform projects (aka MPP). Those two, combined together, provide a flexible standartized [mechanism of mutliplatform development](https://kotlinlang.org/docs/reference/multiplatform.html) across various Kotlin flavours.

Code, common amongst multiple platforms can be placed in common modules, while platform-specific code could be placed into platform-specific modules, and expect/actual declarations can bind them together in developer-friendly way.

One can find a step-by-step tutorial of creating a Kotlin multiplatform application for Android and iOS.

## Creating multiplatform Android/iOS application with Kotlin

To create MPP application one has start with clear understanding which parts of an application is common for a different targets, and which ones are specific, and organize module structure accordingly. For shared Kotlin code the common ground consist of the Kotlin's standard library, which does include basic data structures and computational primitives, along with expect classes with platform-specific implementation. Most frequently, such code consists of GUI, input-output, cryptography and other APIs, available on the particular platform.

In this tutorial, the multiplatform application will include three parts:

 * An **Android application** represented by a separate Android Studio project written in pure Kotlin
 * An **iOS application** represented by a separate XCode project, written in Swift and using Kotlin 
 * A **multiplatform library** represented by a separate Gradle-build and containing a business logic of the application.
   This library can contain both platform-dependent and platform-independent code and is compiled into a `jar`-library
   for Android and in a `Framework` for iOS.

In its turn, the multiplatform library will include three subprojects:

 * `common` - contains a common logic for both applications;
 * `ios` - contains an iOS-specific code;
 * `android` - contains an Android-specific code.

*TODO: link to the calculator project*

### 1. Preparing a workspace

Represent the structure described above as a directory tree. Assume that our multiplatform library is intended to generate different greetings on different platform. Create the following directory structure:

    application/
    ├── androidApp/
    ├── iosApp/
    └── greeting/
        ├── common/
        ├── android/
        └── ios/

Now we have a basic structure of the multiplatform application and can proceed to implementing of the multiplatform library.

**TODO: Some words about Gradle. + the Note paragraph in the next section should be here.**

### 2. Multiplatform library

*Note: It's highly recommended to use [Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
to run the build. To create the wrapper, just execute `gradle wrapper` in the `greeting` directory. After that you can
use `./gradlew` to run the build instead of using your local Gradle installation.*

The multiplatform library is build using Gradle so the first thing we need to do is to specify the structure of the
project. To do this, create `settings.gradle` and declare in it all the subprojects:

    include ':common'
    include ':android'
    include ':ios'

Now Gradle knows how our library is organized and can work with it. Let's write some code and build logic.
Create `build.gradle` in the `greeting` directory and add into it the following snippet:

    // Set up a buildscript dependency on the Kotlin plugin.
    buildscript {
        // Specify a Kotlin version you need.
        ext.kotlin_version = '1.2.31'

        repositories {
            google()
            jcenter()
            maven { url "https://dl.bintray.com/jetbrains/kotlin-native-dependencies" }
        }

        // Specify all the plugins used as dependencies
        dependencies {
            classpath 'com.android.tools.build:gradle:3.1.0'
            classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
            classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.6.2"

        }
    }

    // Set up compilation dependency repositories for all projects.
    subprojects {
        repositories {
            google()
            jcenter()
        }
    }

Now all subprojects of the library can use Kotlin plugins.

#### 2.1 Common subproject

The `common` subproject contains a platform-independent code. Let's create `common/src/main/kotlin/common.kt` and
add some functionality into it:

    // greeting/common/src/main/kotlin/common.kt
    package org.greeting

    expect class Platform() {
        val platform: String
    }

    class Greeting {
        fun greeting(): String = "Hello, ${Platform().platform}"
    }


Here we create a simple class using `expect`/`actual` paradigm. See details about platform-specific declarations
[here](https://kotlinlang.org/docs/reference/multiplatform.html#platform-specific-declarations) **TODO: more details here?**

To build this common project add the following snippet in `common/build.gradle`:

    apply plugin: 'kotlin-platform-common'

    // Specify a group and a version of the library to access it in Android Studio.
    group = 'org.greeting'
    version = 1.0

    dependencies {
        // Set up compilation dependency on common Kotlin stdlib
        compile "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin_version"
    }

#### 2.2 Android subproject

The `android` subproject contains platform-dependent implementations of `expect`-declarations we've created in the
`common` project. Let's implement our `expect`-class:

    // greeting/android/src/main/kotlin/android.kt
    package org.greeting

    actual class Platform actual constructor() {
        actual val platform: String = "Android"
    }

We build this project into a Java-library which can be used by Android Studio project as a dependency. So the content
of `android/build.gradle` will be the following:

    apply plugin: 'java-library'
    apply plugin: 'kotlin-platform-jvm'

    // Specify a group and a version of the library to access it in Android Studio.
    group = 'org.greeting'
    version = 1.0

    dependencies {
        // Specify Kotlin/JVM stdlib dependency.
        implementation "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"

        // Specify dependency on a common project for Kotlin multiplatform build.
        expectedBy project(':common')
    }

#### 2.3 iOS subproject

As well as `android`, this project contains platform-dependent implementations of `expect`-declarations:

    // greeting/ios/src/main/kotlin/ios.kt
    package org.greeting

    actual class Platform actual constructor() {
        actual val platform: String = "iOS"
    }

We build this project into an Objective-C framework using Kotlin/Native compiler. To do this, declare a framework in
`ios/build.gradle` and add an `expectedBy` dependency in the same manner as in the Android project:

    apply plugin: 'konan'

    // Specify targets to build the framework: iOS and iOS simulator
    konan.targets = ['iphone', 'iphone_sim']

    konanArtifacts {
        // Declare building into a framework.
        framework('iosGreeting') {
            // The multiplatform support is disabled by default.
            enableMultiplatform true
        }
    }

    dependencies {
        // Specify dependency on a common project for Kotlin multiplatform build
        expectedBy project(':common')
    }

### 3. Android application

Now we can create an Android application which will use the library we implemented on the previous step. Open Android
Studio and create a new project in the `androidApp` directory. Android Studio generates all necessary files and
directories so we only need to add a dependency on our library. There are 2 actions we need to do:

1. Add dependency on the library. To do this just open `androidApp/app/build.gradle` and add the following snippet in
the `dependencies` script block:

    ```
    implementation 'org.greeting:android:1.0'
    implementation 'org.greeting:common:1.0'
    ```
2. Include `greeting` build in the Android Studio project as a part of
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html). To do this, add the
following line in `androidApp/settings.gradle`:

    ```
    includeBuild '../greeting'
    ```
    Now dependencies of the application can be resolved in artefacts built by `greeting`, so we can access our library.

**TODO: What is better: composite build or adding `greeting` as a subproject in the AS project?**

### 4. iOS application

As said above the multiplatform library can also be used in iOS applications. The general approach here is the same as
in case of an Android application: we create a separate XCode project and add the library as a Framework. But we need
to make some additional steps here.

Unlike Android Studio XCode doesn't use Gradle, so we cannot just add the library as a dependency. Instead we need to
create a new Framework in the XCode project and then replace its default build phases with a custom one that delegates
building the framework to Gradle.

To do this, make the following steps:

1. Create a new XCode project using `iosApp` as a root directory for it.
2. Add a new framework in the project. Go `File` -> `New` -> `Target` -> `Cocoa Touch Framework`. Specify a name of the
framework added, e.g. `iosGreeting`.
3. Choose the new framework in the `Project Navigator` and open `Build Settings` tab. Here we need to add a new build
setting specifying what Gradle task will be executed to build the framework for one or another platform. Create this
build setting in the `User-defined` section, name it `KONAN_TASK` and specify the following values for it depending on
the platform:
    * For any iOS simulator (both debug and release): `compileKonan<framework name>Iphone_sim`
    * For any iOS device (both debug and release): `compileKonan<framework name>Iphone`

    Replace `<framework name>` with the name you specified in the library's `ios/build.gradle`. Use camel case, e.g.
    for our `greeting` library these tasks will be named `compileKonanIosGreetingIphone_sim` and
    `compileKonanIosGreetingIphone`.
4. Select the `Build phases` tab and remove all default phases except `Target Dependencies`.
5. Add a new `Run Script` build phase and put the following line into the script field:

    ```
    "$SRCROOT/../greeting/gradlew" -p "$SRCROOT/../greeting/ios" "$KONAN_TASK" --no-daemon -Pkonan.useEnvironmentVariables=true
    ```
    
    This script executes the Gradle build to compile the multiplatform library into a Framework. Let's examine this command in more detail.
    * `"$SRCROOT/../greeting/gradlew"` - here we invoke the Gradle wrapper located in `greeting`. If you use a local
    Gradle installation you need to invoke it instead of the wrapper.
    * `-p "$SRCROOT/../greeting/ios"` - specify a path to the Gradle subproject containing the framework.
    * `"$KONAN_TASK"` - specify a Gradle task to execute. The build setting created above is used here.
    * `--no-daemon` - disable the [Gradle daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html). This
    setting allows us to workaround [this issue](https://github.com/gradle/gradle/issues/3468) related to a build
    environment in Java 9. If you have Java 8 or earlier you may omit this flag.
    * `-Pkonan.useEnvironmentVariables=true` - enable passing build parameters from XCode to Kotlin/Native Gradle
    plugin via environment variables.

6. Add Kotlin sources into the framework: run `File` -> `Add files to "iosApp"...` and choose a directory with
Kotlin sources (`greeting/ios/src` in this sample). Do this for the common code of the library too.

*TODO: Simple snippet showing how to call the library in swift*