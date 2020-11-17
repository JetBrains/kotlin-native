/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

// static
kotlin::mm::ThreadRegistry kotlin::mm::ThreadRegistry::instance_;

// static
thread_local kotlin::mm::ThreadData* kotlin::mm::ThreadRegistry::currentThreadData_ = nullptr;
