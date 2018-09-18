# Kotlin/Native in multiplatform projects

While Kotlin/Native can be used as the only Kotlin compiler in a project, it is pretty common to combine
Kotlin/Native with other Kotlin backends, such as Kotlin/JVM (for JVM or Android targets) or Kotlin/JS
(for web and Node.js applications). This document describes the recommended approaches and best practices for such scenarios.

Kotlin as a language provides a notion of expect/actual declarations, and Gradle in turn
augments it with the notion of multiplatform projects (aka MPP). These two, combined together, provide a flexible
standardized [mechanism of multiplatform development](https://kotlinlang.org/docs/reference/multiplatform.html)
across various Kotlin flavors.

Code, common amongst multiple platforms can be placed in common modules, while platform-specific code can be placed
into platform-specific modules, and expect/actual declarations can bind them together in a developer-friendly way.

Below you will find a step-by-step tutorial for creating a Kotlin multiplatform application for Android and iOS.

## Creating multiplatform Android/iOS application with Kotlin

To create an MPP application you have to start with clear understanding of which parts of an application are common for different
targets, and which are specific, and then organize the module structure accordingly. For shared Kotlin code the common
ground consists of Kotlin's standard library, which does include basic data structures and computational primitives,
along with some expected classes with platform-specific implementation. Most frequently, such code consists of a GUI,
input-output, cryptography, and other APIs, available on the particular platform.

In this tutorial, the multiplatform application will include three parts:

 * An **Android application** represented by a separate Android Studio project written in Kotlin.
 * An **iOS application** represented by a separate Xcode project, written in Swift.
 * A **multiplatform library** containing the business logic of the application and used by both Android and iOS applications.
   This library can contain both platform-dependent and platform-independent code and is compiled into a `jar`-library
   for Android and in a `Framework` for iOS by Gradle.

So, the multiplatform library will include three subprojects:

 * `common` - contains common logic for both applications
 * `ios` - contains iOS-specific code
 * `android` - contains Android-specific code

### 1. Preparing a workspace

Let's represent the structure described above as a directory tree. Assume that our multiplatform library is intended to
generate different greetings on different platforms. Create the following directory structure:

    application/
    ├── androidApp/
    ├── iosApp/
    └── greeting/
        ├── common/
        ├── android/
        └── ios/

As said above, [Gradle](https://gradle.org/) is the main build system for Kotlin so our project will use it.

To install Gradle refer to [these instructions](https://gradle.org/install/). Despite being able to use the local
Gradle installation for building a project, it's recommended to use the
[Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html) instead. To create the wrapper, install
Gradle as described above, and execute `gradle wrapper` in the root directory of the project. After that you can
use `./gradlew` to run the build instead of using your local Gradle installation.

Once the wrapper is created we need to describe the project structure in Gradle terms. To do this, create
a `settings.gradle` file in the root directory of the project and put the following snippet into it:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
include ':greeting'
include ':greeting:common'
include ':greeting:android'
include ':greeting:ios'
```

</div>

Here we declare all subprojects for our `greeting` multiplatform library. All other multiplatform libraries included
in the project also must be declared here.

Note that both iOS and Android applications are not included in the root Gradle build. They are represented by
independent builds which are managed by specific IDEs (Android Studio and Xcode). Such an approach makes working with
these builds easier from the IDEs.

As for an IDE for other parts of the project, [IntelliJ IDEA](https://www.jetbrains.com/idea/) is recommended.

> Note: Kotlin/Native is not supported by IntelliJ IDEA so the only IDE to develop Kotlin/Native sub-projects is
[CLion](https://www.jetbrains.com/clion/). But at the moment CLion has no Gradle integration. As a workaround you can
create a CLion Cmake project from a Kotlin/Native Gradle one. Just run `./gradlew generateCMake` for this project. It
will generate all the necessary files which are required. See
[this](https://blog.jetbrains.com/kotlin/2017/11/kotlinnative-ide-support-preview/) blog post to learn more about
Kotlin/Native support in CLion.

For the final step create the empty `build.gradle` files in the root directory of the project and in all subprojects which are
included in `settings.gradle`. After performing all these actions the project structure will be the following (files
generated by the Gradle wrapper are not shown):

    application/
    ├── androidApp/
    ├── iosApp/
    ├── greeting/
    │   ├── android/
    │   │   └── build.gradle
    │   ├── common/
    │   │   └── build.gradle
    │   ├── ios/
    │   |   └── build.gradle
    |   └── build.gradle
    ├── build.gradle
    └── settings.gradle

Now we have the basic structure of the project and can proceed to implement the multiplatform library.

### 2. Multiplatform library

We need to add buildscript dependencies to be able to use the Kotlin plugins for Gradle in our build. Open
the `build.gradle` in the `greeting` directory and put the following snippet into it:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
// Set up a buildscript dependency on the Kotlin plugin.
buildscript {
    // Specify a Kotlin version you need.
    ext.kotlin_version = '1.2.41'

    repositories {
        jcenter()
        maven { url "https://dl.bintray.com/jetbrains/kotlin-native-dependencies" }
    }

    // Specify all the plugins used as dependencies
    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlin:kotlin-native-gradle-plugin:0.7"

    }
}

// Set up compilation dependency repositories for all projects.
subprojects {
    repositories {
        jcenter()
    }
}
```

</div>


Now all subprojects of the library can use Kotlin plugins.

#### 2.1 Common subproject

The `common` subproject contains platform-independent code. To build it, add the following snippet in `common/build.gradle`:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
apply plugin: 'kotlin-platform-common'

// Specify a group and a version of the library to access it in Android Studio.
// By default the project directory name is used as an artifact name thus the full dependency
// description will be 'org.greeting:common:1.0'
group = 'org.greeting'
version = 1.0

dependencies {
    // Set up a compilation dependency on common Kotlin stdlib
    implementation "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlin_version"
}
```

</div>

Now we can write some logic available for all platforms. Create `common/src/main/kotlin/common.kt` and add some
functionality into it:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
// greeting/common/src/main/kotlin/common.kt
package org.greeting

expect class Platform() {
    val platform: String
}

class Greeting {
    fun greeting(): String = "Hello, ${Platform().platform}"
}
```

</div>

Here we create a simple class using the `expect`/`actual` paradigm. Find details about platform-specific declarations
[here](https://kotlinlang.org/docs/reference/multiplatform.html#platform-specific-declarations).

#### 2.2 Android subproject

The `android` subproject contains platform-dependent implementations of the `expect`-declarations we've created in the
`common` project. We compile it into a Java library which an Android Studio project can depend on. The content
of the `android/build.gradle` will be the following:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
apply plugin: 'kotlin-platform-jvm'

// Specify a group and a version of the library to access it in Android Studio.
// By default the project directory name is used as an artifact name thus the full dependency
// description will be 'org.greeting:android:1.0'
group = 'org.greeting'
version = 1.0

dependencies {
    // Specify Kotlin/JVM stdlib dependency.
    implementation "org.jetbrains.kotlin:kotlin-stdlib-jre7:$kotlin_version"

    // Specify dependency on a common project for Kotlin multiplatform build.
    expectedBy project(':greeting:common')
}
```

</div>

As mentioned above this subproject should include actual implementations of the common project's `expect`-declarations.
Let's write an Android-specific method:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
// greeting/android/src/main/kotlin/android.kt
package org.greeting

actual class Platform actual constructor() {
    actual val platform: String = "Android"
}
```

</div>

#### 2.3 iOS subproject

This project is compiled into an Objective-C framework using the Kotlin/Native compiler. To do this, declare a framework in
`ios/build.gradle` and add an `expectedBy` dependency in the same manner as was done in the Android project:

<div class="sample" markdown="1" theme="idea" mode="groovy">

```groovy
apply plugin: 'konan'

// Specify targets to build the framework: iOS and iOS simulator
konan.targets = ['ios_arm64', 'ios_x64']

konanArtifacts {
    // Declare building into a framework.
    framework('Greeting') {
        // The multiplatform support is disabled by default.
        enableMultiplatform true
    }
}

dependencies {
    // Specify dependency on a common project for Kotlin multiplatform build
    expectedBy project(':greeting:common')
}
```

</div>

As well as `android`, this project contains platform-dependent implementations of `expect`-declarations:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
// greeting/ios/src/main/kotlin/ios.kt
package org.greeting

actual class Platform actual constructor() {
    actual val platform: String = "iOS"
}
```

</div>


### 3. Android application

Now we can create an Android application which will use the library we implemented in the previous step. Open Android
Studio and create a new project in the `androidApp` directory. Android Studio will generate all the necessary files and
directories.

Kotlin/Native requires Gradle 4.7 or higher so you need to make sure that the AS project uses the correct
Gradle version. To do this, open `androidApp/gradle/gradle-wrapper.properties` and check the `distributionUrl`
property. Upgrade the wrapper if necessary
(see [Gradle documentation](https://docs.gradle.org/current/userguide/gradle_wrapper.html#sec:upgrading_wrapper)).
 
Now we only need to add a dependency on our library. There are 2 actions we need to take:

1. Add dependency on the library. To do this just open `androidApp/app/build.gradle` and add the following snippet in
the `dependencies` script block:

    ```
    implementation 'org.greeting:android:1.0'
    ```
2. Include `greeting` build in the Android Studio project as a part of
[composite build](https://docs.gradle.org/current/userguide/composite_builds.html). To do this, add the
following line in `androidApp/settings.gradle`:

    ```
    includeBuild '../'
    ```
    Now dependencies of the application can be resolved in artifacts built by `greeting`. You also may publish the
    Android part of `greeting` into some Maven repo and get it from there. In this case you don't need to set up
    the composite build.

> Note: Android Studio may fail to resolve declarations from the library added unless it's built. If you face such a
> problem, build the library by executing `./gradlew greeting:android:jar` in the root directory of the project.

> Alternatively you can add the multiplatform library subprojects right into the Android Studio one instead of
> creating a composite build. To do this you need to declare them along with their directories in
> `androidApp/settings.gradle`:
>
><div class="sample" markdown="1" theme="idea" mode="groovy">
>
>```groovy
>include ':greeting'
>include ':greeting:common'
>include ':greeting:android'
>
>project(':greeting').projectDir = file('../greeting')
>project(':greeting:common').projectDir = file('../greeting/common')
>project(':greeting:android').projectDir = file('../greeting/android')
>```
>
></div>
>
> Now you can declare dependencies directly on projects instead of using maven-like coordinates:
>
><div class="sample" markdown="1" theme="idea" mode="groovy">
>
>```groovy
>implementation project(':greeting:android')
>```
>
></div>

After these steps we can access our library as we would with any other Kotlin code:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import org.greeting.*

/* ... */

fun foo() {
    println(Greeting().greeting())
}
```

</div>


### 4. iOS application

As already mentioned above the multiplatform library can also be used in iOS applications. The general approach here is the same as
in the case of the Android application: we create a separate Xcode project and add the library as a framework. But we need
to make some additional steps here.

Unlike Android Studio Xcode doesn't use Gradle, so we cannot just add the library as a dependency. Instead we need to
create a new framework in the Xcode project and then replace its default build phases with a custom one which delegates
building the framework to Gradle.

To do this, perform the following steps:

1. Create a new Xcode project in the root directory of your project (the `application` directory in the
[section 1](#1-preparing-a-workspace)). Name it `iosApp` so Xcode will create the project in the directory we created
in the section 1.
2. Add a new framework in the project. Go to `File` -> `New` -> `Target` -> `Cocoa Touch Framework`. Specify the same
framework name as is in `greeting/ios/build.gradle`: `Greeting`.
3. Choose the new framework in the `Project Navigator` and open the `Build Settings` tab. Here we need to add a new build
setting specifying what Gradle task will be executed to build the framework for one or another platform. Fortunately,
Xcode allows us to set different values for the same build setting depending on the platform. Create a new build
setting in the `User-defined` section and name it `KONAN_TASK`. Then specify the following values for it for different
platforms (for both `Debug` and `Release` modes):

    |Platform               |Value                                  |
    |-----------------------|---------------------------------------|
    |`Any iOS simulator SDK`|`compileKonan<framework name>Ios_x64`  |
    |`Any iOS SDK`          |`compileKonan<framework name>Ios_arm64`|

    Replace `<framework name>` with the name you specified in the library's `ios/build.gradle`. Use camel case, e.g.
    for our `greeting` library these tasks will be named `compileKonanGreetingIos_x64` and
    `compileKonanGreetingIos_arm64`.
4. Add one more build setting for the framework to manage optimizations performed by the Kotlin/Native compiler. Name
it `KONAN_ENABLE_OPTIMIZATIONS ` and set its value to `YES` for the `Release` mode and to `NO` for the `Debug` mode.
5. Ensure that the framework is still selected in the `Project Navigator` and open the `Build phases` tab. Remove all
the default phases except `Target Dependencies`.
6. Add a new `Run Script` build phase and put the following code into the script field:

    ```
    "$SRCROOT/../gradlew" -p "$SRCROOT/../greeting/ios" "$KONAN_TASK" \
    -Pkonan.configuration.build.dir="$CONFIGURATION_BUILD_DIR"        \
    -Pkonan.debugging.symbols="$DEBUGGING_SYMBOLS"                    \
    -Pkonan.optimizations.enable="$KONAN_ENABLE_OPTIMIZATIONS"
    ```

    This script executes the Gradle build to compile the multiplatform library into a framework. Let's examine this
    command in more detail.
    * `"$SRCROOT/../gradlew"` - here we invoke the Gradle wrapper located in the root directory of the project. If you
    use a local Gradle installation you need to invoke it instead of the wrapper.
    * `-p "$SRCROOT/../greeting/ios"` - specify a path to the Gradle subproject containing the framework.
    * `"$KONAN_TASK"` - specify a Gradle task to execute. The build setting created above is used here.
    * `-Pkonan.configuration.build.dir="$CONFIGURATION_BUILD_DIR"` - specify a directory provided by Xcode as an output one.
    * `-Pkonan.debugging.symbols="$DEBUGGING_SYMBOLS"` - allow Xcode to enable debugging symbols generation.
    * `-Pkonan.optimizations.enable="$KONAN_ENABLE_OPTIMIZATIONS"` - disable/enable optimizations. The build setting
    created above is used here.

7. Add Kotlin sources into the framework: run `File` -> `Add files to "iosApp"...` and choose a directory with
Kotlin sources (`greeting/ios/src` in this sample). Choose the framework created as a target to add these sources to.
Do this for the common code of the library too.

Now the framework is added and all the Kotlin API are available from Swift code (note that you need to build the
framework in order to get code completion). Let's print our greeting:

<div class="sample" markdown="1" theme="idea" mode="swift">

```swift
import Greeting

/* ... */

func foo() {
    print(GreetingGreeting().greeting())
}
```

</div>

### Sample

A sample implementation which follows this documentation can be found [here](https://github.com/JetBrains/kotlin-mpp-example).
You may also look at the [calculator sample](https://github.com/JetBrains/kotlin-native/tree/master/samples/calculator). It has a simpler structure (particularly both Android app
and Kotlin/Native library are combined in a single Gradle build) but also uses the multiplatform support provided by Kotlin. 
