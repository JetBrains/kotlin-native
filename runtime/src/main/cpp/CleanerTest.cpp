/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include "gtest/gtest.h"

// TODO: Test concurrent creation of cleaner workers with possible shutdown.
// TODO: Also test disposal. (This requires extracting Worker interface)

TEST(CleanerTest, ExampleTest) {
  EXPECT_EQ(true, false);
}
