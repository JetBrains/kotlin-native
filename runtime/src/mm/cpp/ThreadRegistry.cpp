/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

namespace kotlin {
namespace mm {

// static
ThreadRegistry ThreadRegistry::instance_;

// static
thread_local ThreadData* ThreadRegistry::currentThreadData_ = nullptr;

} // namespace mm
} // namespace kotlin
