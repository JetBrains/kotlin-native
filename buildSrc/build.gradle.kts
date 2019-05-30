/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

buildscript {
    val rootBuildDirectory = "$rootDir/.."
    extra["rootBuildDirectory"] = rootBuildDirectory
    apply(from = "$rootBuildDirectory/gradle/loadRootProperties.gradle")
    apply(from = "$rootBuildDirectory/gradle/kotlinGradlePlugin.gradle")
}

val buildKotlinCompilerRepo: String by project
val kotlinCompilerRepo: String by project

val repos = listOf(
    buildKotlinCompilerRepo,
    kotlinCompilerRepo,
    "https://cache-redirector.jetbrains.com/maven-central",
    "https://kotlin.bintray.com/kotlinx"
)

allprojects {
    repositories {
        repos.forEach { repoUrl ->
            maven { setUrl(repoUrl) }
        }
    }
}

dependencies {
    runtime(project(":plugins"))
}
