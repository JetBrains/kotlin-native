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

#include "KAssert.h"
#include "TypeInfo.h"

// If one shall use binary search when looking up methods and fields.
// TODO: maybe select strategy basing on number of elements.
#define USE_BINARY_SEARCH 1

extern "C" {
#if USE_BINARY_SEARCH

void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature) {
  int bottom = 0;
  int top = info->openMethodsCount_ - 1;

  while (bottom <= top) {
    int middle = (bottom + top) / 2;
    if (info->openMethods_[middle].nameSignature_ < nameSignature)
      bottom = middle + 1;
    else if (info->openMethods_[middle].nameSignature_ == nameSignature)
      return info->openMethods_[middle].methodEntryPoint_;
    else
      top = middle - 1;
  }

  RuntimeAssert(false, "Unknown open method");
  return nullptr;
}

#else

void* LookupOpenMethod(const TypeInfo* info, MethodNameHash nameSignature) {
  for (int i = 0; i < info->openMethodsCount_; ++i) {
    if (info->openMethods_[i].nameSignature_ == nameSignature) {
      return info->openMethods_[i].methodEntryPoint_;
    }
  }
  RuntimeAssert(false, "Unknown open method");
  return nullptr;
}

#endif

void* Kotlin_KClass_getCustomInfo(TypeInfo* typeInfo) {
  if ((typeInfo->flags_ & TF_CUSTOM_INFO_RUNTIME_USED) != 0) return nullptr;
  return typeInfo->customInfo_;
}

void Kotlin_KClass_setCustomInto(TypeInfo* typeInfo, void* info) {
  if ((typeInfo->flags_ & TF_CUSTOM_INFO_RUNTIME_USED) != 0) return;
  typeInfo->customInfo_ = reinterpret_cast<CustomTypeInfo*>(info);
}

}  // extern "C"
