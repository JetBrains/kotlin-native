/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Cleaner.h"

#include "Memory.h"
#include "Runtime.h"
#include "Worker.h"

// Defined in Cleaner.kt
extern "C" void Kotlin_CleanerImpl_shutdownCleanerWorker(bool);

namespace {

struct CleanerImpl {
  ObjHeader header;
  KInt worker;
  KRef clean;
};

bool cleanersDisabled = false;

void disposeCleaner(CleanerImpl* thiz) {
    if (atomicGet(&cleanersDisabled)) {
        if (Kotlin_cleanersLeakCheckerEnabled()) {
            konan::consoleErrorf(
                    "Cleaner %p was disposed during program exit\n"
                    "Use `Platform.isCleanersLeakCheckerActive = false` to avoid this check.\n",
                    thiz);
            RuntimeCheck(false, "Terminating now");
        }
        return;
    }

    WorkerSchedule(thiz->worker, thiz->clean);
}

} // namespace

RUNTIME_NOTHROW void DisposeCleaner(KRef thiz) {
#if KONAN_NO_EXCEPTIONS
    disposeCleaner(reinterpret_cast<CleanerImpl*>(thiz));
#else
    try {
        disposeCleaner(reinterpret_cast<CleanerImpl*>(thiz));
    } catch (...) {
        // A trick to terminate with unhandled exception. This will print a stack trace
        // and write to iOS crash log.
        std::terminate();
    }
#endif
}

void ShutdownCleaners(bool executeScheduledCleaners) {
    atomicSet(&cleanersDisabled, true);
    Kotlin_CleanerImpl_shutdownCleanerWorker(executeScheduledCleaners);
}
