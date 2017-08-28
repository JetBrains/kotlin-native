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

#include <string.h>
#include <stdio.h>

#include <cstddef> // for offsetof

#include "Alloc.h"
#include "Assert.h"
#include "Exceptions.h"
#include "Memory.h"
#include "Natives.h"

// If garbage collection algorithm for cyclic garbage to be used.
// We are using the Bacon's algorithm for GC (http://researcher.watson.ibm.com/researcher/files/us-bacon/Bacon03Pure.pdf).
#define USE_GC 1
// Define to 1 to print all memory operations.
#define TRACE_MEMORY 0
// Trace garbage collection phases.
#define TRACE_GC_PHASES 0

ContainerHeader ObjHeader::theStaticObjectsContainer = {
  CONTAINER_TAG_PERMANENT | CONTAINER_TAG_INCREMENT
};

namespace {

// Granularity of arena container chunks.
constexpr container_size_t kContainerAlignment = 1024;
// Single object alignment.
constexpr container_size_t kObjectAlignment = 8;

#if USE_GC
// Collection threshold default (collect after having so many elements in the
// release candidates set). Better be a prime number.
constexpr size_t kGcThreshold = 9341;

typedef KStdDeque<ContainerHeader*> ContainerHeaderDeque;
#endif

}  // namespace

#if TRACE_MEMORY || USE_GC
typedef KStdUnorderedSet<ContainerHeader*> ContainerHeaderSet;
typedef KStdVector<ContainerHeader*> ContainerHeaderList;
typedef KStdVector<KRef*> KRefPtrList;
#endif

struct FrameOverlay {
  ArenaContainer* arena;
};

struct MemoryState {
  // Current number of allocated containers.
  int allocCount = 0;

#if TRACE_MEMORY
  // List of all global objects addresses.
  KRefPtrList* globalObjects;
  // Set of all containers.
  ContainerHeaderSet* containers;
#endif

#if USE_GC
  // Finalizer queue.
  ContainerHeaderDeque* finalizerQueue;

  /*
   * Typical scenario for GC is as following:
   * we have 90% of objects with refcount = 0 which will be deleted during
   * the first phase of the algorithm.
   * We could mark them with a bit in order to tell the next two phases to skip them
   * and thus requiring only one list, but the downside is that both of the
   * next phases would iterate over the whole list of objects instead of only 10%.
   */
  ContainerHeaderList* toFree; // List of all cycle candidates.
  ContainerHeaderList* roots; // Real candidates excluding those with refcount = 0.
  // How many GC suspend requests happened.
  int gcSuspendCount;
  // How many candidate elements in toFree shall trigger collection.
  size_t gcThreshold;
  // If collection is in progress.
  bool gcInProgress;
#endif // USE_GC
};

void FreeContainer(ContainerHeader* header);

namespace {

// TODO: can we pass this variable as an explicit argument?
THREAD_LOCAL_VARIABLE MemoryState* memoryState = nullptr;

constexpr int kFrameOverlaySlots = sizeof(FrameOverlay) / sizeof(ObjHeader**);

inline bool isFreeable(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) < CONTAINER_TAG_PERMANENT;
}

inline bool isPermanent(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_PERMANENT;
}

inline bool isArena(const ContainerHeader* header) {
  return (header->refCount_ & CONTAINER_TAG_MASK) == CONTAINER_TAG_STACK;
}

inline container_size_t alignUp(container_size_t size, int alignment) {
  return (size + alignment - 1) & ~(alignment - 1);
}

// TODO: shall we do padding for alignment?
inline container_size_t objectSize(const ObjHeader* obj) {
  const TypeInfo* type_info = obj->type_info();
  container_size_t size = type_info->instanceSize_ < 0 ?
      // An array.
      ArrayDataSizeBytes(obj->array()) + sizeof(ArrayHeader)
      :
      type_info->instanceSize_ + sizeof(ObjHeader);
  return alignUp(size, kObjectAlignment);
}

inline bool isArenaSlot(ObjHeader** slot) {
  return (reinterpret_cast<uintptr_t>(slot) & ARENA_BIT) != 0;
}

