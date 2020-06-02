/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Exceptions.h"
#include "MemoryPrivate.hpp"
#include "MemorySharedRefs.hpp"
#include "Runtime.h"
#include "Types.h"

extern "C" {
// Returns a string describing object at `address` of type `typeInfo`.
OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address);
}  // extern "C"

namespace {

inline bool isForeignRefAccessible(ObjHeader* object, ForeignRefContext context) {
  if (!Kotlin_hasRuntime()) {
    // So the object is either unowned or shared.
    // In the former case initialized runtime is required to throw exceptions
    // in the latter case -- to provide proper execution context for caller.
    Kotlin_initRuntimeIfNeeded();
  }

  return IsForeignRefAccessible(object, context);
}

RUNTIME_NORETURN inline void throwIllegalSharingException(ObjHeader* object) {
  // TODO: add some info about the context.
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  ThrowIllegalObjectSharingException(object->type_info(), object);
}

RUNTIME_NORETURN inline void terminateWithIllegalSharingException(ObjHeader* object) {
#if KONAN_NO_EXCEPTIONS
  // This will terminate the program.
  throwIllegalSharingException(object);
#else
  // A trick to terminate with unhandled exception. This will print a stack trace
  // and write to iOS crash log.
  try {
    throwIllegalSharingException(object);
  } catch (...) {
    std::terminate();
  }
#endif
}

}  // namespace

void KRefSharedHolder::initLocal(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  context_ = InitLocalForeignRef(obj);
  obj_ = obj;
}

void KRefSharedHolder::init(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  context_ = InitForeignRef(obj);
  obj_ = obj;
}

namespace {
template<ErrorHandlingPolicy> RUNTIME_NORETURN void error(KRef obj);

template<> RUNTIME_NORETURN void error<doTerminate>(KRef obj) {
  terminateWithIllegalSharingException(obj);
}

template<> RUNTIME_NORETURN void error<doThrow>(KRef obj) {
  throwIllegalSharingException(obj);
}
}

template <ErrorHandlingPolicy eh> ObjHeader* KRefSharedHolder::ref() const {
  if (auto* result = refOrNull())
    return result;
  error<eh>(obj_);
}

ObjHeader* KRefSharedHolder::refOrNull() const {
  if (!isRefAccessible()) {
    return nullptr;
  }
  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

void KRefSharedHolder::dispose() const {
  if (obj_ == nullptr) {
    // To handle the case when it is not initialized. See [KotlinMutableSet/Dictionary dealloc].
    return;
  }

  DeinitForeignRef(obj_, context_);
}

OBJ_GETTER0(KRefSharedHolder::describe) const {
  // Note: retrieving 'type_info()' is supposed to be correct even for unowned object.
  RETURN_RESULT_OF(DescribeObjectForDebugging, obj_->type_info(), obj_);
}

bool KRefSharedHolder::isRefAccessible() const {
  return isForeignRefAccessible(obj_, context_);
}

void BackRefFromAssociatedObject::initAndAddRef(ObjHeader* obj) {
  RuntimeAssert(obj != nullptr, "must not be null");
  obj_ = obj;

  // Generally a specialized addRef below:
  context_ = InitForeignRef(obj);
  refCount = 1;
}


template <ErrorHandlingPolicy eh>
void BackRefFromAssociatedObject::addRef() {
  if (atomicAdd(&refCount, 1) == 1) {
    // There are no references to the associated object itself, so Kotlin object is being passed from Kotlin,
    // and it is owned therefore.
    ensureRefAccessible<eh>(); // TODO: consider removing explicit verification.

    // Foreign reference has already been deinitialized (see [releaseRef]).
    // Create a new one:
    context_ = InitForeignRef(obj_);
  }
}

template <ErrorHandlingPolicy eh>
bool BackRefFromAssociatedObject::tryAddRef() {
  // Suboptimal but simple:
  this->ensureRefAccessible();
  ObjHeader* obj = this->obj_;

  if (!TryAddHeapRef(obj)) return false;
  this->addRef<eh>();
  ReleaseHeapRef(obj); // Balance TryAddHeapRef.
  // TODO: consider optimizing for non-shared objects.

  return true;
}

void BackRefFromAssociatedObject::releaseRef() {
  ForeignRefContext context = context_;
  if (atomicAdd(&refCount, -1) == 0) {
    // Note: by this moment "subsequent" addRef may have already happened and patched context_.
    // So use the value loaded before refCount update:
    DeinitForeignRef(obj_, context);
    // From this moment [context] is generally a dangling pointer.
    // This is handled in [IsForeignRefAccessible] and [addRef].
  }
}

ObjHeader* BackRefFromAssociatedObject::refOrNull() const {
  if (!isRefAccessible()) {
    return nullptr;
  }
  AdoptReferenceFromSharedVariable(obj_);
  return obj_;
}

ObjHeader* BackRefFromAssociatedObject::ref() const {
  if (auto* result = refOrNull())
    return result;

  terminateWithIllegalSharingException(obj_);
}

bool BackRefFromAssociatedObject::isRefAccessible() const {
  return isForeignRefAccessible(obj_, context_);
}

template <ErrorHandlingPolicy eh>
void BackRefFromAssociatedObject::ensureRefAccessible() const {
  if (isRefAccessible())
    return;

  error<eh>(obj_);
}

extern "C" {
RUNTIME_NOTHROW void KRefSharedHolder_initLocal(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->initLocal(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_init(KRefSharedHolder* holder, ObjHeader* obj) {
  holder->init(obj);
}

RUNTIME_NOTHROW void KRefSharedHolder_dispose(const KRefSharedHolder* holder) {
  holder->dispose();
}

RUNTIME_NOTHROW ObjHeader* KRefSharedHolder_ref(const KRefSharedHolder* holder) {
  return holder->ref();
}
} // extern "C"
