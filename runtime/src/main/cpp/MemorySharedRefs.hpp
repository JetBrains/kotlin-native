/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MEMORYSHAREDREFS_HPP
#define RUNTIME_MEMORYSHAREDREFS_HPP

#include <type_traits>

#include "Memory.h"

// TODO: Generalize for uses outside this file.
enum class ErrorHandlingPolicy {
  kIgnore,
  kReturnDefault,
  kThrowException,
  kTerminate,
};

class KRefSharedHolder {
 public:
  void initLocal(ObjHeader* obj);

  void init(ObjHeader* obj);

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorHandlingPolicy errorHandlingPolicy>
  ObjHeader* ref() const;
  ObjHeader* refOrTerminate() const {
    return ref<ErrorHandlingPolicy::kTerminate>();
  }
  ObjHeader* refOrThrow() const {
    return ref<ErrorHandlingPolicy::kThrowException>();
  }
  ObjHeader* refOrNull() const {
    return ref<ErrorHandlingPolicy::kReturnDefault>();
  }

  void dispose() const;

  OBJ_GETTER0(describe) const;

 private:
  ObjHeader* obj_;
  ForeignRefContext context_;
};

static_assert(std::is_trivially_destructible<KRefSharedHolder>::value,
    "KRefSharedHolder destructor is not guaranteed to be called.");

class BackRefFromAssociatedObject {
 public:
  void initAndAddRef(ObjHeader* obj);

  // Error if refCount is zero and it's called from the wrong worker with non-frozen obj_.
  template <ErrorHandlingPolicy errorHandlingPolicy>
  void addRef();
  void addRefOrTerminate() {
    addRef<ErrorHandlingPolicy::kTerminate>();
  }
  void addRefOrThrow() {
    addRef<ErrorHandlingPolicy::kThrowException>();
  }

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorHandlingPolicy errorHandlingPolicy>
  bool tryAddRef();
  bool tryAddRefOrTerminate() {
    return tryAddRef<ErrorHandlingPolicy::kTerminate>();
  }
  bool tryAddRefOrThrow() {
    return tryAddRef<ErrorHandlingPolicy::kThrowException>();
  }

  void releaseRef();

  // Error if called from the wrong worker with non-frozen obj_.
  template <ErrorHandlingPolicy errorHandlingPolicy>
  ObjHeader* ref() const;
  ObjHeader* refOrTerminate() const {
    return ref<ErrorHandlingPolicy::kTerminate>();
  }
  ObjHeader* refOrThrow() const {
    return ref<ErrorHandlingPolicy::kThrowException>();
  }
  ObjHeader* refOrNull() const {
    return ref<ErrorHandlingPolicy::kReturnDefault>();
  }

  inline bool permanent() const {
    return obj_->permanent(); // Safe to query from any thread.
  }

 private:
  ObjHeader* obj_;
  ForeignRefContext context_;
  volatile int refCount;
};

static_assert(std::is_trivially_destructible<BackRefFromAssociatedObject>::value,
    "BackRefFromAssociatedObject destructor is not guaranteed to be called.");

#endif // RUNTIME_MEMORYSHAREDREFS_HPP