inline ObjHeader** asArenaSlot(ObjHeader** slot) {
  return reinterpret_cast<ObjHeader**>(
      reinterpret_cast<uintptr_t>(slot) & ~ARENA_BIT);
}

inline FrameOverlay* asFrameOverlay(ObjHeader** slot) {
  return reinterpret_cast<FrameOverlay*>(slot);
}

inline bool isRefCounted(KConstRef object) {
  return (object->container()->refCount_ & CONTAINER_TAG_MASK) ==
      CONTAINER_TAG_NORMAL;
}
} // namespace

extern "C" {
void objc_release(void* ptr);
}

inline void runDeallocationHooks(ObjHeader* obj) {
#if KONAN_OBJC_INTEROP
  if (obj->type_info() == theObjCPointerHolderTypeInfo) {
    void* objcPtr =  *reinterpret_cast<void**>(obj + 1); // TODO: use more reliable layout description
    objc_release(objcPtr);
  }
#endif
}

inline void runDeallocationHooks(ContainerHeader* container) {
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);

  for (int index = 0; index < container->objectCount(); index++) {
    runDeallocationHooks(obj);

    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
}

static inline void DeinitInstanceBodyImpl(const TypeInfo* typeInfo, void* body) {
  for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
    ObjHeader** location = reinterpret_cast<ObjHeader**>(
        reinterpret_cast<uintptr_t>(body) + typeInfo->objOffsets_[index]);
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from DeinitInstanceBodyImpl\n");
#endif
    UpdateRef(location, nullptr);
  }
}

void DeinitInstanceBody(const TypeInfo* typeInfo, void* body) {
  DeinitInstanceBodyImpl(typeInfo, body);
}

namespace {

#if USE_GC
inline void processFinalizerQueue(MemoryState* state) {
  // TODO: reuse elements of finalizer queue for new allocations.
  while (!state->finalizerQueue->empty()) {
    auto container = memoryState->finalizerQueue->back();
    state->finalizerQueue->pop_back();
    if ((reinterpret_cast<uintptr_t>(container) & 1) != 0) {
      container = reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) & ~1);
#if TRACE_MEMORY
      state->containers->erase(container);
#endif
      runDeallocationHooks(container);
    }
    konanFreeMemory(container);
    state->allocCount--;
  }
}
#endif

inline void scheduleDestroyContainer(
    MemoryState* state, ContainerHeader* container) {
#if USE_GC
  state->finalizerQueue->push_front(container);
  // We cannot clean finalizer queue while in GC.
  if (!state->gcInProgress && state->finalizerQueue->size() > 256) {
    processFinalizerQueue(state);
  }
#else
  state->allocCount--;
  konanFreeMemory(header);
#endif
}


#if !USE_GC

inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount();
}

inline void DecrementRC(ContainerHeader* container) {
  if (container->decRefCount() == 0) {
    FreeContainer(container);
  }
}

#else // USE_GC

inline uint32_t freeableSize(MemoryState* state) {
  return state->toFree->size();
}

inline void IncrementRC(ContainerHeader* container) {
  container->incRefCount();
  container->setColor(CONTAINER_TAG_GC_BLACK);
}

inline void DecrementRC(ContainerHeader* container) {
  if (container->decRefCount() == 0) {
    FreeContainer(container);
  } else { // Possible root.
    if (container->color() != CONTAINER_TAG_GC_PURPLE) {
      container->setColor(CONTAINER_TAG_GC_PURPLE);
      if (!container->buffered()) {
        container->setBuffered();
        auto state = memoryState;
        state->toFree->push_back(container);
        if (state->gcSuspendCount == 0 && freeableSize(state) > state->gcThreshold) {
          GarbageCollect();
        }
      }
    }
  }
}

inline void initThreshold(MemoryState* state, uint32_t gcThreshold) {
  state->gcThreshold = gcThreshold;
}
#endif // USE_GC

