package org.jetbrains.kotlin.builtins.konan

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.StorageManager

class KonanBuiltIns(storageManager: StorageManager) : KotlinBuiltIns(storageManager) {

    override fun getSuspendFunction(parameterCount: Int) =
            getBuiltInClassByName(Name.identifier("SuspendFunction$parameterCount"))
}

private val STDLIB_MODULE_NAME = Name.special("<stdlib>")

fun ModuleDescriptor.isKonanStdlib() = name == STDLIB_MODULE_NAME
