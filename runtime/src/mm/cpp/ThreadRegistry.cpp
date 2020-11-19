/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

using namespace kotlin;

// static
mm::ThreadRegistry mm::ThreadRegistry::instance_;

// static
thread_local mm::ThreadData* mm::ThreadRegistry::currentThreadData_ = nullptr;
