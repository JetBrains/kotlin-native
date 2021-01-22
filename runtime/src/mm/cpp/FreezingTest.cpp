/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Freezing.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "FreezeHooksTestSupport.hpp"
#include "Memory.h"
#include "Utils.hpp"

using namespace kotlin;

namespace {

struct WithFreezeHook {
    static constexpr bool hasFreezeHook = true;
};

struct NoFreezeHook {
    static constexpr bool hasFreezeHook = false;
};

template <size_t Fields, typename Traits = NoFreezeHook>
class Object : private Pinned {
public:
    Object() {
        auto* type = new TypeInfo();
        type->typeInfo_ = type;
        if (Traits::hasFreezeHook) {
            type->flags_ |= TF_HAS_FREEZE_HOOK;
        } else {
            type->flags_ &= ~TF_HAS_FREEZE_HOOK;
        }
        type->objOffsetsCount_ = Fields;
        if (Fields > 0) {
            auto* offsets = new int32_t[Fields];
            for (size_t i = 0; i < Fields; ++i) {
                fields_[i] = nullptr;
                offsets[i] = reinterpret_cast<uintptr_t>(&fields_[i]) - reinterpret_cast<uintptr_t>(&header_);
            }
            type->objOffsets_ = offsets;
        }
        type->instanceSize_ = sizeof(*this);
        header_.typeInfoOrMeta_ = type;
    }

    ~Object() {
        if (header_.has_meta_object()) {
            ObjHeader::destroyMetaObject(&header_);
        }
        auto* type = header_.type_info();
        if (Fields > 0) {
            delete[] type->objOffsets_;
        }
        delete type;
    }

    ObjHeader* header() { return &header_; }

    ObjHeader*& operator[](size_t field) { return fields_[field]; }

private:
    ObjHeader header_;
    std::array<ObjHeader*, Fields> fields_;
};

template <size_t Elements, typename Traits = NoFreezeHook>
class Array : private Pinned {
public:
    Array() {
        auto* type = new TypeInfo();
        type->typeInfo_ = type;
        if (Traits::hasFreezeHook) {
            type->flags_ |= TF_HAS_FREEZE_HOOK;
        } else {
            type->flags_ &= ~TF_HAS_FREEZE_HOOK;
        }
        for (size_t i = 0; i < Elements; ++i) {
            elements_[i] = nullptr;
        }
        type->instanceSize_ = -static_cast<int32_t>(sizeof(ObjHeader*));
        header_.typeInfoOrMeta_ = type;
        header_.count_ = Elements;
    }

    ~Array() {
        auto* objectHeader = header_.obj();
        if (objectHeader->has_meta_object()) {
            ObjHeader::destroyMetaObject(objectHeader);
        }
        auto* type = header_.type_info();
        delete type;
    }

    ObjHeader* header() { return header_.obj(); }

    ObjHeader*& operator[](size_t index) { return elements_[index]; }

private:
    ArrayHeader header_;
    std::array<ObjHeader*, Elements> elements_;
};

class FreezingTest : public testing::Test {
public:
    testing::MockFunction<void(ObjHeader*)>& freezeHook() { return freezeHooks_.freezeHook(); }

private:
    FreezeHooksTestSupport freezeHooks_;
};

class TypesNames {
public:
    template <typename T>
    static std::string GetName(int i) {
        switch (i) {
            case 0:
                return "object";
            case 1:
                return "array";
            default:
                return "unknown";
        }
    }
};

template <typename T>
class FreezingEmptyNoHookTest : public FreezingTest {};
using EmptyNoHookTypes = testing::Types<Object<0, NoFreezeHook>, Array<0, NoFreezeHook>>;
TYPED_TEST_SUITE(FreezingEmptyNoHookTest, EmptyNoHookTypes, TypesNames);

template <typename T>
class FreezingEmptyWithHookTest : public FreezingTest {};
using EmptyWithHookTypes = testing::Types<Object<0, WithFreezeHook>, Array<0, WithFreezeHook>>;
TYPED_TEST_SUITE(FreezingEmptyWithHookTest, EmptyWithHookTypes, TypesNames);

template <typename T>
class FreezingNoHookTest : public FreezingTest {};
using NoHookTypes = testing::Types<Object<3, NoFreezeHook>, Array<3, NoFreezeHook>>;
TYPED_TEST_SUITE(FreezingNoHookTest, NoHookTypes, TypesNames);

template <typename T>
class FreezingWithHookTest : public FreezingTest {};
using WithHookTypes = testing::Types<Object<3, WithFreezeHook>, Array<3, WithFreezeHook>>;
TYPED_TEST_SUITE(FreezingWithHookTest, WithHookTypes, TypesNames);

} // namespace