template<typename func>
void traverseContainerObjectFields(ContainerHeader* container, func process) {
  ObjHeader* obj = reinterpret_cast<ObjHeader*>(container + 1);
  for (int object = 0; object < container->objectCount(); object++) {
    const TypeInfo* typeInfo = obj->type_info();
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
      ObjHeader** location = reinterpret_cast<ObjHeader**>(
          reinterpret_cast<uintptr_t>(obj + 1) + typeInfo->objOffsets_[index]);
      process(location);
    }
    if (typeInfo == theArrayTypeInfo) {
      ArrayHeader* array = obj->array();
      for (int index = 0; index < array->count_; index++) {
        process(ArrayAddressOfElementAt(array, index));
      }
    }
    obj = reinterpret_cast<ObjHeader*>(
      reinterpret_cast<uintptr_t>(obj) + objectSize(obj));
  }
}

template<typename func>
void traverseContainerReferredObjects(ContainerHeader* container, func process) {
  traverseContainerObjectFields(container, [process](ObjHeader** location) {
    ObjHeader* ref = *location;
    if (ref != nullptr) process(ref);
  });
}

#if TRACE_MEMORY || USE_GC

void dumpWorker(const char* prefix, ContainerHeader* header, ContainerHeaderSet* seen) {
  fprintf(stderr, "%s: %p (%08x): %d refs\n",
          prefix,
          header, header->refCount_, header->refCount_ >> CONTAINER_TAG_SHIFT);
  seen->insert(header);
  traverseContainerReferredObjects(header, [prefix, seen](ObjHeader* ref) {
    auto child = ref->container();
    RuntimeAssert(!isArena(child), "A reference to local object is encountered");
    if (!isPermanent(child) && (seen->count(child) == 0)) {
      dumpWorker(prefix, child, seen);
    }
  });
}

void dumpReachable(const char* prefix, const ContainerHeaderSet* roots) {
  ContainerHeaderSet seen;
  for (auto container : *roots) {
    fprintf(stderr, "%p is root\n", container);
    dumpWorker(prefix, container, &seen);
  }
}

#endif

void MarkRoots(MemoryState*);
void DeleteCorpses(MemoryState*);
void ScanRoots(MemoryState*);
void CollectRoots(MemoryState*);
void MarkGray(ContainerHeader* container);
void Scan(ContainerHeader* container);
void ScanBlack(ContainerHeader* container);
void CollectWhite(MemoryState*, ContainerHeader* container);

void CollectCycles(MemoryState* state) {
  MarkRoots(state);
  ScanRoots(state);
  CollectRoots(state);
  state->toFree->clear();
  state->roots->clear();
}

void MarkRoots(MemoryState* state) {
  for (auto container : *(state->toFree)) {
    if ((reinterpret_cast<uintptr_t>(container) & 1) != 0)
      continue;
    auto color = container->color();
    auto rcIsZero = container->refCount() == 0;
    if (color == CONTAINER_TAG_GC_PURPLE && !rcIsZero) {
      MarkGray(container);
      state->roots->push_back(container);
    } else {
      container->resetBuffered();
      if (color == CONTAINER_TAG_GC_BLACK && rcIsZero) {
        scheduleDestroyContainer(state, reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) | 1));
      }
    }
  }
}

void ScanRoots(MemoryState* state) {
  for (auto container : *(state->roots)) {
    Scan(container);
  }
}

void CollectRoots(MemoryState* state) {
  for (auto container : *(state->roots)) {
    container->resetBuffered();
    CollectWhite(state, container);
  }
}

void MarkGray(ContainerHeader* container) {
  if (container->color() == CONTAINER_TAG_GC_GRAY) return;
  container->setColor(CONTAINER_TAG_GC_GRAY);
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!isPermanent(childContainer)) {
      childContainer->decRefCount();
      MarkGray(childContainer);
    }
  });
}

void Scan(ContainerHeader* container) {
  if (container->color() != CONTAINER_TAG_GC_GRAY) return;
  if (container->refCount() != 0) {
    ScanBlack(container);
    return;
  }
  container->setColor(CONTAINER_TAG_GC_WHITE);
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!isPermanent(childContainer)) {
      Scan(childContainer);
    }
  });
}

void ScanBlack(ContainerHeader* container) {
  container->setColor(CONTAINER_TAG_GC_BLACK);
  traverseContainerReferredObjects(container, [](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!isPermanent(childContainer)) {
      childContainer->incRefCount();
      if (childContainer->color() != CONTAINER_TAG_GC_BLACK)
        ScanBlack(childContainer);
    }
  });
}

