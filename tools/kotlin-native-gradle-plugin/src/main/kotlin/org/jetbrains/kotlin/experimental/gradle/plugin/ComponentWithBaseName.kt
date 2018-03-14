package org.jetbrains.kotlin.experimental.gradle.plugin

import org.gradle.api.component.SoftwareComponent
import org.gradle.api.provider.Provider

interface ComponentWithBaseName: SoftwareComponent {
    fun getBaseName(): Provider<String>
}
