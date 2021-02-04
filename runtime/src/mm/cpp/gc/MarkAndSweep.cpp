/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MarkAndSweep.hpp"

#include "../ExtraObjectData.hpp"
#include "../GlobalData.hpp"
#include "../ObjectFactory.hpp"
#include "../RootSet.hpp"
#include "../ThreadData.hpp"
#include "Cleaner.h"
#include "FinalizerHooks.hpp"
#include "MarkAndSweepUtils.hpp"
#include "Natives.h"
#include "ObjectTraversal.hpp"
#include "Runtime.h"
#include "WorkerBoundReference.h"

using namespace kotlin;

namespace {

struct MarkTraits {
    static bool TryMark(ObjHeader* object) noexcept {
        auto& objectData = mm::ObjectFactory<mm::MarkAndSweep>::NodeRef::From(object).GCObjectData();
        if (objectData.color() == mm::MarkAndSweep::ObjectData::Color::kBlack) return false;
        objectData.setColor(mm::MarkAndSweep::ObjectData::Color::kBlack);
        return true;
    };
};

struct SweepTraits {
    using ObjectFactory = mm::ObjectFactory<mm::MarkAndSweep>;

    static bool TryResetMark(ObjectFactory::NodeRef node) noexcept {
        auto& objectData = node.GCObjectData();
        if (objectData.color() == mm::MarkAndSweep::ObjectData::Color::kWhite) return false;
        objectData.setColor(mm::MarkAndSweep::ObjectData::Color::kWhite);
        return true;
    }
};

struct FinalizeTraits {
    using ObjectFactory = mm::ObjectFactory<mm::MarkAndSweep>;
};

} // namespace

void mm::MarkAndSweep::ThreadData::SafePointFunctionEpilogue() noexcept {
    if (gc_.GetThreshold() == 0 || (safePointsCounter_ + 1) % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void mm::MarkAndSweep::ThreadData::SafePointLoopBody() noexcept {
    if (gc_.GetThreshold() == 0 || (safePointsCounter_ + 1) % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void mm::MarkAndSweep::ThreadData::SafePointExceptionUnwind() noexcept {
    if (gc_.GetThreshold() == 0 || (safePointsCounter_ + 1) % gc_.GetThreshold() == 0) {
        PerformFullGC();
    }
    ++safePointsCounter_;
}

void mm::MarkAndSweep::ThreadData::SafePointAllocation(size_t size) noexcept {
    size_t allocationOverhead =
            gc_.GetAllocationThresholdBytes() == 0 ? allocatedBytes_ : allocatedBytes_ % gc_.GetAllocationThresholdBytes();
    if (allocationOverhead + size >= gc_.GetAllocationThresholdBytes()) {
        PerformFullGC();
    }
    allocatedBytes_ += size;
}

void mm::MarkAndSweep::ThreadData::PerformFullGC() noexcept {
    gc_.PerformFullGC();
}

void mm::MarkAndSweep::ThreadData::OnOOM(size_t size) noexcept {
    PerformFullGC();
}

void mm::MarkAndSweep::PerformFullGC() noexcept {
    RuntimeAssert(running_ == false, "Cannot have been called during another collection");
    running_ = true;

    KStdVector<ObjHeader*> graySet;
    for (auto& thread : mm::GlobalData::Instance().threadRegistry().Iter()) {
        thread.Publish();
        for (auto* object : mm::ThreadRootSet(thread)) {
            graySet.push_back(object);
        }
    }
    mm::StableRefRegistry::Instance().ProcessDeletions();
    for (auto* object : mm::GlobalRootSet()) {
        graySet.push_back(object);
    }

    mm::Mark<MarkTraits>(std::move(graySet));
    auto finalizerQueue = mm::Sweep<SweepTraits>(mm::GlobalData::Instance().objectFactory());

    running_ = false;

    // TODO: These will actually need to be run on a separate thread.
    mm::Finalize<FinalizeTraits>(std::move(finalizerQueue));
}
