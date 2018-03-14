package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.provider.Provider


interface KotlinNativeApplication: ProductionKotlinNativeComponent {
    override fun getDevelopmentBinary(): Provider<out KotlinNativeExecutable>
}