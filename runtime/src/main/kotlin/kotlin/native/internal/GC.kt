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

package kotlin.native.internal

// Cycle garbage collector interface.
//
// Konan relies upon reference counting for object management, however it could
// not collect cyclical garbage, so we perform periodic garbage collection.
// This may slow down application, so this interface provides control over how
// garbage collector activates and runs.
// Garbage collector can be in one of the following states:
//  * running
//  * suspended (so cycle candidates are collected, but GC is not performed until resume)
//  * stopped (all cyclical garbage is hopelessly lost)
// Immediately after startup GC is in running state.
// Depending on application needs it may select to suspend GC for certain phases of
// its lifetime, and resume it later on, or just completely turn it off, if GC pauses
// are less desirable than cyclical garbage leaks.
object GC {
    // To force garbage collection immediately, unless collector is stopped
    // with stop() operation. Even if GC is suspended, collect() still triggers collection.
    @SymbolName("Kotlin_native_internal_GC_collect")
    external fun collect()

    // Suspend garbage collection. Release candidates are still collected, but
    // GC algorithm is not executed.
    @SymbolName("Kotlin_native_internal_GC_suspend")
    external fun suspend()

    // Resume garbage collection. Can potentially lead to GC immediately.
    @SymbolName("Kotlin_native_internal_GC_resume")
    external fun resume()

    // Stop garbage collection. Cyclical garbage is no longer collected.
    @SymbolName("Kotlin_native_internal_GC_stop")
    external fun stop()

    // Start garbage collection. Cyclical garbage produced while GC was stopped
    // cannot be reclaimed, but all new garbage is collected.
    @SymbolName("Kotlin_native_internal_GC_start")
    external fun start()

    // GC threshold, controlling how frequenly GC is activated, and how much time GC
    // takes. Bigger values lead to longer GC pauses, but less GCs.
    var threshold: Int
        get() = getThreshold()
        set(value) = setThreshold(value)

    @SymbolName("Kotlin_native_internal_GC_getThreshold")
    private external fun getThreshold(): Int

    @SymbolName("Kotlin_native_internal_GC_setThreshold")
    private external fun setThreshold(value: Int)
}