void CollectWhite(MemoryState* state, ContainerHeader* container) {
  if (container->color() != CONTAINER_TAG_GC_WHITE
        || container->buffered())
    return;
  container->setColor(CONTAINER_TAG_GC_BLACK);
  traverseContainerReferredObjects(container, [state](ObjHeader* ref) {
    auto childContainer = ref->container();
    RuntimeAssert(!isArena(childContainer), "A reference to local object is encountered");
    if (!isPermanent(childContainer)) {
      CollectWhite(state, childContainer);
    }
  });
  scheduleDestroyContainer(state, reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) | 1));
}

inline void AddRef(ContainerHeader* header) {
  // Looking at container type we may want to skip AddRef() totally
  // (non-escaping stack objects, constant objects).
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_STACK:
    case CONTAINER_TAG_PERMANENT:
      break;
    case CONTAINER_TAG_NORMAL:
      IncrementRC(header);
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
}

inline void Release(ContainerHeader* header) {
  // Looking at container type we may want to skip Release() totally
  // (non-escaping stack objects, constant objects).
  switch (header->refCount_ & CONTAINER_TAG_MASK) {
    case CONTAINER_TAG_PERMANENT:
    case CONTAINER_TAG_STACK:
      break;
    case CONTAINER_TAG_NORMAL:
      DecrementRC(header);
      break;
    default:
      RuntimeAssert(false, "unknown container type");
      break;
  }
}

// We use first slot as place to store frame-local arena container.
// TODO: create ArenaContainer object on the stack, so that we don't
// do two allocations per frame (ArenaContainer + actual container).
inline ArenaContainer* initedArena(ObjHeader** auxSlot) {
  auto frame = asFrameOverlay(auxSlot);
#if TRACE_MEMORY
  fprintf(stderr, "Initializing arena at %p\n", frame);
#endif
  auto arena = frame->arena;
  if (!arena) {
    arena = konanConstructInstance<ArenaContainer>();
    arena->Init();
    frame->arena = arena;
  }
  return arena;
}

}  // namespace

ContainerHeader* AllocContainer(size_t size) {
  auto state = memoryState;
#if USE_GC
  // TODO: try to reuse elements of finalizer queue for new allocations, question
  // is how to get actual size of container.
#endif
  ContainerHeader* result = konanConstructSizedInstance<ContainerHeader>(size);
#if TRACE_MEMORY
  fprintf(stderr, ">>> alloc %d -> %p\n", static_cast<int>(size), result);
  state->containers->insert(result);
#endif
  state->allocCount++;
  return result;
}

void FreeContainer(ContainerHeader* header) {
  RuntimeAssert(!isPermanent(header), "this kind of container shalln't be freed");
  auto state = memoryState;
#if TRACE_MEMORY
  if (isFreeable(header)) {
    fprintf(stderr, "<<< free<FreeContainer> %p\n", header);
  }
#endif

  // Now let's clean all object's fields in this container.
  traverseContainerObjectFields(header, [](ObjHeader** location) {
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from FreeContainer\n");
#endif

    UpdateRef(location, nullptr);
  });

  // And release underlying memory.
  if (!isFreeable(header)) {
    runDeallocationHooks(header);
  } else {
    header->setColor(CONTAINER_TAG_GC_BLACK);
    if (!header->buffered()) {

      runDeallocationHooks(header);

#if TRACE_MEMORY
    memoryState->containers->erase(header);
#endif

      scheduleDestroyContainer(state, header);
    }
  }
}

void ObjectContainer::Init(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "Must be an object");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ObjHeader) + type_info->instanceSize_;
  header_ = AllocContainer(alloc_size);
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
     // header->refCount_ is zero initialized by AllocContainer().
    SetMeta(GetPlace(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "object at %p\n", GetPlace());
#endif
  }
}

