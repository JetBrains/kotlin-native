package org.jetbrains.kotlin.experimental.gradle.plugin.sourcesets

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.experimental.gradle.plugin.KotlinNativeComponent
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinNativeSourceSet: Named {
    /*
        Нам нужно:
            sourceDir set-ы на каждый таргет по одному
            возможность получить sourceDirSet (ы) по таргету
            возможность получить binary по таргету.
            возможность получить KotlinNativeComponent по таргету
    */

    val common: SourceDirectorySet
    val component: KotlinNativeComponent

    fun getCommonSources(): SourceDirectorySet = common
    fun getPlatformSources(target: KonanTarget): SourceDirectorySet
    fun getAllSources(target: KonanTarget): FileCollection

    fun common(configureClosure: Closure<*>): KotlinNativeSourceSet
    fun common(configureAction: Action<in SourceDirectorySet>): KotlinNativeSourceSet
    fun common(configureLambda: SourceDirectorySet.() -> Unit): KotlinNativeSourceSet

    // TODO: Implement DSL for per-target source directory setting

    // TODO: Implement
    // TODO: may be provide some way to use different configs for different targets?
    //val objectKlibs: Configuration
    //fun getOutput(): FileCollection

    // TODO: Также тут нужны:
    // Таска для сборки клибы
    // конфигурация с клибами для каждого таргета. Возможно, одна, от которой будут наследоваться специальные конфиги
}
