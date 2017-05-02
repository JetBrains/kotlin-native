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

struct RuntimeState;
struct InitNode;

#ifdef __cplusplus
extern "C" {
#endif

RuntimeState* InitRuntime();
void DeinitRuntime(RuntimeState* state);

// Appends given node to an initializer list.
void AppendToInitializersTail(struct InitNode*);

#ifdef __cplusplus
}
#endif

#endif // RUNTIME_RUNTIME_H