void ArrayContainer::Init(const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "Must be an array");
  uint32_t alloc_size =
      sizeof(ContainerHeader) + sizeof(ArrayHeader) -
      type_info->instanceSize_ * elements;
  header_ = AllocContainer(alloc_size);
  RuntimeAssert(header_ != nullptr, "Cannot alloc memory");
  if (header_) {
    // One object in this container.
    header_->setObjectCount(1);
    // header->refCount_ is zero initialized by AllocContainer().
    GetPlace()->count_ = elements;
    SetMeta(GetPlace()->obj(), type_info);
#if TRACE_MEMORY
    fprintf(stderr, "array at %p\n", GetPlace());
#endif
  }
}

// TODO: store arena containers in some reuseable data structure, similar to
// finalizer queue.
void ArenaContainer::Init() {
  allocContainer(1024);
}

void ArenaContainer::Deinit() {
#if TRACE_MEMORY
  fprintf(stderr, "Arena::Deinit start\n");
#endif
  auto chunk = currentChunk_;
  while (chunk != nullptr) {
    // FreeContainer() doesn't release memory when CONTAINER_TAG_STACK is set.
#if TRACE_MEMORY
    fprintf(stderr, "Arena::Deinit free chunk\n");
#endif
    FreeContainer(chunk->asHeader());
    chunk = chunk->next;
  }
  chunk = currentChunk_;
  while (chunk != nullptr) {
    auto toRemove = chunk;
    chunk = chunk->next;
    konanFreeMemory(toRemove);
  }
#if TRACE_MEMORY
  fprintf(stderr, "Arena::Deinit end\n");
#endif
}

bool ArenaContainer::allocContainer(container_size_t minSize) {
  auto size = minSize + sizeof(ContainerHeader) + sizeof(ContainerChunk);
  size = alignUp(size, kContainerAlignment);
  // TODO: keep simple cache of container chunks.
  ContainerChunk* result = konanConstructSizedInstance<ContainerChunk>(size);
  RuntimeAssert(result != nullptr, "Cannot alloc memory");
  if (result == nullptr) return false;
  result->next = currentChunk_;
  result->arena = this;
  result->asHeader()->refCount_ = (CONTAINER_TAG_STACK | CONTAINER_TAG_INCREMENT);
  currentChunk_ = result;
  current_ = reinterpret_cast<uint8_t*>(result->asHeader() + 1);
  end_ = reinterpret_cast<uint8_t*>(result) + size;
  return true;
}

void* ArenaContainer::place(container_size_t size) {
  size = alignUp(size, kObjectAlignment);
  // Fast path.
  if (current_ + size < end_) {
    void* result = current_;
    current_ += size;
    return result;
  }
  if (!allocContainer(size)) {
    return nullptr;
  }
  void* result = current_;
  current_ += size;
  RuntimeAssert(current_ <= end_, "Must not overflow");
  return result;
}

#define ARENA_SLOTS_CHUNK_SIZE 16

ObjHeader** ArenaContainer::getSlot() {
  if (slots_ == nullptr || slotsCount_ >= ARENA_SLOTS_CHUNK_SIZE) {
    slots_ = PlaceArray(theArrayTypeInfo, ARENA_SLOTS_CHUNK_SIZE);
    slotsCount_ = 0;
  }
  return ArrayAddressOfElementAt(slots_, slotsCount_++);
}

ObjHeader* ArenaContainer::PlaceObject(const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  uint32_t size = type_info->instanceSize_ + sizeof(ObjHeader);
  ObjHeader* result = reinterpret_cast<ObjHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  currentChunk_->asHeader()->incObjectCount();
  setMeta(result, type_info);
  return result;
}

ArrayHeader* ArenaContainer::PlaceArray(const TypeInfo* type_info, uint32_t count) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  container_size_t size = sizeof(ArrayHeader) - type_info->instanceSize_ * count;
  ArrayHeader* result = reinterpret_cast<ArrayHeader*>(place(size));
  if (!result) {
    return nullptr;
  }
  currentChunk_->asHeader()->incObjectCount();
  setMeta(result->obj(), type_info);
  result->count_ = count;
  return result;
}

inline void AddRef(const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "AddRef on %p in %p\n", object, object->container());
#endif
  AddRef(object->container());
}

inline void ReleaseRef(const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseRef on %p in %p\n", object, object->container());
#endif
  Release(object->container());
}

