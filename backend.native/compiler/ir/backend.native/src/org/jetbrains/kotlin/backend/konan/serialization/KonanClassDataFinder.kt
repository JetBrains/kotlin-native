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

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.serialization.ClassData
import org.jetbrains.kotlin.serialization.ClassDataWithSource
import org.jetbrains.kotlin.serialization.DescriptorSerializer
import org.jetbrains.kotlin.serialization.KonanLinkData
import org.jetbrains.kotlin.serialization.StringTable
import org.jetbrains.kotlin.serialization.deserialization.NameResolver
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder

class KonanClassDataFinder(
        private val fragment: KonanLinkData.PackageFragment,
        private val nameResolver: NameResolver
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassDataWithSource? {
        val proto = fragment.classes
        val classList = proto.getClassesList()
        val nameList = proto.getClassNameList()

        val index = nameList.indexOfFirst({ 
            nameResolver.getClassId(it) == classId })
        if (index == -1) 
            error("Could not find serialized class ${classId}")

        val foundClass = proto.getClasses(index)
        if (foundClass == null) 
            error("Could not find data for serialized class ${classId}")

        return ClassDataWithSource(ClassData(nameResolver, foundClass), SourceElement.NO_SOURCE)
    }
}


