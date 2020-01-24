/*
 * Copyright 2010-2020 JetBrains s.r.o.
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
#ifndef KONAN_NO_THREADS
#define WITH_WORKERS 1
#endif

#include "Alloc.h"
#include "Atomic.h"
#include "KAssert.h"
#include "Memory.h"
#include "Natives.h"
#include "Porting.h"
#include "Types.h"

#if WITH_WORKERS
#include <pthread.h>
#include <sys/time.h>
#endif

#if WITH_WORKERS

// Define to 1 to print collector traces.
#define TRACE_COLLECTOR 0

#if TRACE_COLLECTOR
#define COLLECTOR_LOG(...) konan::consolePrintf(__VA_ARGS__);
#else
#define COLLECTOR_LOG(...)
#endif

/**
 * Theory of operations:
 *
 * Kotlin/Native runtime has concurrent cyclic garbage collection for the shared mutable objects,
 * such as `AtomicReference` and `FreezableAtomicReference` instances (further known as the atomic rootset).
 * We perform such analysis by iterating over the transitive closure of the atomic rootset, and computing
 * aggregated inner reference counter for rootset elements over this transitive closure.
 * Collector runs in its own thread and is started by an explicit request or after certain time interval since last
 * collection passes, thus its operation does not affect UI responsiveness in most cases.
 * Atomic rootset is built by maintaining the set of all atomic and freezable atomic references objects.
 * Elements whose transitive closure inner reference count matches the actual reference count are ones
 * belonging to the garbage cycles and thus can be discarded.
 * We ignore elements reachable from objects having external references (i.e. inner rc != real rc).
 * If during computations of the aggregated RC there were modifications in the reference counts of
 * elements of the atomic rootset:
 *   - if it is being increased, then someone already got an external reference to this element, thus we may not
 *     end up matching the inner reference count anyway
 *   - if it is being decreased and object become garbage, it will be collected next time
 * If transitive closure of the atomic rootset mutates, it could only happen via changing the atomics references,
 * as all elements of this closure are frozen.
 * To handle such mutations we keep collector flag, which is cleared before analysis and set on every
 * atomic reference value update. If flag's value changes - collector restarts its analysis.
 * There are not so much of complications in this algorithm due to the delayed reference counting as if there's a
 * stack reference to the shared object - it's reflected in the reference counter (see rememberNewContainer()).
 * We release objects found by the collector on a rendezvouz callback, but not on the main thread,
 * to keep UI responsive, as taking GC lock can take time, sometimes.
 */
namespace {

class Locker {
  pthread_mutex_t* lock_;

 public:
  Locker(pthread_mutex_t* alock): lock_(alock) {
    pthread_mutex_lock(lock_);
  }

  ~Locker() {
    pthread_mutex_unlock(lock_);
  }
};

template <typename func>
inline void traverseObjectFields(ObjHeader* obj, func process) {
  RuntimeAssert(obj != nullptr, "Must be non null");
  const TypeInfo* typeInfo = obj->type_info();
  if (typeInfo != theArrayTypeInfo) {
    for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
      ObjHeader** location = reinterpret_cast<ObjHeader**>(
          reinterpret_cast<uintptr_t>(obj) + typeInfo->objOffsets_[index]);
      process(location);
    }
  } else {
    ArrayHeader* array = obj->array();
    for (int index = 0; index < array->count_; index++) {
      process(ArrayAddressOfElementAt(array, index));
    }
  }
}

#define CHECK_CALL(call, message) RuntimeCheck((call) == 0, message)

class CyclicCollector {
  pthread_mutex_t lock_;
  pthread_cond_t cond_;
  pthread_t gcThread_;

  int currentAliveWorkers_;
  int gcRunning_;
  int mutatedAtomics_;
  int pendingRelease_;
  bool shallCollectGarbage_;
  bool shallRunCollector_;
  bool terminateCollector_;
  int32_t currentTick_;
  int32_t lastTick_;
  int64_t lastTimestampUs_;
  void* mainWorker_;
  KStdUnorderedMap<ObjHeader*, int> sideRefCounts_;
  KStdUnorderedSet<ObjHeader*> rootset_;
  KStdUnorderedSet<ObjHeader*> toRelease_;

 public:
  CyclicCollector() {
    CHECK_CALL(pthread_mutex_init(&lock_, nullptr), "Cannot init collector mutex")
    CHECK_CALL(pthread_cond_init(&cond_, nullptr), "Cannot init collector condition")
    CHECK_CALL(pthread_create(&gcThread_, nullptr, gcWorkerRoutine, this), "Cannot start collector thread")
  }

  ~CyclicCollector() {
    {
      Locker locker(&lock_);
      terminateCollector_ = true;
      shallRunCollector_ = true;
      CHECK_CALL(pthread_cond_signal(&cond_), "Cannot signal collector")
    }
    // TODO: improve waiting for collector termination.
    while (atomicGet(&terminateCollector_)) {}
    releasePendingUnlocked(nullptr);
    pthread_cond_destroy(&cond_);
    pthread_mutex_destroy(&lock_);
  }

