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

#ifndef RUNTIME_RUNTIME_H
#define RUNTIME_RUNTIME_H

#include "Porting.h"

struct InitNode;

#ifdef __cplusplus
extern "C" {
#endif

void Kotlin_initRuntimeIfNeeded();

// Kotlin runtime cannot be created in this process after this call.
// This does not give any guarantees as to what will be destroyed.
void Kotlin_destroyRuntime();

// Appends given node to an initializer list.
void AppendToInitializersTail(struct InitNode*);

bool Kotlin_memoryLeakCheckerEnabled();

bool Kotlin_cleanersLeakCheckerEnabled();

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_RUNTIME_H
