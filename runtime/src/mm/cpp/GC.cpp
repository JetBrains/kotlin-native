/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "GC.hpp"

#include "GlobalData.hpp"

using namespace kotlin;

mm::GC::ThreadData::ThreadData(GC& gc) noexcept {}

void mm::GC::ThreadData::PerformFullGC() noexcept {
    TODO();
}

void mm::GC::ThreadData::SafePointFunctionEpilogue() noexcept {
    // TODO: Implement
}

void mm::GC::ThreadData::SafePointLoopBody() noexcept {
    // TODO: Implement
}

void mm::GC::ThreadData::SafePointExceptionUnwind() noexcept {
    // TODO: Implement
}

void mm::GC::ThreadData::SafePointAllocation(size_t allocationSize) noexcept {
    // TODO: Implement
}

// static
mm::GC& mm::GC::Instance() noexcept {
    return GlobalData::Instance().gc();
}

mm::GC::GC() noexcept = default;
mm::GC::~GC() = default;
