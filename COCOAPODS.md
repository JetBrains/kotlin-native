# CocoaPods integration

Kotlin/Native provides integration with the [CocoaPods dependency manager](https://cocoapods.org/). 
You can add dependencies on Pod libraries as well as use a multiplatform project with 
native targets as a CocoaPods dependency (Kotlin Pod).

You can manage Pod dependencies directly in IntelliJ IDEA and enjoy all the additional features such as code highlighting 
and completion. You can build the whole Kotlin project with Gradle and not ever have to switch to Xcode. 

Use Xcode only when you need to write Swift/Objective-C code or run your application on a simulator or device.
To work correctly with Xcode, you should [update you Podfile](#update-podfile-for-xcode). 

Depending on your project and purposes, you can add dependencies between [a Kotlin project and a Pod library](#add-dependencies-on-pod-libraries) as well as [a Kotlin Pod and an Xcode project](#use-a-kotlin-gradle-project-as-a-cocoapods-dependency). 

>You can also add dependencies between a Kotlin Pod and multiple Xcode projects. However, in this case you need to add a 
>dependency by calling `pod install` manually for each Xcode project. In other cases, it's done automatically.
{:.note}

## Install the CocoaPods dependency manager and plugin

1. Install the [CocoaPods dependency manager](https://cocoapods.org/).
    
    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    $ sudo gem install cocoapods
    ```
    
    </div>

2. Install the [`cocoapods-generate`](https://github.com/square/cocoapods-generate) plugin.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    $ sudo gem install cocoapods-generate
    ```
    
    </div>
    
3. In `build.gradle.kts` (or `build.gradle`) of your IDEA project, apply the CocoaPods plugin as well as the Kotlin
 Multiplatform plugin.

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    plugins {
       kotlin("multiplatform") version "{{ site.data.releases.latest.version }}"
       kotlin("native.cocoapods") version "{{ site.data.releases.latest.version }}"
    }
    ```
    
    </div> 

4. Configure `summary`, `homepage`, and `frameworkName`of the `Podspec` file in the `cocoapods` block.  
`version` is a version of the Gradle project.

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    plugins {
        kotlin("multiplatform") version "{{ site.data.releases.latest.version }}"
        kotlin("native.cocoapods") version "{{ site.data.releases.latest.version }}"
    }
    
    // CocoaPods requires the podspec to have a version.
    version = "1.0"
    
    kotlin {
        cocoapods {
            // Configure fields required by CocoaPods.
            summary = "Some description for a Kotlin/Native module"
            homepage = "Link to a Kotlin/Native module homepage"
    
            // You can change the name of the produced framework.
            // By default, it is the name of the Gradle project.
            frameworkName = "my_framework"
        }
    }
    ```
    
    </div>

5. Re-import the project.

When applied, the CocoaPods plugin does the following:

* Adds both `debug` and `release` frameworks as output binaries for all macOS, iOS, tvOS, and watchOS targets.
* Creates a `podspec` task which generates a [Podspec](https://guides.cocoapods.org/syntax/podspec.html)
file for the project.

The `Podspec` file includes a path to an output framework and script phases that automate building this framework during 
the build process of an Xcode project.

## Add dependencies on Pod libraries

To add dependencies between a Kotlin project and a Pod library, you should [complete the initial configuration](#install-the-cocoapods-dependency-manager-and-plugin).
This allows you to add dependencies on the following types of Pod libraries: 
 * [A Pod library from a CocoaPods repository](#add-a-dependency-on-a-pod-library-from-the-cocoapods-repository) 
 * [A Pod library stored locally](#add-a-dependency-on-a-pod-library-stored-locally)
 * [A Pod library from a Git repository](#add-a-dependency-on-a-pod-library-from-the-git-repository)
 * [A Pod library from an archive](#add-a-dependency-on-a-pod-library-from-an-archive)
 * [A Pod library from a custom Spec repository](#add-a-dependency-on-a-pod-library-from-a-custom-spec-repository)
 * [A Pod library with custom C Interop options](#add-a-dependency-on-a-pod-library-with-custom-c-interoperability-options)
 * [A static Pod library](#add-a-dependency-on-a-static-pod-library)

A Kotlin project requires the `pod()` function call in `build.gradle.kts` (`build.gradle`) for adding a Pod dependency. Each dependency requires a separate function call.
The function has a configuration block where you could specify the parameters for the dependency.

When you add a new dependency and re-import the project in IntelliJ IDEA the new dependency will be added automatically.
There are no additional steps required. 

To use your Kotlin project with Xcode, you should [make changes in your project Podfile](#update-podfile-for-xcode).

### Add a dependency on a Pod library from the CocoaPods repository

You can add dependencies on a Pod library from the CocoaPods repository with `pod()` to `build.gradle.kts` 
(`build.gradle`) of your project:
 
1. Specify the name of a Pod library in the `pod()` function. In the configuration block you can specify the version of the library using `version` parameter. To use the latest version of the library, omit the `version` parameter.

   > You can add dependencies on subspecs.
   {:.note}

2. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>
   
   ```kotlin
   kotlin {
       ios()

       cocoapods {

           ios.deploymentTarget = "13.5"

           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           pod("AFNetworking") {
                version = "~> 4.0.1"
           }
       }
   }
   ```
   
   </div>

3. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library stored locally

You can add a dependency on a Pod library stored locally with `pod()` to `build.gradle.kts` (`build.gradle`) of your project:

1. Specify the name of a Pod library in the `pod()` function. In the configuration block specify the path to the local Pod library: use the `path()` function in the `source` parameter value.

   > You can add local dependencies on subspecs as well.
   > The `cocoapods` block can include dependencies to Pods stored locally and Pods from the CocoaPods repository at
   > the same time.
   {:.note}

2. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>
   
   ```kotlin
   kotlin {
       ios()

       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           pod("pod_dependency") {
               version = "1.0"
               source = path(project.file("../pod_dependency/pod_dependency.podspec"))
           }
           pod("subspec_dependency/Core") {
               version = "1.0"
               source = path(project.file("../subspec_dependency/subspec_dependency.podspec"))
           }
           pod("AFNetworking") {
               version = "~> 4.0.1"
           }
           pod("SDWebImage/MapKit")
       }
   }
   ```
   
   </div>
   
   > You can also specify the version of the library using `version` parameter in the configuration block.
   > To use the latest version of the library, omit the parameter.
   {:.note}

3. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.pod_dependency.*
import cocoapods.subspec_dependency.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library from the Git repository

You can add dependencies on a Pod library from a custom Git repository with `pod()` to `build.gradle.kts` 
(`build.gradle`) of your project: 

1. Specify the name of a Pod library in the `pod()` function.
In the configuration block specify the path to the git repository: use the `git()` function in the `source` parameter value.

    Additionally, you can specify the following parameters in the block after `git()`:
    * `commit` – to use a specific commit of the repository
    * `tag` – to use a specific tag of the repository
    * `branch` – to use a specific branch of the repository

    The `git()` function prioritizes passed parameters in the following order (from highest to lowest): `commit`, `tag`, `branch`.
    If you don't specify any parameter, Kotlin plugin uses `HEAD` of the `master` branch.

2. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```kotlin
   kotlin {
       ios()

       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           pod("AFNetworking") {
               source = git("https://github.com/AFNetworking/AFNetworking") {
                   tag = "4.0.0"
              }
           }

           pod("JSONModel") {
               source = git("https://github.com/jsonmodel/jsonmodel.git") {
                   branch = "key-mapper-class"
               }
           }

           pod("CocoaLumberjack") {
               source = git("https://github.com/CocoaLumberjack/CocoaLumberjack.git") {
                   commit = "3e7f595e3a459c39b917aacf9856cd2a48c4dbf3"
               }
           }
       }
   }
   ```
   
   </div>

   > You can combine `branch`, `commit`, and `tag` parameters to get the specific version of a Pod.
   {:.note}

3. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library from an archive

You can add dependencies on a Pod library from `zip`, `tar`, or `jar` archive with `pod()` to `build.gradle.kts` 
(`build.gradle`) of your project:

1. Specify the name of a Pod library in the `pod()` function.
In the configuration block specify the path to the archive: use the `url()` function with an arbitrary HTTP address in the `source` parameter value. 

    Additionally, you can specify the Boolean `flatten` parameter as a second argument for the `url()` function.
    It indicates that all Pod files located in the root directory of the archive.

2. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```kotlin
   kotlin {
       ios()

       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           pod("pod_dependency") {
              source = url("https://github.com/Kotlin/kotlin-with-cocoapods-sample/raw/cocoapods-zip/cocoapodSourcesZip.zip", flatten = true)
           }

       }
   }
   ```
   
   </div>

3. Re-import the project.

> To work correctly with Xcode, you should specify the path to the Podspec in your Podfile.
> For example:
>
> <div class="sample" markdown="1" theme="idea" data-highlight-only>
>
> ```ruby
> target 'ios-app' do
>     # ... other pod depedencies ...
>    pod 'podspecWithFilesExample', :path => '../kotlin-libr/build/cocoapods/externalSources/url/podspecWithFilesExample'
> end
> ```
> 
> </div>
>
{:.note} 

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library from a custom Spec repository

You can add dependencies on a Pod library from a custom Spec repository with `pod()` and `specRepos` to `build.gradle.kts` 
(`build.gradle`) of your project:

1. Specify the HTTP address to the custom Spec repository using the `url()` inside the `specRepos` block.

2. Specify the name of a Pod library in the `pod()` function.

3. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```kotlin
   kotlin {
       ios()

       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           specRepos {
              url("https://github.com/Kotlin/kotlin-cocoapods-spec.git")
           }
           pod("example")

       }
   }
   ```

   </div>

4. Re-import the project.

> To work correctly with Xcode, you should specify the location of specs at the beginning of your Podfile.
> For example, `source 'https://github.com/Kotlin/kotlin-cocoapods-spec.git'`
{:.note} 

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

You can find a sample project [here](https://github.com/Kotlin/kotlin-with-cocoapods-sample).

### Add a dependency on a Pod library with custom C interoperability options

You can add dependencies on a Pod library with custom C interoperability options with `pod()` to `build.gradle.kts` 
(`build.gradle`) of your project:

1. Specify the name of a Pod library in the `pod()` function.
In the configuration block specify the C interoperability options:

    * `extraOpts` – to specify the list of options to a Pod library. For example, specific flags
    * `packageName` – to specify the package name. If you specify it, you can import the library using the package name: `import <packageName>`

2. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```kotlin
   kotlin {
       ios()
   
       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           pod("new_pod") {
               extraOpts = listOf("-compiler-option")
               packageName = "package"
           }

       }
   }
   ```

   </div>

3. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

If you use the `packageName` parameter, you can import the library using the package name: `import <packageName>`:

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import packageName
```

</div>

### Add a dependency on a static Pod library

You can add dependencies on a static Pod library with `pod()` and `useLibraries()` to `build.gradle.kts` 
(`build.gradle`) of your project:

1. Specify the name of the library using the `pod()` function.

2. Call the `useLibraries()` function: it enables a special flag for static libraries.

3. Specify the minimum deployment target version for the Pod library.

   > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
   {:.note}

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```kotlin
   kotlin {
       ios()

       cocoapods {
           summary = "CocoaPods test library"
           homepage = "https://github.com/JetBrains/kotlin"

           ios.deploymentTarget = "13.5"

           pod("YandexMapKit") {
               version = "~> 3.2"
           }
           useLibraries()

       }
   }
   ```

   </div>


4. Re-import the project.

To use these dependencies from the Kotlin code, import the packages `cocoapods.<library-name>`.

<div class="sample" markdown="1" theme="idea" data-highlight-only>

```kotlin
import cocoapods.AFNetworking.*
import cocoapods.SDWebImage.*
```

</div>

### Update Podfile for Xcode

If you already have the Xcode project, CocoaPods plugin requires Podfile changes to work correctly:

* If your project has any Git, HTTP, custom Spec repository dependency, you should also specify the path to the Podspec in the Podfile.

   For example, if you add the dependency on `podspecWithFilesExample`, declare the path to the Podspec in the Podfile:

   <div class="sample" markdown="1" theme="idea" data-highlight-only>
   
   ```ruby
   target 'ios-app' do
       # ... other depedencies ...
       pod 'podspecWithFilesExample', :path => '../kotlin-libr/build/cocoapods/externalSources/url/podspecWithFilesExample'
   end
   ```

   </div>

* When you add a library from the custom Spec repository, you should also specify the [location](https://guides.cocoapods.org/syntax/podfile.html#source) of specs at the beginning of your Podfile:

   <div class="sample" markdown="1" theme="idea" data-highlight-only>

   ```ruby
   source 'https://github.com/Kotlin/kotlin-cocoapods-spec.git'

   target 'kotlin-cocoapods-xcproj' do
       # ... other depedencies ...
       pod 'example'
   end
   ```

   </div>

> Re-import the project after making changes in Podfile.
{:.note}

If you don't make the changes of the Podfile, the `podInstall` task will fails and CocoaPods plugin shows the error message in the log.

See the `withXcproject` branch of the [sample project](https://github.com/Kotlin/kotlin-with-cocoapods-sample) that contains an example of Xcode integration with the existing Xcode project named `kotlin-cocoapods-xcproj`.

## Use a Kotlin Gradle project as a CocoaPods dependency

You can use a Kotlin Multiplatform project with native targets as a CocoaPods dependency (Kotlin Pod). You can include such a dependency
in the Podfile of the Xcode project by its name and path to the project directory containing the generated Podspec.
This dependency will be automatically built (and rebuilt) along with this project.
Such an approach simplifies importing to Xcode by removing a need to write the corresponding Gradle tasks and Xcode build steps manually.

You can add dependencies between:
* [A Kotlin Pod and an Xcode project with one target](#add-a-dependency-between-a-kotlin-pod-and-xcode-project-with-one-target)
* [A Kotlin Pod and an Xcode project with several targets](#add-a-dependency-between-a-kotlin-pod-with-an-xcode-project-with-several-targets)

> To correctly import the dependencies into the Kotlin/Native module, the
`Podfile` must contain either [`use_modular_headers!`](https://guides.cocoapods.org/syntax/podfile.html#use_modular_headers_bang)
or [`use_frameworks!`](https://guides.cocoapods.org/syntax/podfile.html#use_frameworks_bang)
directive.
{:.note}

### Add a dependency between a Kotlin Pod and Xcode project with one target

1. Create an Xcode project with a `Podfile` if you haven’t done so yet.
2. Add the path to your Xcode project `Podfile` with `podfile = project.file(..)` to `build.gradle.kts` (`build.gradle`) 
of your Kotlin project.  
    This step helps synchronize your Xcode project with Kotlin Pod dependencies by calling `pod install` for your `Podfile`.
3. Specify the minimum deployment target version for the Pod library.
    > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
    {:.note}

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
        
    ```kotlin
    kotlin {
        ios()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            ios.deploymentTarget = "13.5"
            pod("AFNetworking") {
                version = "~> 4.0.0"
            }
            podfile = project.file("../ios-app/Podfile")
        }
    }
    ```
    
    </div>

4. Add the name and path of the Kotlin Pod you want to include in the Xcode project to `Podfile`.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    use_frameworks!
    
    platform :ios, '9.0'
    
    target 'ios-app' do
            pod 'kotlin_library', :path => '../kotlin-library'
    end
    ```
    
    </div>

5. Re-import the project.

### Add a dependency between a Kotlin Pod with an Xcode project with several targets

1. Create an Xcode project with a `Podfile` if you haven’t done so yet.
2. Add the path to your Xcode project `Podfile` with `podfile = project.file(..)` to `build.gradle.kts` (`build.gradle`) of
 your Kotlin project.  
    This step helps synchronize your Xcode project with Kotlin Pod dependencies by calling `pod install` for your `Podfile`.
3. Add dependencies to the Pod libraries that you want to use in your project with `pod()`.
4. For each target, specify the minimum deployment target version for the Pod library.
    > If you don't specify the minimum deployment target version and a dependency Pod requires a higher deployment target, you may get an error.
    {:.note}

    <div class="sample" markdown="1" theme="idea" data-highlight-only>
    
    ```kotlin
    kotlin {
        ios()
        tvos()
    
        cocoapods {
            summary = "CocoaPods test library"
            homepage = "https://github.com/JetBrains/kotlin"
            ios.deploymentTarget = "13.5"
            tvos.deploymentTarget = "13.4"
       
            pod("AFNetworking") {
                version = "~> 4.0.0"
            }
            podfile = project.file("../severalTargetsXcodeProject/Podfile") // specify the path to Podfile
        }
    }
    ```
    
    </div>

5. Add the name and path of the Kotlin Pod you want to include in the Xcode project to the `Podfile`.

    <div class="sample" markdown="1" theme="idea" mode="ruby" data-highlight-only>
    
    ```ruby
    target 'iosApp' do
      use_frameworks!
      platform :ios, '13.5'
      # Pods for iosApp
      pod 'kotlin_library', :path => '../kotlin-library'
    end
    
    target 'TVosApp' do
      use_frameworks!
      platform :tvos, '13.4'
      
      # Pods for TVosApp
      pod 'kotlin_library', :path => '../kotlin-library'
    end
    ```
    
    </div>
    
6. Re-import the project.

You can find a sample project [here](https://github.com/Kotlin/multitarget-xcode-with-kotlin-cocoapods-sample).