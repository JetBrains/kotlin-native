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

#include "Atomic.h"
#include "Common.h"
#include "Memory.h"
#include "Types.h"

namespace {

struct AtomicReferenceLayout {
  KRef value_;
  KInt lock_;
};

template <typename T> T addAndGetImpl(KRef thiz, T delta) {
  volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
  return atomicAdd(location, delta);
}

template <typename T> T compareAndSwapImpl(KRef thiz, T expectedValue, T newValue) {
    volatile T* location = reinterpret_cast<volatile T*>(thiz + 1);
    return compareAndSwap(location, expectedValue, newValue);
}

inline AtomicReferenceLayout* asAtomicReference(KRef thiz) {
    return reinterpret_cast<AtomicReferenceLayout*>(thiz + 1);
}

inline void lock(KRef thiz) {
    KInt* location = &asAtomicReference(thiz)->lock_;
    while (compareAndSwap(location, 0, 1) != 0) {}
}

inline void unlock(KRef thiz) {
    KInt* location = &asAtomicReference(thiz)->lock_;
    RuntimeCheck(compareAndSwap(location, 1, 0) == 1, "Must succeed");
}

}  // namespace

extern "C" {

RUNTIME_NORETURN void ThrowInvalidMutabilityException();

KInt Kotlin_AtomicInt_addAndGet(KRef thiz, KInt delta) {
    return addAndGetImpl(thiz, delta);
}

KInt Kotlin_AtomicInt_compareAndSwap(KRef thiz, KInt expectedValue, KInt newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KLong Kotlin_AtomicLong_addAndGet(KRef thiz, KLong delta) {
    return addAndGetImpl(thiz, delta);
}

KLong Kotlin_AtomicLong_compareAndSwap(KRef thiz, KLong expectedValue, KLong newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

KNativePtr Kotlin_AtomicNativePtr_compareAndSwap(KRef thiz, KNativePtr expectedValue, KNativePtr newValue) {
    return compareAndSwapImpl(thiz, expectedValue, newValue);
}

void Kotlin_AtomicReference_checkIfFrozen(KRef value) {
    if (value != nullptr && !value->container()->permanentOrFrozen()) {
        ThrowInvalidMutabilityException();
    }
}

// TODO: maybe those two belongs to Memory.cpp, as somewhat depends on ARC/GC semantics.
OBJ_GETTER(Kotlin_AtomicReference_compareAndSwap, KRef thiz, KRef expectedValue, KRef newValue) {
    Kotlin_AtomicReference_checkIfFrozen(newValue);
    if (expectedValue == newValue) {
        RETURN_OBJ(expectedValue);
    }
    // See Kotlin_AtomicReference_get() for explanations, why locking is needed.
    lock(thiz);
    // As we lock [thiz], atomicity here is not really important.
    KRef old = compareAndSwapImpl(thiz, expectedValue, newValue);
    unlock(thiz);
    if (old == expectedValue) {
        // CAS to the new value was successful, we transfer ownership of [expectedValue] to the caller.
        // No need to update the reference counter, as before @AtomicReference was holding the reference,
        // and now result holds it. Also note, that object referred by [expectedValue] cannot be freed,
        // as it is guaranteed to be held by the caller (and by [thiz]).
        *OBJ_RESULT = old;
        return old;
    } else {
        // On this path we just create an additional reference to the [expectedValue], held by caller anyway.
        RETURN_OBJ(old);
    }
}

OBJ_GETTER(Kotlin_AtomicReference_get, KRef thiz) {
    // Here we must take a lock to prevent race when value, while taken here, is CASed and immediately
    // destroyed by an another thread. AtomicReference no longer holds such an object, so if we got
    // rescheduled unluckily, between the moment value is read from the field and RC is incremented,
    // object may go away.
    lock(thiz);
    KRef result = asAtomicReference(thiz)->value_;
    // TODO: curiously, this may be artificially a very long lock, if we return to an already used slot
    // and it triggers the cycle collector.
    // Proper behavior would be to release the lock after incrementing [result] RC, but before decrementing
    // RC of the old return slot content, but this would require primitives not yet exposed by Memory.cpp.
    // This shall be fixed, if @AtomicReference would become a performance-sensitive primitive.
    UpdateReturnRef(OBJ_RESULT, result);
    unlock(thiz);
    return result;
}

}  // extern "C"