extern "C" {

MemoryState* InitMemory() {
  RuntimeAssert(offsetof(ArrayHeader, type_info_)
                ==
                offsetof(ObjHeader,   type_info_),
                "Layout mismatch");
  RuntimeAssert(offsetof(ArrayHeader, container_offset_negative_)
                ==
                offsetof(ObjHeader  , container_offset_negative_),
                "Layout mismatch");
  RuntimeAssert(sizeof(FrameOverlay) % sizeof(ObjHeader**) == 0, "Frame overlay should contain only pointers")
  RuntimeAssert(memoryState == nullptr, "memory state must be clear");
  memoryState = konanConstructInstance<MemoryState>();
  // TODO: initialize heap here.
  memoryState->allocCount = 0;
#if TRACE_MEMORY
  memoryState->globalObjects = konanConstructInstance<KRefPtrList>();
  memoryState->containers = konanConstructInstance<ContainerHeaderSet>();
#endif
#if USE_GC
  memoryState->finalizerQueue = konanConstructInstance<ContainerHeaderDeque>();
  memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
  memoryState->roots = konanConstructInstance<ContainerHeaderList>();
  memoryState->gcInProgress = false;
  initThreshold(memoryState, kGcThreshold);
  memoryState->gcSuspendCount = 0;
#endif
  return memoryState;
}

void DeinitMemory(MemoryState* memoryState) {
#if TRACE_MEMORY
  // Free all global objects, to ensure no memory leaks happens.
  for (auto location: *memoryState->globalObjects) {
    fprintf(stderr, "Release global in *%p: %p\n", location, *location);
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from DeinitMemory\n");
#endif
    UpdateRef(location, nullptr);
  }
  konanDestructInstance(memoryState->globalObjects);
  memoryState->globalObjects = nullptr;
#endif

#if USE_GC
  GarbageCollect();
  RuntimeAssert(memoryState->toFree->size() == 0, "Some memory have not been released after GC");
  konanDestructInstance(memoryState->toFree);
  konanDestructInstance(memoryState->roots);

  konanDestructInstance(memoryState->finalizerQueue);
  memoryState->finalizerQueue = nullptr;

#endif // USE_GC

#if TRACE_MEMORY
  if (memoryState->allocCount > 0) {
    fprintf(stderr, "*** Memory leaks, leaked %d containers ***\n",
            memoryState->allocCount);
    dumpReachable("", memoryState->containers);
  }
  konanDestructInstance(memoryState->containers);
  memoryState->containers = nullptr;
#else
  RuntimeAssert(memoryState->allocCount == 0, "Memory leaks found");
#endif

  konanFreeMemory(memoryState);
  ::memoryState = nullptr;
}

OBJ_GETTER(AllocInstance, const TypeInfo* type_info) {
  RuntimeAssert(type_info->instanceSize_ >= 0, "must be an object");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceObject(type_info);
#if TRACE_MEMORY
    fprintf(stderr, "instance %p in arena: %p\n", result, arena);
#endif
    return result;
  }
  RETURN_OBJ(ObjectContainer(type_info).GetPlace());
}

OBJ_GETTER(AllocArrayInstance, const TypeInfo* type_info, uint32_t elements) {
  RuntimeAssert(type_info->instanceSize_ < 0, "must be an array");
  if (isArenaSlot(OBJ_RESULT)) {
    auto arena = initedArena(asArenaSlot(OBJ_RESULT));
    auto result = arena->PlaceArray(type_info, elements)->obj();
#if TRACE_MEMORY
    fprintf(stderr, "array[%d] %p in arena: %p\n", elements, result, arena);
#endif
    return result;
  }
  RETURN_OBJ(ArrayContainer(type_info, elements).GetPlace()->obj());
}

