/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GC_H
#define RUNTIME_MM_GC_H

#include <cstddef>

#include "Utils.hpp"

namespace kotlin {
namespace mm {

class ThreadData;

// TODO: Come up with an abstraction to substitute different GCs at compile time without redoing the entire MM.
class GC : private Pinned {
public:
    class ThreadData : private Pinned {
    public:
        explicit ThreadData(GC& gc) noexcept;

        // Perform GC and wait for it to finish.
        void PerformFullGC() noexcept;

        void SafePointFunctionEpilogue() noexcept;
        void SafePointLoopBody() noexcept;
        void SafePointExceptionUnwind() noexcept;
        void SafePointAllocation(size_t allocationSize) noexcept;
    };

    static GC& Instance() noexcept;

private:
    friend class GlobalData;

    GC() noexcept;
    ~GC();
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GC_H
