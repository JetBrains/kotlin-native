/*
 * Copyright 2010-2018 JetBrains s.r.o.
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

#ifndef LIBLLVMEXT_UTILITY_H
#define LIBLLVMEXT_UTILITY_H

#include <llvm/Support/raw_ostream.h>

using namespace llvm;

namespace logging {
raw_ostream &info();

raw_ostream &debug();

raw_ostream &error();
}

#endif //LIBLLVMEXT_UTILITY_H
