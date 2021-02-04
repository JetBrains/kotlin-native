/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_MARK_AND_SWEEP_UTILS_H
#define RUNTIME_MM_MARK_AND_SWEEP_UTILS_H

#include "../ExtraObjectData.hpp"
#include "FinalizerHooks.hpp"
#include "Memory.h"
#include "ObjectTraversal.hpp"
#include "Runtime.h"
#include "Types.h"

namespace kotlin {
namespace mm {

// TODO: Because of `graySet` this implementation may allocate heap memory during GC.
template <typename Traits>
void Mark(KStdVector<ObjHeader*> graySet) noexcept {
    while (!graySet.empty()) {
        ObjHeader* top = graySet.back();
        graySet.pop_back();

        if (top == nullptr) continue;
        // Handle initializing singleton.
        // TODO: Probably not the place for it.
        if (top == reinterpret_cast<ObjHeader*>(1)) continue;

        if (top->heap()) {
            if (!Traits::TryMark(top)) {
                continue;
            }
        }

        traverseReferredObjects(top, [&graySet](ObjHeader* field) noexcept { graySet.push_back(field); });

        if (auto* extraObjectData = mm::ExtraObjectData::GetOrNull(top)) {
            graySet.push_back(*extraObjectData->GetWeakCounterLocation());
        }
    }
}

template <typename Traits>
typename Traits::ObjectFactory::FinalizerQueue Sweep(typename Traits::ObjectFactory& objectFactory) noexcept {
    typename Traits::ObjectFactory::FinalizerQueue finalizerQueue;

    auto iter = objectFactory.Iter();
    for (auto it = iter.begin(); it != iter.end();) {
        if (Traits::TryResetMark(*it)) {
            ++it;
            continue;
        }
        auto* objHeader = it->IsArray() ? it->GetArrayHeader()->obj() : it->GetObjHeader();
        if (HasFinalizers(objHeader)) {
            iter.MoveAndAdvance(finalizerQueue, it);
        } else {
            iter.EraseAndAdvance(it);
        }
    }

    return finalizerQueue;
}

template <typename Traits>
void Finalize(typename Traits::ObjectFactory::FinalizerQueue finalizerQueue) noexcept {
    RuntimeAssert(Kotlin_hasRuntime(), "Finalizers need a Kotlin runtime");
    for (auto node : finalizerQueue) {
        RunFinalizers(node->IsArray() ? node->GetArrayHeader()->obj() : node->GetObjHeader());
    }
}

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_MARK_AND_SWEEP_UTILS_H
