/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RootSet.hpp"

#include "KAssert.h"
#include "GlobalData.hpp"
#include "ThreadData.hpp"

using namespace kotlin;

mm::RootSet::ThreadRootSetIterator::ThreadRootSetIterator(begin_t, ThreadRootSetIterable& owner) noexcept :
    owner_(owner), phase_(Phase::kStack), stackIterator_(owner_.stack_.begin()) {
    Init();
}

mm::RootSet::ThreadRootSetIterator::ThreadRootSetIterator(end_t, ThreadRootSetIterable& owner) noexcept :
    owner_(owner), phase_(Phase::kDone) {}

ObjHeader*& mm::RootSet::ThreadRootSetIterator::operator*() noexcept {
    switch (phase_) {
        case Phase::kStack:
            return *stackIterator_;
        case Phase::kTLS:
            return **tlsIterator_;
        case Phase::kDone:
            RuntimeFail("Cannot dereference");
    }
}

mm::RootSet::ThreadRootSetIterator& mm::RootSet::ThreadRootSetIterator::operator++() noexcept {
    switch (phase_) {
        case Phase::kStack:
            ++stackIterator_;
            Init();
            return *this;
        case Phase::kTLS:
            ++tlsIterator_;
            Init();
            return *this;
        case Phase::kDone:
            return *this;
    }
}

bool mm::RootSet::ThreadRootSetIterator::operator==(const ThreadRootSetIterator& rhs) const noexcept {
    if (phase_ != rhs.phase_) {
        return false;
    }

    switch (phase_) {
        case Phase::kDone:
            return true;
        case Phase::kStack:
            return stackIterator_ == rhs.stackIterator_;
        case Phase::kTLS:
            return tlsIterator_ == rhs.tlsIterator_;
    }
}

void mm::RootSet::ThreadRootSetIterator::Init() noexcept {
    while (phase_ != Phase::kDone) {
        switch (phase_) {
            case Phase::kStack:
                if (stackIterator_ != owner_.stack_.end()) return;
                phase_ = Phase::kTLS;
                tlsIterator_ = owner_.tls_.begin();
                break;
            case Phase::kTLS:
                if (tlsIterator_ != owner_.tls_.end()) return;
                phase_ = Phase::kDone;
                break;
            case Phase::kDone:
                RuntimeFail("Impossible");
        }
    }
}

mm::RootSet::GlobalRootSetIterator::GlobalRootSetIterator(begin_t, GlobalRootSetIterable& owner) noexcept :
    owner_(owner), phase_(Phase::kGlobals), globalsIterator_(owner_.globalsIterable_.begin()) {
    Init();
}

mm::RootSet::GlobalRootSetIterator::GlobalRootSetIterator(end_t, GlobalRootSetIterable& owner) noexcept :
    owner_(owner), phase_(Phase::kDone) {}

ObjHeader*& mm::RootSet::GlobalRootSetIterator::operator*() noexcept {
    switch (phase_) {
        case Phase::kGlobals:
            return **globalsIterator_;
        case Phase::kStableRefs:
            return *stableRefsIterator_;
        case Phase::kDone:
            RuntimeFail("Cannot dereference");
    }
}

mm::RootSet::GlobalRootSetIterator& mm::RootSet::GlobalRootSetIterator::operator++() noexcept {
    switch (phase_) {
        case Phase::kGlobals:
            ++globalsIterator_;
            Init();
            return *this;
        case Phase::kStableRefs:
            ++stableRefsIterator_;
            Init();
            return *this;
        case Phase::kDone:
            return *this;
    }
}

bool mm::RootSet::GlobalRootSetIterator::operator==(const GlobalRootSetIterator& rhs) const noexcept {
    if (phase_ != rhs.phase_) {
        return false;
    }

    switch (phase_) {
        case Phase::kDone:
            return true;
        case Phase::kGlobals:
            return globalsIterator_ == rhs.globalsIterator_;
        case Phase::kStableRefs:
            return stableRefsIterator_ == rhs.stableRefsIterator_;
    }
}

void mm::RootSet::GlobalRootSetIterator::Init() noexcept {
    while (phase_ != Phase::kDone) {
        switch (phase_) {
            case Phase::kGlobals:
                if (globalsIterator_ != owner_.globalsIterable_.end()) return;
                phase_ = Phase::kStableRefs;
                stableRefsIterator_ = owner_.stableRefsIterable_.begin();
                break;
            case Phase::kStableRefs:
                if (stableRefsIterator_ != owner_.stableRefsIterable_.end()) return;
                phase_ = Phase::kDone;
                break;
            case Phase::kDone:
                RuntimeFail("Impossible");
        }
    }
}

mm::RootSet::ThreadRootSetIterable mm::RootSet::ThreadIter(ThreadData& threadData) noexcept {
    return ThreadRootSetIterable(threadData.shadowStack(), threadData.tls());
}

mm::RootSet::GlobalRootSetIterable mm::RootSet::GlobalIter() noexcept {
    auto& globalData = mm::GlobalData::Instance();
    return GlobalRootSetIterable(globalData.globalsRegistry(), globalData.stableRefRegistry());
}
