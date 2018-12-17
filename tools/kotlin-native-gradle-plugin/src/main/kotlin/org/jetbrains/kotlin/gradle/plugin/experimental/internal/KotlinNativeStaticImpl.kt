/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeStatic
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject


open class KotlinNativeStaticImpl @Inject constructor(
    name: String,
    baseName: Provider<String>,
    componentDependencies: KotlinNativeDependenciesImpl,
    component: KotlinNativeMainComponent,
    identity: KotlinNativeVariantIdentity,
    projectLayout: ProjectLayout,
    objects: ObjectFactory,
    configurations: ConfigurationContainer,
    fileOperations: FileOperations
) : AbstractKotlinNativeBinary(
    name,
    baseName,
    component,
    identity,
    projectLayout,
    CompilerOutputKind.STATIC,
    objects,
    componentDependencies,
    configurations,
    fileOperations
), KotlinNativeStatic {
    override val outputRootName: String = "lib"
}