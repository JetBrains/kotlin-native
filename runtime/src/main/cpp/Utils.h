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

#ifndef RUNTIME_UTILS_H
#define RUNTIME_UTILS_H

#include <cstdint>
#include "KAssert.h"

class SimpleMutex {
 private:
  int32_t atomicInt = 0;

 public:
  void lock() {
    while (!__sync_bool_compare_and_swap(&atomicInt, 0, 1)) {
      // TODO: yield.
    }
  }

  void unlock() {
    if (!__sync_bool_compare_and_swap(&atomicInt, 1, 0)) {
      RuntimeAssert(false, "Unable to unlock");
    }
  }
};

// TODO: use std::lock_guard instead?
template <class Mutex>
class LockGuard {
 public:
  explicit LockGuard(Mutex& mutex_) : mutex(mutex_) {
    mutex.lock();
  }

  ~LockGuard() {
    mutex.unlock();
  }

 private:
  Mutex& mutex;

  LockGuard(const LockGuard&) = delete;
  LockGuard& operator=(const LockGuard&) = delete;
};


namespace kstd { // TODO: suggest better name

// Should I test for C++17 (more general) instead? #if __cplusplus >= 201703L
#ifdef __cpp_deduction_guides

/**
 * Usage:
 *   @code{.cpp}
 *   auto guard = Scoped {
 *       [&]() { lock(x); },    // lock now (constructor)
 *       [&]() { unlock(x); }   // unlock at block exit (destructor)
 *   };
 *   // Another example, at exit only, no ctor, alternative form of declaring
 *   Scoped atBlockExit { []() {
 *       log_trace << "Nested scoped guard: this should happen right before unlock!";
 *   }};
 *   @endcode
 */
template <class Exit, class Enter = Exit > class Scoped {
	const Exit atExit_;
	Scoped(const Scoped&) = delete;
	Scoped(Scoped&&) = delete;
	Scoped& operator=(const Scoped& other) = delete;
	Scoped& operator=(Scoped&&) = delete;
public:
	explicit Scoped(const Exit& teardown) : atExit_(teardown) {}
	Scoped(const Enter& init, const Exit& teardown) : Scoped(teardown) { init(); }
	~Scoped() { atExit_(); }
};

template <class F2> Scoped(F2) -> Scoped<F2, F2>;  //!<  At exit only
template <class F1, class F2> Scoped(F1, F2) -> Scoped<F2, F1>;  //!< Both ctor and dtor

#endif // #ifdef __cpp_deduction_guides

}

#endif // RUNTIME_UTILS_H