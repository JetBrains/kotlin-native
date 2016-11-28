package org.jetbrains.kotlin.backend.konan.optimizer

// If we need to patch an instance of T with an instance of S
// but produce only a single copy of the patched instance
public class RewriteMap<T, S> {
    val context = mutableMapOf<Pair<T, S>, T>()

    fun getOrPut(old: T, arg: S, new: () -> T ): T {
         val key = Pair(old, arg)
         return context.getOrPut(key, new)
    }
}


