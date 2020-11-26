/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_GLOBALS_REGISTRY_H
#define RUNTIME_MM_GLOBALS_REGISTRY_H

#include <mutex>
#include <vector>

#include "Memory.h"
#include "Mutex.hpp"
#include "ThreadRegistry.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

class GlobalsRegistry : Pinned {
public:
    class ThreadQueue {
    private:
        friend class GlobalsRegistry;

        void AddToQueue(ObjHeader** location) noexcept;
        void DumpInto(std::vector<ObjHeader**>& storage) noexcept;

        std::vector<ObjHeader**> queue_;
    };

    using Iterator = std::vector<ObjHeader**>::iterator;

    class Iterable : MoveOnly {
    public:
        explicit Iterable(GlobalsRegistry* registry) noexcept : registry_(registry), guard_(registry->mutex_) {}

        Iterator begin() noexcept { return registry_->globals_.begin(); }

        Iterator end() noexcept { return registry_->globals_.end(); }

    private:
        GlobalsRegistry* registry_;
        std::unique_lock<SimpleMutex> guard_;
    };

    static GlobalsRegistry& Instance() noexcept;

    void RegisterStorageForGlobal(mm::ThreadData* threadData, ObjHeader** location) noexcept;

    // Collect globals from threads and lock GlobalsRegistry for safe iteration.
    Iterable CollectAndIterate(ThreadRegistry::Iterable& threadRegistry) noexcept;

private:
    std::vector<ObjHeader**> globals_;
    SimpleMutex mutex_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_GLOBALS_REGISTRY_H
