package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.provider.Provider
import org.gradle.language.ProductionComponent

interface ProductionKotlinNativeComponent: KotlinNativeComponent, ProductionComponent {
    override fun getDevelopmentBinary(): Provider<out KotlinNativeBinary>
}