OBJ_GETTER(InitInstance,
    ObjHeader** location, const TypeInfo* type_info, void (*ctor)(ObjHeader*)) {
  ObjHeader* value = *location;

  if (value != nullptr) {
    // OK'ish, inited by someone else.
    RETURN_OBJ(value);
  }

  ObjHeader* object = AllocInstance(type_info, OBJ_RESULT);
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from InitInstance\n");
#endif
  UpdateRef(location, object);
#if KONAN_NO_EXCEPTIONS
  ctor(object);
#if TRACE_MEMORY
  memoryState->globalObjects->push_back(location);
#endif
  return object;
#else
  try {
    ctor(object);
#if TRACE_MEMORY
    memoryState->globalObjects->push_back(location);
#endif
    return object;
  } catch (...) {
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from InitInstance #2\n");
#endif
    UpdateRef(OBJ_RESULT, nullptr);
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from InitInstance #3\n");
#endif
    UpdateRef(location, nullptr);
    throw;
  }
#endif
}

void SetRef(ObjHeader** location, const ObjHeader* object) {
#if TRACE_MEMORY
  fprintf(stderr, "SetRef *%p: %p\n", location, object);
#endif
  *const_cast<const ObjHeader**>(location) = object;
  AddRef(object);
}

ObjHeader** GetReturnSlotIfArena(ObjHeader** returnSlot, ObjHeader** localSlot) {
  return isArenaSlot(returnSlot) ? returnSlot : localSlot;
}

ObjHeader** GetParamSlotIfArena(ObjHeader* param, ObjHeader** localSlot) {
  if (param == nullptr) return localSlot;
  auto container = param->container();
  if ((container->refCount_ & CONTAINER_TAG_MASK) != CONTAINER_TAG_STACK)
    return localSlot;
  auto chunk = reinterpret_cast<ContainerChunk*>(container) - 1;
  return reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(&chunk->arena) | ARENA_BIT);
}

void UpdateReturnRef(ObjHeader** returnSlot, const ObjHeader* object) {
  if (isArenaSlot(returnSlot)) {
    // Not a subject of reference counting.
    if (object == nullptr || !isRefCounted(object)) return;
    auto arena = initedArena(asArenaSlot(returnSlot));
    returnSlot = arena->getSlot();
  }
#if TRACE_MEMORY
    fprintf(stderr, "Calling UpdateRef from UpdateReturnRef\n");
#endif
  UpdateRef(returnSlot, object);
}

void UpdateRef(ObjHeader** location, const ObjHeader* object) {
  RuntimeAssert(!isArenaSlot(location), "must not be a slot");
  ObjHeader* old = *location;
  if (old != object) {
#if TRACE_MEMORY
  fprintf(stderr, "UpdateRef *%p: %p -> %p\n", location, old, object);
  fprintf(stderr, "          *%p: %p -> %p\n", location, old == nullptr ? nullptr : old->container(), object == nullptr ? nullptr : object->container());
#endif
    if (object != nullptr) {
      AddRef(object);
    }
    *const_cast<const ObjHeader**>(location) = object;
    if (old > reinterpret_cast<ObjHeader*>(1)) {
      ReleaseRef(old);
    }
  }
}

void EnterFrame(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "EnterFrame %p .. %p\n", start, start + count);
#endif
}

void LeaveFrame(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "LeaveFrame %p .. %p\n", start, start + count);
#endif
  ReleaseRefs(start + kFrameOverlaySlots, count - kFrameOverlaySlots);
  if (*start != nullptr) {
    auto arena = initedArena(start);
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame: free arena %p\n", arena);
#endif
    arena->Deinit();
    konanFreeMemory(arena);
#if TRACE_MEMORY
    fprintf(stderr, "LeaveFrame: free arena done %p\n", arena);
#endif
  }
}

void ReleaseRefs(ObjHeader** start, int count) {
#if TRACE_MEMORY
  fprintf(stderr, "ReleaseRefs %p .. %p\n", start, start + count);
#endif
  ObjHeader** current = start;
  auto state = memoryState;
  while (count-- > 0) {
    ObjHeader* object = *current;
    if (object != nullptr) {
      ReleaseRef(object);
      // Just for sanity, optional.
      *current = nullptr;
    }
    current++;
  }
}

#if USE_GC

void GarbageCollect() {
  MemoryState* state = memoryState;
  RuntimeAssert(!state->gcInProgress, "Recursive GC is disallowed");

#if TRACE_MEMORY
  fprintf(stderr, "Garbage collect\n");
#endif

  state->gcInProgress = true;

  while (state->toFree->size() > 0) {
    CollectCycles(state);
    processFinalizerQueue(state);
  }

  state->gcInProgress = false;
}