TYPED_TEST(FreezingEmptyNoHookTest, UnfrozenByDefault) {
    TypeParam object;
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FailToEnsureNeverFrozen) {
    TypeParam object;
    ASSERT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    ASSERT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::EnsureNeverFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, Freeze) {
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FreezeTwice) {
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyNoHookTest, FreezeForbidden) {
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, Freeze) {
    TypeParam object;
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, FreezeTwice) {
    TypeParam object;
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingEmptyWithHookTest, FreezeForbidden) {
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, Freeze) {
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTwice) {
    TypeParam object;
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeForbidden) {
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTree) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeTwice) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeForbidden) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeForbiddenByField) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(field2.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingNoHookTest, FreezeTreeRecursive) {
    TypeParam object;
    TypeParam inner1;
    TypeParam inner2;
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingWithHookTest, Freeze) {
    TypeParam object;
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTwice) {
    TypeParam object;
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeForbidden) {
    TypeParam object;
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTree) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeTwice) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    // Only called for the first freeze.
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    testing::Mock::VerifyAndClearExpectations(&this->freezeHook());
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbidden) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), object.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbiddenByField) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ASSERT_TRUE(mm::EnsureNeverFrozen(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
    EXPECT_FALSE(mm::IsFrozen(field3.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeRecursive) {
    TypeParam object;
    TypeParam inner1;
    TypeParam inner2;
    object[0] = inner1.header();
    inner1[0] = inner2.header();
    inner2[0] = object.header();
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(inner1.header()));
    EXPECT_CALL(this->freezeHook(), Call(inner2.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(inner1.header()));
    EXPECT_TRUE(mm::IsFrozen(inner2.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeWithHookRewrite) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    TypeParam oldInner;
    TypeParam newInner;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    field2[0] = oldInner.header();
    ON_CALL(this->freezeHook(), Call(field2.header())).WillByDefault([&field2, &newInner](ObjHeader* obj) {
        field2[0] = newInner.header();
    });
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_CALL(this->freezeHook(), Call(newInner.header()));
    EXPECT_CALL(this->freezeHook(), Call(oldInner.header())).Times(0);
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), nullptr);
    EXPECT_TRUE(mm::IsFrozen(object.header()));
    EXPECT_TRUE(mm::IsFrozen(field1.header()));
    EXPECT_TRUE(mm::IsFrozen(field2.header()));
    EXPECT_TRUE(mm::IsFrozen(field3.header()));
    EXPECT_TRUE(mm::IsFrozen(newInner.header()));
    EXPECT_FALSE(mm::IsFrozen(oldInner.header()));
}

TYPED_TEST(FreezingWithHookTest, FreezeTreeForbiddenByHook) {
    TypeParam object;
    TypeParam field1;
    TypeParam field2;
    TypeParam field3;
    object[0] = field1.header();
    object[1] = field2.header();
    object[2] = field3.header();
    ON_CALL(this->freezeHook(), Call(field2.header())).WillByDefault([](ObjHeader* obj) { EXPECT_TRUE(mm::EnsureNeverFrozen(obj)); });
    EXPECT_CALL(this->freezeHook(), Call(object.header()));
    EXPECT_CALL(this->freezeHook(), Call(field1.header()));
    EXPECT_CALL(this->freezeHook(), Call(field2.header()));
    EXPECT_CALL(this->freezeHook(), Call(field3.header()));
    EXPECT_THAT(mm::FreezeSubgraph(object.header()), field2.header());
    EXPECT_FALSE(mm::IsFrozen(object.header()));
    EXPECT_FALSE(mm::IsFrozen(field1.header()));
    EXPECT_FALSE(mm::IsFrozen(field2.header()));
}
