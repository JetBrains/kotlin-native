/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.frontend.di.configureModule
import org.jetbrains.kotlin.platform.konan.KonanPlatforms
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.konan.platform.NativePlatformAnalyzerServices
import org.jetbrains.kotlin.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.kotlin.types.SubstitutingScopeProviderImpl

fun createTopDownAnalyzerProviderForKonan(
        moduleContext: ModuleContext,
        bindingTrace: BindingTrace,
        declarationProviderFactory: DeclarationProviderFactory,
        languageVersionSettings: LanguageVersionSettings,
        initContainer: StorageComponentContainer.() -> Unit
): ComponentProvider {
    return createContainer("TopDownAnalyzerForKonan", NativePlatformAnalyzerServices) {
        configureModule(moduleContext, KonanPlatforms.defaultKonanPlatform, NativePlatformAnalyzerServices, bindingTrace, languageVersionSettings)

        useInstance(declarationProviderFactory)
        useImpl<AnnotationResolverImpl>()

        CompilerEnvironment.configure(this)

        useImpl<SubstitutingScopeProviderImpl>()
        useImpl<ResolveSession>()
        useImpl<LazyTopDownAnalyzer>()

        initContainer()
    }.apply {
        get<ModuleDescriptorImpl>().initialize(get<KotlinCodeAnalyzer>().packageFragmentProvider)
    }
}