#endif // USE_GC

void Kotlin_konan_internal_GC_collect(KRef) {
#if USE_GC
  GarbageCollect();
#endif
}

void Kotlin_konan_internal_GC_suspend(KRef) {
#if USE_GC
  memoryState->gcSuspendCount++;
#endif
}

void Kotlin_konan_internal_GC_resume(KRef) {
#if USE_GC
  MemoryState* state = memoryState;
  if (state->gcSuspendCount > 0) {
    state->gcSuspendCount--;
    if (state->toFree != nullptr &&
        freeableSize(state) >= state->gcThreshold) {
      GarbageCollect();
    }
  }
#endif
}

void Kotlin_konan_internal_GC_stop(KRef) {
#if USE_GC
  if (memoryState->toFree != nullptr) {
    GarbageCollect();
    konanDestructInstance(memoryState->toFree);
    konanDestructInstance(memoryState->roots);
    memoryState->toFree = nullptr;
    memoryState->roots = nullptr;
  }
#endif
}

void Kotlin_konan_internal_GC_start(KRef) {
#if USE_GC
  if (memoryState->toFree == nullptr) {
    memoryState->toFree = konanConstructInstance<ContainerHeaderList>();
    memoryState->roots = konanConstructInstance<ContainerHeaderList>();
  }
#endif
}

void Kotlin_konan_internal_GC_setThreshold(KRef, KInt value) {
#if USE_GC
  if (value > 0) {
    initThreshold(memoryState, value);
  }
#endif
}

KInt Kotlin_konan_internal_GC_getThreshold(KRef) {
#if USE_GC
  return memoryState->gcThreshold;
#else
  return -1;
#endif
}

KNativePtr CreateStablePointer(KRef any) {
  if (any == nullptr) return nullptr;
  AddRef(any->container());
  return reinterpret_cast<KNativePtr>(any);
}

void DisposeStablePointer(KNativePtr pointer) {
  if (pointer == nullptr) return;
  KRef ref = reinterpret_cast<KRef>(pointer);
  Release(ref->container());
}

OBJ_GETTER(DerefStablePointer, KNativePtr pointer) {
  KRef ref = reinterpret_cast<KRef>(pointer);
  RETURN_OBJ(ref);
}

OBJ_GETTER(AdoptStablePointer, KNativePtr pointer) {
#ifndef KONAN_NO_THREADS
  __sync_synchronize();
#endif
  KRef ref = reinterpret_cast<KRef>(pointer);
  // Somewhat hacky.
  *OBJ_RESULT = ref;
  return ref;
}

bool ClearSubgraphReferences(ObjHeader* root, bool checked) {
#if USE_GC
  if (root != nullptr) {
    auto state = memoryState;

    auto container = root->container();
    ContainerHeaderList todo;
    ContainerHeaderSet subgraph;
    todo.push_back(container);
    while (todo.size() > 0) {
      auto header = todo.back();
      todo.pop_back();
      if (subgraph.count(header) != 0)
        continue;
      subgraph.insert(header);
#if TRACE_MEMORY
      fprintf(stderr, "Calling removeFreeable from ClearSubgraphReferences\n");
#endif
      traverseContainerReferredObjects(header, [&todo](ObjHeader* ref) {
        auto child = ref->container();
        RuntimeAssert(!isArena(child), "A reference to local object is encountered");
        if (!isPermanent(child)) {
          todo.push_back(child);
        }
      });
    }
    for (auto it = state->toFree->begin(); it != state->toFree->end(); ++it) {
      auto container = *it;
      if (subgraph.find(container) != subgraph.end()) {
        container->resetBuffered();
        container->setColor(CONTAINER_TAG_GC_BLACK);
        *it = reinterpret_cast<ContainerHeader*>(reinterpret_cast<uintptr_t>(container) | 1);
      }
    }
  }
#endif  // USE_GC
  // TODO: perform trial deletion starting from this root, if in checked mode.
  return true;
}

} // extern "C"
