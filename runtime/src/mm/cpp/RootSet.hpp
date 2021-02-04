/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_ROOT_SET_H
#define RUNTIME_MM_ROOT_SET_H

#include "GlobalsRegistry.hpp"
#include "ShadowStack.hpp"
#include "StableRefRegistry.hpp"
#include "ThreadLocalStorage.hpp"

struct ObjHeader;

namespace kotlin {
namespace mm {

class ThreadData;

// A helper class to unify root set iteration.
// Use `ThreadIter(ThreadData&)` to iterate over thread-specific rootset, and use `GlobalIter` to
// lock and iterate over global rootset.
class RootSet {
public:
    class ThreadRootSetIterable;

    class ThreadRootSetIterator {
    public:
        struct begin_t {};
        static constexpr inline begin_t begin = begin_t{};

        struct end_t {};
        static constexpr inline end_t end = end_t{};

        ThreadRootSetIterator(begin_t, ThreadRootSetIterable& owner) noexcept;
        ThreadRootSetIterator(end_t, ThreadRootSetIterable& owner) noexcept;

        ObjHeader*& operator*() noexcept;

        ThreadRootSetIterator& operator++() noexcept;

        bool operator==(const ThreadRootSetIterator& rhs) const noexcept;
        bool operator!=(const ThreadRootSetIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        enum class Phase {
            kStack,
            kTLS,
            kDone,
        };

        void Init() noexcept;

        ThreadRootSetIterable& owner_;
        Phase phase_;
        union {
            ShadowStack::Iterator stackIterator_;
            ThreadLocalStorage::Iterator tlsIterator_;
        };
    };

    class ThreadRootSetIterable {
    public:
        ThreadRootSetIterable(ShadowStack& stack, ThreadLocalStorage& tls) noexcept : stack_(stack), tls_(tls) {}

        ThreadRootSetIterator begin() noexcept { return ThreadRootSetIterator(ThreadRootSetIterator::begin, *this); }
        ThreadRootSetIterator end() noexcept { return ThreadRootSetIterator(ThreadRootSetIterator::end, *this); }

    private:
        friend class ThreadRootSetIterator;

        ShadowStack& stack_;
        ThreadLocalStorage& tls_;
    };

    class GlobalRootSetIterable;

    class GlobalRootSetIterator {
    public:
        struct begin_t {};
        static constexpr inline begin_t begin = begin_t{};

        struct end_t {};
        static constexpr inline end_t end = end_t{};

        GlobalRootSetIterator(begin_t, GlobalRootSetIterable& owner) noexcept;
        GlobalRootSetIterator(end_t, GlobalRootSetIterable& owner) noexcept;

        ObjHeader*& operator*() noexcept;

        GlobalRootSetIterator& operator++() noexcept;

        bool operator==(const GlobalRootSetIterator& rhs) const noexcept;
        bool operator!=(const GlobalRootSetIterator& rhs) const noexcept { return !(*this == rhs); }

    private:
        enum class Phase {
            kGlobals,
            kStableRefs,
            kDone,
        };

        void Init() noexcept;

        GlobalRootSetIterable& owner_;
        Phase phase_;
        union {
            GlobalsRegistry::Iterator globalsIterator_;
            StableRefRegistry::Iterator stableRefsIterator_;
        };
    };

    class GlobalRootSetIterable {
    public:
        GlobalRootSetIterable(GlobalsRegistry& globalsRegistry, StableRefRegistry& stableRefRegistry) noexcept :
            globalsIterable_(globalsRegistry.Iter()), stableRefsIterable_(stableRefRegistry.Iter()) {}

        GlobalRootSetIterator begin() noexcept { return GlobalRootSetIterator(GlobalRootSetIterator::begin, *this); }
        GlobalRootSetIterator end() noexcept { return GlobalRootSetIterator(GlobalRootSetIterator::end, *this); }

    private:
        friend class GlobalRootSetIterator;

        // TODO: These use separate locks, which is inefficient, and slightly dangerous. In practice it's
        //       fine, because this is the only place where these two locks are taken simultaneously.
        GlobalsRegistry::Iterable globalsIterable_;
        StableRefRegistry::Iterable stableRefsIterable_;
    };

    ThreadRootSetIterable ThreadIter(ThreadData& threadData) noexcept;

    GlobalRootSetIterable GlobalIter() noexcept;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_ROOT_SET_H
