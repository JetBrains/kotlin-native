#ifndef RUNTIME_WORKER_H
#define RUNTIME_WORKER_H

#include "Common.h"
#include "Types.h"

class Worker;

Worker* WorkerInit(KBoolean errorReporting);
void WorkerDeinit(Worker* worker);

Worker* WorkerSuspend();
void WorkerResume(Worker* worker);

size_t ActiveWorkersCount();

#endif // RUNTIME_WORKER_H