  static void* gcWorkerRoutine(void* argument) {
    CyclicCollector* thiz = reinterpret_cast<CyclicCollector*>(argument);
    thiz->gcProcessor();
    return nullptr;
  }

  void gcProcessor() {
     {
       Locker locker(&lock_);
       KStdDeque<ObjHeader*> toVisit;
       KStdUnorderedSet<ObjHeader*> visited;
       int restartCount = 0;
       while (!terminateCollector_) {
         CHECK_CALL(pthread_cond_wait(&cond_, &lock_), "Cannot wait collector condition")
         if (!shallRunCollector_) continue;
         atomicSet(&gcRunning_, 1);
         restartCount = 0;
        restart:
         if (restartCount > 10) {
           COLLECTOR_LOG("wait for some time to avoid GC trashing\n");
           struct timeval tv;
           struct timespec ts;
           long long nsDelta = 1000LL * 1000LL;
           ts.tv_nsec = (tv.tv_usec * 1000LL + nsDelta) % 1000000000LL;
           ts.tv_sec = (tv.tv_sec * 1000000000LL + nsDelta) / 1000000000LL;
           pthread_cond_timedwait(&cond_, &lock_, &ts);
         }
         atomicSet(&mutatedAtomics_, 0);
         visited.clear();
         toVisit.clear();
         sideRefCounts_.clear();
         for (auto* root: rootset_) {
           // We only care about frozen values here, as only they could become part of shared cycles.
           if (!root->container()->frozen()) continue;
           COLLECTOR_LOG("process root %p\n", root);
           traverseObjectFields(root, [&toVisit, &visited](ObjHeader** location) {
             ObjHeader* ref = *location;
             if (ref != nullptr && visited.count(ref) == 0) {
               toVisit.push_back(ref);
               COLLECTOR_LOG("adding %p for visiting\n", ref);
             }
           });
           if (atomicGet(&mutatedAtomics_) != 0) {
             COLLECTOR_LOG("restarted during visiting collect\n")
             restartCount++;
             goto restart;
           }
           while (toVisit.size() > 0)  {
             auto* obj = toVisit.front();
             toVisit.pop_front();
             COLLECTOR_LOG("visit %p\n", obj);
             auto* objContainer = obj->container();
             if (objContainer == nullptr) continue;  // Permanent object.
             if (!objContainer->frozen()) continue;
             sideRefCounts_[obj]++;
             visited.insert(obj);
             if (atomicGet(&mutatedAtomics_) != 0) {
               COLLECTOR_LOG("restarted during rootset visit\n")
               restartCount++;
               goto restart;
             }
             traverseObjectFields(obj, [&toVisit, &visited](ObjHeader** location) {
               ObjHeader* ref = *location;
               if (ref != nullptr && visited.count(ref) == 0) {
                 toVisit.push_back(ref);
               }
             });
           }
         }
         // Now find all elements with external references, and mark objects reachable from them as non suitable
         // for collection by setting their side reference count to -1.
         toVisit.clear();
         for (auto it: sideRefCounts_) {
           auto* obj = it.first;
           auto* objContainer = obj->container();
           if (objContainer == nullptr) continue;  // Permanent object.
           if (it.second != objContainer->refCount()) {
             toVisit.push_back(it.first);
           }
         }
         visited.clear();
         while (toVisit.size() > 0)  {
           auto* obj = toVisit.front();
           toVisit.pop_front();
           auto* objContainer = obj->container();
           if (objContainer == nullptr) continue;  // Permanent object.
           if (!objContainer->frozen()) continue;
           sideRefCounts_[obj] = -1;
           visited.insert(obj);
           if (atomicGet(&mutatedAtomics_) != 0) {
             COLLECTOR_LOG("restarted during reachable visit\n")
             restartCount++;
             goto restart;
           }
           traverseObjectFields(obj, [&toVisit, &visited](ObjHeader** location) {
              ObjHeader* ref = *location;
              if (ref != nullptr && (visited.count(ref) == 0)) {
                toVisit.push_back(ref);
              }
           });
         }
         // Now release all atomic roots with matching reference counters, as only their destruction is controlled.
         for (auto it: sideRefCounts_) {
           auto* obj = it.first;
           // Only do that for atomic rootset elements.
           if ((obj->type_info()->flags_ & TF_LEAK_DETECTOR_CANDIDATE) == 0) {
             continue;
           }
           auto* objContainer = obj->container();
           if (!objContainer->frozen()) continue;
           COLLECTOR_LOG("for %p inner %d actual %d\n", obj, it.second, objContainer->refCount());
           // All references are inner. We compare the number of counted
           // inner references with the number of non-stack references and per-thread ownership value
           // (see rememberNewContainer()).
           if (it.second == objContainer->refCount()) {
             COLLECTOR_LOG("adding %p to release candidates\n", it.first);
             toRelease_.insert(it.first);
           }
         }
         if (toRelease_.size() > 0)
           atomicSet(&pendingRelease_, 1);
         atomicSet(&gcRunning_, 0);
         shallRunCollector_ = false;
       }
     }
     atomicSet(&terminateCollector_, false);
  }

