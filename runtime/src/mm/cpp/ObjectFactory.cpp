/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectFactory.hpp"

#include "GlobalData.hpp"
#include "Memory.h"
#include "Types.h"

using namespace kotlin;

bool mm::ObjectFactory::Iterator::IsArray() noexcept {
    // `ArrayHeader` and `ObjHeader` are kept compatible, so the former can
    // be always casted to the other.
    auto* object = static_cast<ObjHeader*>((*iterator_).Data());
    return object->type_info()->IsArray();
}

ObjHeader* mm::ObjectFactory::Iterator::GetObjHeader() noexcept {
    auto* object = static_cast<ObjHeader*>((*iterator_).Data());
    RuntimeAssert(!object->type_info()->IsArray(), "Must not be an array");
    return object;
}

ArrayHeader* mm::ObjectFactory::Iterator::GetArrayHeader() noexcept {
    auto* array = static_cast<ArrayHeader*>((*iterator_).Data());
    RuntimeAssert(array->type_info()->IsArray(), "Must be an array");
    return array;
}

mm::ObjectFactory::ObjectFactory() noexcept = default;
mm::ObjectFactory::~ObjectFactory() = default;

// static
mm::ObjectFactory& mm::ObjectFactory::Instance() noexcept {
    return GlobalData::Instance().objectFactory();
}
