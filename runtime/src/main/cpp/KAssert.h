/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef RUNTIME_ASSERT_H
#define RUNTIME_ASSERT_H

#include "Common.h"

// To avoid cluttering optimized code with asserts, they could be turned off.
#define KONAN_ENABLE_ASSERT 1

#define STRINGIFY(x) #x
#define TOSTRING(x) STRINGIFY(x)

RUNTIME_NORETURN void RuntimeAssertFailed(const char* location, const char* message);

// During codegeneration we set this constant to 1 or 0 to allow bitcode optimizer
// to get rid of code behind condition.
extern "C" const int KonanNeedDebugInfo;

#if KONAN_ENABLE_ASSERT
// Use RuntimeAssert() in internal state checks, which could be ignored in production.
#define RuntimeAssert(condition, message)                           \
if (KonanNeedDebugInfo && (!(condition))) {                         \
    RuntimeAssertFailed( __FILE__ ":" TOSTRING(__LINE__), message); \
}
#else
#define RuntimeAssert(condition, message)
#endif

// Use RuntimeCheck() in runtime checks that could fail due to external condition and shall lead
// to program termination. Never compiled out.
#define RuntimeCheck(condition, message)   \
  if (!(condition)) {                      \
    RuntimeAssertFailed(nullptr, message); \
  }

#endif // RUNTIME_ASSERT_H
