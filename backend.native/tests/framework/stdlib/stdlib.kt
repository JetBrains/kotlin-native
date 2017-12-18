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

@file:Suppress("UNUSED")

package stdlib

fun <K, V> isEmpty(map: Map<K, V>) = map.isEmpty()

fun <K, V> getKeysAsSet(map: Map<K, V>) = map.keys
fun <K, V> getKeysAsList(map: Map<K, V>) = map.keys.toList()

fun <K, V> toMutableMap(map: HashMap<K, V>) = map.toMutableMap()

fun <E> getFirstElement(collection: Collection<E>) = collection.first()

fun <K, V> tryToAdd(entry: Map.Entry<K, V>, to: Map<K, V>) {

}