/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_ALIGNMENT_H
#define RUNTIME_ALIGNMENT_H

#include <cstddef>

namespace kotlin {

constexpr inline size_t AlignUp(size_t size, size_t alignment) {
    return (size + alignment - 1) & ~(alignment - 1);
}

} // namespace kotlin

#endif // RUNTIME_ALIGNMENT_H
