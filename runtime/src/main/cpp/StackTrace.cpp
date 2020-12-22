/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "StackTrace.hpp"

#include <cstdlib>
#include <cstring>

#include "Porting.h"
#include "Types.h"

#if USE_GCC_UNWIND
// GCC unwinder for backtrace.
#include <unwind.h>
// AddressToSymbol mapping.
#include "ExecFormat.h"
#elif USE_LIBC_UNWIND
// Glibc backtrace() function.
#include <execinfo.h>
#endif

using namespace kotlin;

namespace {

#if (__MINGW32__ || __MINGW64__)
// Skip the stack frames related to `StackTrace` ctor and `_Unwind_Backtrace`.
static constexpr size_t kSkipFrames = 2;
#else
// Skip the stack frame related to the `StackTrace` ctor.
static constexpr size_t kSkipFrames = 1;
#endif

class StringBuilder {
public:
    StringBuilder(char* buffer, size_t size) noexcept : buffer_(buffer), size_(size) {}

    void Append(char c) noexcept {
        if (size_ <= 1) {
            return;
        }

        buffer_[0] = c;
        buffer_[1] = '\0';
        ++buffer_;
        --size_;
    }

    void Append(const char* string) {
        if (size_ <= 1) {
            return;
        }

        size_t stringLength = konan::strnlen(string, size_ - 1);
        // `stringLength` < `size_`
        strncpy(buffer_, string, stringLength);
        buffer_[stringLength] = '\0';
        buffer_ += stringLength;
        size_ -= stringLength;
    }

    template <typename... Args>
    void Append(const char* format, Args&&... args) noexcept {
        if (size_ <= 1) {
            return;
        }

        int rv = konan::snprintf(buffer_, size_, format, std::forward<Args>(args)...);
        if (rv < 0) {
            return;
        }

        size_t stringLength = std::min(static_cast<size_t>(rv), size_ - 1);
        buffer_ += stringLength;
        size_ -= stringLength;
    }

private:
    char* buffer_;
    size_t size_;
};

} // namespace

void kotlin::internal::PrettyPrintSymbol(void* address, const char* name, SourceInfo sourceInfo, char* buffer, size_t bufferSize) noexcept {
    bool needsAddress = true;

    StringBuilder builder(buffer, bufferSize);

    if (name != nullptr) {
#if USE_LIBC_UNWIND
        // With libc's `backtrace` address is already included in the symbol name.
        needsAddress = false;
#endif
        builder.Append(name);
        builder.Append(' ');
    }

    if (needsAddress) {
        builder.Append("%p ", address);
    }

    if (sourceInfo.fileName == nullptr) return;

    builder.Append("(%s:", sourceInfo.fileName);

    if (sourceInfo.lineNumber == -1) {
        builder.Append("<unknown>)");
    } else {
        builder.Append("%d:%d)", sourceInfo.lineNumber, sourceInfo.column);
    }
}

// TODO: this implementation is just a hack, e.g. the result is inexact;
// however it is better to have an inexact stacktrace than not to have any.
NO_INLINE StackTrace::StackTrace(size_t skipFrames) noexcept : skipFrames_(skipFrames + kSkipFrames) {
#if USE_GCC_UNWIND
    _Unwind_Trace_Fn unwindCallback = [](_Unwind_Context* context, void* arg) {
        auto* stackTrace = static_cast<StackTrace*>(arg);
        // We do not allocate a dynamic storage for the stacktrace so store only first `kBufferCapacity` elements.
        if (stackTrace->size_ >= stackTrace->buffer_.size()) {
            return _URC_NO_REASON;
        }

#if (__MINGW32__ || __MINGW64__)
        _Unwind_Ptr ptr = _Unwind_GetRegionStart(context);
#else
        _Unwind_Ptr ptr = _Unwind_GetIP(context);
#endif
        stackTrace->buffer_[stackTrace->size_++] = reinterpret_cast<void*>(ptr);
        return _URC_NO_REASON;
    };
    _Unwind_Backtrace(unwindCallback, this);
#elif USE_LIBC_UNWIND
    size_ = backtrace(buffer_.data(), buffer_.size());
#endif
}

SymbolicStackTrace::Symbol::Symbol(const SymbolicStackTrace& owner, size_t index) noexcept : address_(owner.addresses_[index]) {
#if USE_GCC_UNWIND
    if (!AddressToSymbol(address_, name_.data(), name_.size())) {
        // Make empty string:
        name_[0] = '\0';
    }
#elif USE_LIBC_UNWIND
    // Can be null if we failed to allocated dynamic memory for the symbols. In which case - fine,
    // we just avoid printing the symbols
    if (owner.symbols_ != nullptr) {
        name_ = owner.symbols_[index];
    }
#endif
}

const char* SymbolicStackTrace::Symbol::Name() const noexcept {
#if NO_UNWIND
    return nullptr;
#elif USE_GCC_UNWIND
    return name_.data();
#elif USE_LIBC_UNWIND
    return name_;
#endif
}

std::array<char, 1024> SymbolicStackTrace::Symbol::PrettyPrint(bool allowSourceInfo) const noexcept {
    void* address = address_;
    const char* name = Name();
    SourceInfo sourceInfo = allowSourceInfo ? Kotlin_getSourceInfo(address) : SourceInfo{nullptr, -1, -1};
    std::array<char, 1024> buffer;
    internal::PrettyPrintSymbol(address, name, sourceInfo, buffer.data(), buffer.size());
    return buffer;
}

SymbolicStackTrace::SymbolicStackTrace(void* const* addresses, size_t size) noexcept : addresses_(addresses), size_(size) {
#if USE_LIBC_UNWIND
    if (size_ > 0) {
        symbols_ = backtrace_symbols(addresses_, size_);
    }
#endif
}

SymbolicStackTrace::SymbolicStackTrace(const StackTrace& stackTrace) noexcept : SymbolicStackTrace(stackTrace.data(), stackTrace.size()) {}

SymbolicStackTrace::SymbolicStackTrace(SymbolicStackTrace&& rhs) noexcept : addresses_(rhs.addresses_), size_(rhs.size_) {
    rhs.size_ = 0;
    rhs.addresses_ = nullptr;
#if USE_LIBC_UNWIND
    symbols_ = rhs.symbols_;
    rhs.symbols_ = nullptr;
#endif
}

SymbolicStackTrace& SymbolicStackTrace::operator=(SymbolicStackTrace&& rhs) noexcept {
    SymbolicStackTrace copy(std::move(rhs));
    swap(copy);
    return *this;
}

SymbolicStackTrace::~SymbolicStackTrace() {
#if USE_LIBC_UNWIND
    // Not `konan::free`. Used to free memory allocated in `backtrace_symbols` where `malloc` is used.
    free(symbols_);
#endif
}

void SymbolicStackTrace::swap(SymbolicStackTrace& rhs) noexcept {
    std::swap(addresses_, rhs.addresses_);
    std::swap(size_, rhs.size_);
#if USE_LIBC_UNWIND
    std::swap(symbols_, rhs.symbols_);
#endif
}

NO_INLINE void kotlin::PrintStackTraceStderr(bool allowSourceInfo) noexcept {
    // Skip this function in the stack trace.
    StackTrace stackTrace(1);
    SymbolicStackTrace symbolicStackTrace(stackTrace);

    for (const auto& symbol : symbolicStackTrace) {
        auto line = symbol.PrettyPrint(allowSourceInfo);
        konan::consoleErrorUtf8(line.data(), konan::strnlen(line.data(), line.size()));
        konan::consoleErrorUtf8("\n", 1);
    }
}
