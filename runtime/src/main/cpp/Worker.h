#ifndef RUNTIME_WORKER_H
#define RUNTIME_WORKER_H

#include "Common.h"
#include "Types.h"

class Worker;

Worker* WorkerInit(KBoolean errorReporting);
// Returns true if no leaks were detected.
bool WorkerDeinit(Worker* worker, bool checkLeaks);

Worker* WorkerSuspend();
void WorkerResume(Worker* worker);

#endif // RUNTIME_WORKER_H