  void addWorker(void* worker) {
    Locker lock(&lock_);
    currentAliveWorkers_++;
    if (mainWorker_ == nullptr) mainWorker_ = worker;
  }

  void removeWorker(void* worker) {
    Locker lock(&lock_);
    // When exiting the worker - we shall collect the cyclic garbage here.
    shallCollectGarbage_ = true;
    shallRunCollector_ = true;
    CHECK_CALL(pthread_cond_signal(&cond_), "Cannot signal collector")
    currentAliveWorkers_--;
  }

  void addRoot(ObjHeader* obj) {
    COLLECTOR_LOG("add root %p\n", obj);
    // TODO: we can only add root when collector is not processing, which looks like a limitation,
    //  instead we can add elements to the side buffer or have a separate lock for that.
    Locker lock(&lock_);
    rootset_.insert(obj);
  }

  void removeRoot(ObjHeader* obj) {
    COLLECTOR_LOG("remove root %p\n", obj);
    // Note that we can only remove root when the collector is not processing.
    Locker lock(&lock_);
    toRelease_.erase(obj);
    rootset_.erase(obj);
  }

  void mutateRoot(ObjHeader* newValue) {
    // TODO: consider optimization, when clearing value (setting to null) in atomic reference shall not lead
    //   to invalidation of the collector analysis state.
    atomicSet(&mutatedAtomics_, 1);
  }

  bool checkIfShallCollect() {
    auto tick = atomicAdd(&currentTick_, 1);
    if (shallCollectGarbage_) return true;
    auto delta = tick - atomicGet(&lastTick_);
    if (delta > 10 || delta < 0) {
      auto currentTimestampUs = konan::getTimeMicros();
      if (currentTimestampUs - atomicGet(&lastTimestampUs_) > 10000) {
        // TODO: maybe take another lock.
        Locker locker(&lock_);
        lastTick_ = currentTick_;
        lastTimestampUs_ = currentTimestampUs;
        shallCollectGarbage_ = true;
        return true;
      }
    }
    return false;
  }

  void releasePendingUnlocked(void* worker) {
    // We are not doing that on the UI thread, as taking lock is slow, unless
    // it happens on deinit of the collector or if there are no other workers.
    if ((atomicGet(&pendingRelease_) != 0) && ((worker != mainWorker_) || (currentAliveWorkers_ == 1))) {
      Locker locker(&lock_);
      COLLECTOR_LOG("clearing %d release candidates on %p\n", toRelease_.size(), worker);
      for (auto* it: toRelease_) {
        COLLECTOR_LOG("clear references in %p\n", it)
        traverseObjectFields(it, [](ObjHeader** location) {
          ZeroHeapRef(location);
        });
      }
      toRelease_.clear();
      atomicSet(&pendingRelease_, 0);
    }
  }

  void collectorCallaback(void* worker) {
    if (atomicGet(&gcRunning_) != 0) return;
    releasePendingUnlocked(worker);
    if (checkIfShallCollect()) {
      Locker locker(&lock_);
      shallRunCollector_ = true;
      CHECK_CALL(pthread_cond_signal(&cond_), "Cannot signal collector")
    }
  }

  void scheduleGarbageCollect() {
    if (atomicGet(&gcRunning_) != 0) return;
    Locker lock(&lock_);
    shallRunCollector_ = true;
    CHECK_CALL(pthread_cond_signal(&cond_), "Cannot signal collector")
  }

  void localGC() {
    // We just need to take GC lock here, to avoid release of object we walk on.
    Locker locker(&lock_);
  }

};

CyclicCollector* cyclicCollector = nullptr;

}  // namespace

#endif  // WITH_WORKERS

void cyclicInit() {
#if WITH_WORKERS
  RuntimeAssert(cyclicCollector == nullptr, "Must be not yet inited");
  cyclicCollector = konanConstructInstance<CyclicCollector>();
#endif
}

void cyclicDeinit() {
#if WITH_WORKERS
  RuntimeAssert(cyclicCollector != nullptr, "Must be inited");
  auto* local = cyclicCollector;
  cyclicCollector = nullptr;
  konanDestructInstance(local);
#endif  // WITH_WORKERS
}

void cyclicAddWorker(void* worker) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->addWorker(worker);
#endif  // WITH_WORKERS
}

void cyclicRemoveWorker(void* worker) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->removeWorker(worker);
#endif  // WITH_WORKERS
}

void cyclicCollectorCallback(void* worker) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->collectorCallaback(worker);
#endif  // WITH_WORKERS
}

void cyclicScheduleGarbageCollect() {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->scheduleGarbageCollect();
#endif  // WITH_WORKERS
}

void cyclicAddAtomicRoot(ObjHeader* obj) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->addRoot(obj);
#endif  // WITH_WORKERS
}

void cyclicRemoveAtomicRoot(ObjHeader* obj) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->removeRoot(obj);
#endif  // WITH_WORKERS
}

void cyclicMutateAtomicRoot(ObjHeader* newValue) {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->mutateRoot(newValue);
#endif  // WITH_WORKERS
}

void cyclicLocalGC() {
#if WITH_WORKERS
  if (cyclicCollector)
    cyclicCollector->localGC();
#endif  // WITH_WORKERS
}