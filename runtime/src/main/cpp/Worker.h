#ifndef RUNTIME_WORKER_H
#define RUNTIME_WORKER_H

#include "Common.h"
#include "Types.h"

class Worker;

Worker* WorkerInit(KBoolean errorReporting, bool isMainThread);
// Returns true if completed successfully.
bool WorkerDeinit(Worker* worker);

Worker* WorkerSuspend();
void WorkerResume(Worker* worker);

#endif // RUNTIME_WORKER_H