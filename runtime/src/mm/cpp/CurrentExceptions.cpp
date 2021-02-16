/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CurrentExceptions.hpp"

#include <algorithm>

#include "KAssert.h"

using namespace kotlin;

namespace {

template <typename ForwardIt, typename T>
ForwardIt remove_one(ForwardIt begin, ForwardIt end, const T& value) noexcept {
    bool shouldContinue = true;
    return std::remove_if(begin, end, [&shouldContinue, &value](const T& currentValue) noexcept {
        if (!shouldContinue) return false;
        if (currentValue != value) return false;
        shouldContinue = false;
        return true;
    });
}

} // namespace

void mm::CurrentExceptions::AddException(ObjHeader* exception) noexcept {
    currentExceptions_.push_back(exception);
}

void mm::CurrentExceptions::RemoveException(ObjHeader* exception) noexcept {
    // We're not expecting a lot of nested exceptions, so linear scan is fine.
    auto it = remove_one(currentExceptions_.begin(), currentExceptions_.end(), exception);
    RuntimeAssert(it != currentExceptions_.end(), "Cannot remove exception %p that wasn't previously added", exception);
    currentExceptions_.erase(it, currentExceptions_.end());
}
