/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CurrentException.hpp"

#include <algorithm>

#include "KAssert.h"

using namespace kotlin;

void mm::CurrentException::AddException(ObjHeader* exception) noexcept {
    currentExceptions_.push_back(exception);
}

void mm::CurrentException::RemoveException(ObjHeader* exception) noexcept {
    // We're not expecting a lot of nested exceptions, so linear scan is fine.
    auto it = std::remove(currentExceptions_.begin(), currentExceptions_.end(), exception);
    RuntimeAssert(it != currentExceptions_.end(), "Cannot remove exception %p that wasn't previously added", exception);
    currentExceptions_.erase(it, currentExceptions_.end());
}
