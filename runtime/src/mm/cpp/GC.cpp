/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GC.hpp"

#include "GlobalData.hpp"
#include "KAssert.h"

using namespace kotlin;

mm::GC::ThreadData::ThreadData(GC& gc) noexcept {}

mm::GC::ThreadData::~ThreadData() = default;

void mm::GC::ThreadData::SafePointFunctionEpilogue() noexcept {
    TODO();
}

void mm::GC::ThreadData::SafePointLoopBody() noexcept {
    TODO();
}

void mm::GC::ThreadData::SafePointExceptionUnwind() noexcept {
    TODO();
}

void mm::GC::ThreadData::SafePointAllocation(size_t size) noexcept {
    // TODO: Unimplemented
}

void mm::GC::ThreadData::PerformFullGC() noexcept {
    TODO();
}

void mm::GC::ThreadData::OnOOM(size_t size) noexcept {
    TODO();
}

// static
mm::GC& mm::GC::Instance() noexcept {
    return GlobalData::Instance().gc();
}
