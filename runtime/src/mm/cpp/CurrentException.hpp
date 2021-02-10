/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_CURRENT_EXCEPTION_H
#define RUNTIME_MM_CURRENT_EXCEPTION_H

#include "Types.h"
#include "Utils.hpp"

struct ObjHeader;

namespace kotlin {
namespace mm {

class CurrentException : private Pinned {
public:
    using Iterator = KStdVector<ObjHeader*>::iterator;

    void AddException(ObjHeader* exception) noexcept;
    void RemoveException(ObjHeader* exception) noexcept;

    Iterator begin() noexcept { return currentExceptions_.begin(); }
    Iterator end() noexcept { return currentExceptions_.end(); }

private:
    // TODO: Maybe better use small vector optimization here.
    KStdVector<ObjHeader*> currentExceptions_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_CURRENT_EXCEPTION_H
