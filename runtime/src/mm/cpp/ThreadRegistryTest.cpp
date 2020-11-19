/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ThreadRegistry.hpp"

#include <thread>

#include "gtest/gtest.h"

using namespace kotlin;

TEST(ThreadRegistryTest, RegisterCurrentThread) {
    std::thread t([]() {
        auto* node = mm::ThreadRegistry::instance().RegisterCurrentThread();
        auto* threadData = SingleLockList<mm::ThreadData>::ValueForNode(node);
        EXPECT_EQ(threadData, mm::ThreadRegistry::instance().CurrentThreadData());
    });
    t.join();
}
