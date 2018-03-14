package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.provider.Provider
import org.gradle.language.ProductionComponent

// TODO: Use generics instead of overriding getDevelopmentBinary method.
interface ProductionKotlinNativeComponent: KotlinNativeComponent, ProductionComponent {
    override fun getDevelopmentBinary(): Provider<out KotlinNativeBinary>
}
