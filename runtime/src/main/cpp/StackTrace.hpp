/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_STACK_TRACE_H
#define RUNTIME_STACK_TRACE_H

#include <array>
#include <cstddef>
#include <cstdint>

#include "SourceInfo.h"
#include "Utils.hpp"

#if USE_GCC_UNWIND
#elif NO_UNWIND
#else
#define USE_LIBC_UNWIND 1
#endif

namespace kotlin {

namespace internal {

// TODO: This API is asking for a span.
void PrettyPrintSymbol(void* address, const char* name, SourceInfo sourceInfo, char* buffer, size_t bufferSize) noexcept;

} // namespace internal

class StackTrace {
public:
    explicit StackTrace(size_t skipFrames = 0) noexcept;

    // TODO: This API is asking for a span.

    size_t size() const noexcept { return skipFrames_ >= size_ ? 0 : size_ - skipFrames_; }

    void* const* data() const noexcept { return buffer_.data() + skipFrames_; }

private:
    size_t size_ = 0;
    size_t skipFrames_;
    std::array<void*, 32> buffer_ = {nullptr};
};

class SymbolicStackTrace : private MoveOnly {
public:
    class Symbol {
    public:
        Symbol(const SymbolicStackTrace& owner, size_t index) noexcept;

        void* Address() const noexcept { return address_; }
        const char* Name() const noexcept;

        std::array<char, 1024> PrettyPrint(bool allowSourceInfo = true) const noexcept;

    private:
        void* address_;
#if USE_LIBC_UNWIND
        char* name_ = nullptr;
#elif USE_GCC_UNWIND
        std::array<char, 512> name_;
#endif
    };

    class Iterator {
    public:
        Iterator(const SymbolicStackTrace& owner, size_t index) noexcept : owner_(owner), index_(index) {}

        Iterator& operator++() noexcept {
            ++index_;
            return *this;
        }

        Symbol operator*() const noexcept { return Symbol(owner_, index_); }

        bool operator==(const Iterator& rhs) const noexcept { return index_ == rhs.index_; }

        bool operator!=(const Iterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        const SymbolicStackTrace& owner_;
        size_t index_ = 0;
    };

    // TODO: This argument should be a span.
    SymbolicStackTrace(void* const* addresses, size_t size) noexcept;
    explicit SymbolicStackTrace(const StackTrace& stackTrace) noexcept;

    SymbolicStackTrace(SymbolicStackTrace&& rhs) noexcept;
    SymbolicStackTrace& operator=(SymbolicStackTrace&& rhs) noexcept;

    ~SymbolicStackTrace();

    void swap(SymbolicStackTrace& rhs) noexcept;

    Iterator begin() const noexcept { return Iterator(*this, 0); }
    Iterator end() const noexcept { return Iterator(*this, size_); }

    size_t size() const noexcept { return size_; }

    Symbol operator[](size_t index) const noexcept { return Symbol(*this, index); }

private:
    void* const* addresses_;
    size_t size_;
#if USE_LIBC_UNWIND
    char** symbols_ = nullptr;
#endif
};

void PrintStackTraceStderr(bool allowSourceInfo = true) noexcept;

} // namespace kotlin

#endif // RUNTIME_STACK_TRACE_H
