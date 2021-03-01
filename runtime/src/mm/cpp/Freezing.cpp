/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Freezing.hpp"

#include <deque>
#include <vector>

#include "ExtraObjectData.hpp"
#include "FreezeHooks.hpp"
#include "Memory.h"
#include "Natives.h"
#include "Types.h"

using namespace kotlin;

namespace {

// This is copied verbatim from legacy MM.
// TODO: Come up with a better way to iterate object fields.
template <typename func>
inline void traverseObjectFields(ObjHeader* obj, func process) {
    const TypeInfo* typeInfo = obj->type_info();
    if (typeInfo != theArrayTypeInfo) {
        for (int index = 0; index < typeInfo->objOffsetsCount_; index++) {
            ObjHeader** location = reinterpret_cast<ObjHeader**>(reinterpret_cast<uintptr_t>(obj) + typeInfo->objOffsets_[index]);
            process(*location);
        }
    } else {
        ArrayHeader* array = obj->array();
        for (uint32_t index = 0; index < array->count_; index++) {
            process(*ArrayAddressOfElementAt(array, index));
        }
    }
}

} // namespace

bool mm::IsFrozen(const ObjHeader* object) noexcept {
    if (auto* extraObjectData = mm::ExtraObjectData::Get(object)) {
        return (extraObjectData->flags() & mm::ExtraObjectData::FLAGS_FROZEN) != 0;
    }
    return false;
}

ObjHeader* mm::FreezeSubgraph(ObjHeader* root) noexcept {
    if (mm::IsFrozen(root)) return nullptr;

    std::vector<ObjHeader*> objects;
    std::vector<ObjHeader*> stack;
    // TODO: This may be a suboptimal container for the job.
    std::set<ObjHeader*> visited;
    stack.push_back(root);
    while (!stack.empty()) {
        ObjHeader* object = stack.back();
        stack.pop_back();
        if (object == nullptr || mm::IsFrozen(object)) continue;
        auto visitedResult = visited.insert(object);
        if (!visitedResult.second) continue;
        objects.push_back(object);
        RunFreezeHooks(object);
        traverseObjectFields(object, [&stack](ObjHeader* field) noexcept { stack.push_back(field); });
    }
    for (auto* object : objects) {
        if (auto* extraObjectData = mm::ExtraObjectData::Get(object)) {
            if ((extraObjectData->flags() & mm::ExtraObjectData::FLAGS_NEVER_FROZEN) != 0) return object;
        }
    }
    for (auto* object : objects) {
        auto& flags = mm::ExtraObjectData::GetOrInstall(object).flags();
        flags = static_cast<mm::ExtraObjectData::Flags>(flags | mm::ExtraObjectData::FLAGS_FROZEN);
    }
    return nullptr;
}

bool mm::EnsureNeverFrozen(ObjHeader* object) noexcept {
    auto& flags = mm::ExtraObjectData::GetOrInstall(object).flags();
    if ((flags & mm::ExtraObjectData::FLAGS_FROZEN) != 0) {
        return false;
    }
    flags = static_cast<mm::ExtraObjectData::Flags>(flags | mm::ExtraObjectData::FLAGS_NEVER_FROZEN);
    return true;
}
