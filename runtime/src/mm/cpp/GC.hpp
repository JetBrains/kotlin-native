/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GC_H
#define RUNTIME_MM_GC_H

#include <cstddef>
#include <cstdint>

#include "Utils.hpp"

namespace kotlin {
namespace mm {

// TODO: GC should be extracted into a separate module, so that we can do different GCs without
//       the need to redo the entire MM.

class GC : private Pinned {
public:
    class ObjectData {
    public:
        enum class Color : uint32_t {
            kWhite = 0,
            kBlack,
        };

        Color color() const noexcept { return static_cast<Color>(flags_ & 0x1); }

        void setColor(Color color) noexcept { flags_ |= static_cast<uint32_t>(color); }

    private:
        uint32_t flags_ = 0;
    };

    class ThreadData : private Pinned {
    public:
        using ObjectData = GC::ObjectData;

        explicit ThreadData(GC& gc) noexcept;
        ~ThreadData();

        void SafePointFunctionEpilogue() noexcept;
        void SafePointLoopBody() noexcept;
        void SafePointExceptionUnwind() noexcept;
        void SafePointAllocation(size_t size) noexcept;

        // Stops the thread until full GC is performed.
        void PerformFullGC() noexcept;

        void OnOOM(size_t size) noexcept;

    private:
    };

    GC() noexcept = default;
    ~GC() = default;

    static GC& Instance() noexcept;

private:
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GC_H
