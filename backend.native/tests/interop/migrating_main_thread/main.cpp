/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "testlib_api.h"

#include <cassert>
#include <thread>

constexpr int kInitialValue = 0;
constexpr int kNewValue = 0;

#if defined(IS_LEGACY)
// Globals were reinitialized.
constexpr int kResultValue = kInitialValue;
#else
// Globals were kept.
constexpr int kResultValue = kNewValue;
#endif

int main() {
    std::thread main1([]() {
        assert(testlib_symbols()->kotlin.root.readFromA() == kInitialValue);
        testlib_symbols()->kotlin.root.writeToA(kNewValue);
        assert(testlib_symbols()->kotlin.root.readFromA() == kNewValue);
    });
    main1.join();

    std::thread main2([]() { assert(testlib_symbols()->kotlin.root.readFromA() == kResultValue); });
    main2.join();

    return 0;
}
