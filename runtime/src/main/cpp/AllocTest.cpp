/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "Alloc.h"

#include <array>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Types.h"

namespace {

class A : public KonanAllocatorAware {
public:
    using DestructorHook = testing::StrictMock<testing::MockFunction<void(int)>>;

    static thread_local DestructorHook* destructorHook;

    explicit A(int value = -1) : value_(value) {}

    ~A() { destructorHook->Call(value_); }

    int value() const { return value_; }

    bool operator==(const A& rhs) const { return value_ == rhs.value_; }

private:
    int value_;
};

// static
thread_local A::DestructorHook* A::destructorHook = nullptr;

struct B {
    explicit B(int value) : a(value) {}

    A a;
};

} // namespace

class KonanAllocatorAwareTest : public testing::Test {
public:
    KStdUniquePtr<A::DestructorHook> destructorHook;

    void SetUp() override {
        Test::SetUp();

        destructorHook = MakeUnique<A::DestructorHook>();
        A::destructorHook = destructorHook.get();
    }

    void TearDown() override {
        A::destructorHook = nullptr;
        destructorHook.reset();

        Test::TearDown();
    }
};

TEST_F(KonanAllocatorAwareTest, AllocatedOnStack) {
    A a(42);
    EXPECT_THAT(a.value(), 42);
    EXPECT_CALL(*destructorHook, Call(42));
}

TEST_F(KonanAllocatorAwareTest, AllocatedInAnotherObject) {
    // We do not control how `B` is allocated.
    B* b = new B(42);
    EXPECT_THAT(b->a.value(), 42);
    EXPECT_CALL(*destructorHook, Call(42));
    delete b;
}

TEST_F(KonanAllocatorAwareTest, AllocatedByItself) {
    A* a = new (KonanAlloc) A(42);
    EXPECT_THAT(a->value(), 42);
    EXPECT_CALL(*destructorHook, Call(42));
    delete a;
}

TEST_F(KonanAllocatorAwareTest, AllocateArray) {
    constexpr size_t kCount = 5;
    A* as = new (KonanAlloc) A[kCount];

    std::vector<int> actual;
    for (A* a = as; a != as + kCount; ++a) {
        actual.push_back(a->value());
    }
    std::array<int, kCount> expected;
    for (int& element : expected) {
        element = -1;
    }
    EXPECT_THAT(actual, testing::ElementsAreArray(expected));

    EXPECT_CALL(*destructorHook, Call(-1)).Times(kCount);
    delete[] as;
}

TEST_F(KonanAllocatorAwareTest, PlacementAllocated) {
    void* ptr = ::operator new(sizeof(A));
    A* a = new (ptr) A(42);
    EXPECT_THAT(a->value(), 42);
    EXPECT_CALL(*destructorHook, Call(42));
    a->~A();
    testing::Mock::VerifyAndClearExpectations(destructorHook.get());
    ::operator delete(ptr);
}

TEST_F(KonanAllocatorAwareTest, PlacementConstructedArray) {
    constexpr size_t kCount = 5;
    void* ptr = ::operator new[](sizeof(A) * kCount);
    A* as = new (ptr) A[kCount];

    std::vector<int> actual;
    for (A* a = as; a != as + kCount; ++a) {
        actual.push_back(a->value());
    }
    std::array<int, kCount> expected;
    for (int& element : expected) {
        element = -1;
    }
    EXPECT_THAT(actual, testing::ElementsAreArray(expected));

    EXPECT_CALL(*destructorHook, Call(-1)).Times(kCount);
    for (A* a = as; a != as + kCount; ++a) {
        a->~A();
    }
    testing::Mock::VerifyAndClearExpectations(destructorHook.get());
    ::operator delete[](ptr);
}
