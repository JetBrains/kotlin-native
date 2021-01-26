/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "RefUpdates.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "ThreadData.hpp"

using namespace kotlin;

using testing::_;

namespace {

class InitSingletonTest : public testing::Test {
public:
    InitSingletonTest() {
        typeInfo_.typeInfo_ = &typeInfo_;
        typeInfo_.instanceSize_ = sizeof(ObjHeader);

        globalConstructor_ = &constructor_;
    }

    ~InitSingletonTest() {
        globalConstructor_ = nullptr;
        // Make sure to clean everything allocated by the tests.
        threadData_.objectFactoryThreadQueue().ClearForTests();
    }

    mm::ThreadData& threadData() { return threadData_; }

    testing::MockFunction<void(ObjHeader*)>& constructor() { return constructor_; }

    ObjHeader* InitThreadLocalSingleton(ObjHeader** location) {
        return mm::InitThreadLocalSingleton(&threadData_, location, &typeInfo_, constructorImpl);
    }

    OBJ_GETTER(InitSingleton, ObjHeader** location) {
        RETURN_RESULT_OF(mm::InitSingleton, &threadData_, location, &typeInfo_, constructorImpl);
    }

private:
    testing::StrictMock<testing::MockFunction<void(ObjHeader*)>> constructor_;
    // TODO: It makes sense to somehow abstract it away. Allocation in this case.
    mm::ThreadData threadData_{pthread_t{}};
    TypeInfo typeInfo_; // Only used for allocator calls, uninteresting for these tests.

    static testing::MockFunction<void(ObjHeader*)>* globalConstructor_;

    static void constructorImpl(ObjHeader* object) { globalConstructor_->Call(object); }
};

// static
testing::MockFunction<void(ObjHeader*)>* InitSingletonTest::globalConstructor_ = nullptr;

} // namespace

TEST_F(InitSingletonTest, InitThreadLocalSingleton) {
    ObjHeader* location = nullptr;

    ObjHeader* valueAtConstructor = nullptr;
    EXPECT_CALL(constructor(), Call(_)).WillOnce([&location, &valueAtConstructor](ObjHeader* value) {
        EXPECT_THAT(value, location);
        valueAtConstructor = value;
    });
    ObjHeader* value = InitThreadLocalSingleton(&location);
    EXPECT_THAT(value, location);
    EXPECT_THAT(valueAtConstructor, location);
}

TEST_F(InitSingletonTest, InitThreadLocalSingletonTwice) {
    ObjHeader previousValue;
    ObjHeader* location = &previousValue;

    EXPECT_CALL(constructor(), Call(_)).Times(0);
    ObjHeader* value = InitThreadLocalSingleton(&location);
    EXPECT_THAT(value, location);
    EXPECT_THAT(value, &previousValue);
}

TEST_F(InitSingletonTest, InitThreadLocalSingletonFail) {
    ObjHeader* location = nullptr;
    constexpr int kException = 42;

    EXPECT_CALL(constructor(), Call(_)).WillOnce([]() { throw kException; });
    try {
        InitThreadLocalSingleton(&location);
        ASSERT_TRUE(false); // Cannot be reached.
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    }
    EXPECT_THAT(location, nullptr);
}

TEST_F(InitSingletonTest, InitSingleton) {
    ObjHeader* location = nullptr;
    ObjHeader* stackLocation = nullptr;

    ObjHeader* valueAtConstructor = nullptr;
    EXPECT_CALL(constructor(), Call(_)).WillOnce([&location, &stackLocation, &valueAtConstructor](ObjHeader* value) {
        EXPECT_THAT(value, stackLocation);
        EXPECT_THAT(location, reinterpret_cast<ObjHeader*>(1));
        valueAtConstructor = value;
    });
    ObjHeader* value = InitSingleton(&location, &stackLocation);
    EXPECT_THAT(value, stackLocation);
    EXPECT_THAT(value, location);
    EXPECT_THAT(valueAtConstructor, location);
}

TEST_F(InitSingletonTest, InitSingletonTwice) {
    ObjHeader previousValue;
    ObjHeader* location = &previousValue;
    ObjHeader* stackLocation = nullptr;

    EXPECT_CALL(constructor(), Call(_)).Times(0);
    ObjHeader* value = InitSingleton(&location, &stackLocation);
    EXPECT_THAT(value, stackLocation);
    EXPECT_THAT(value, location);
    EXPECT_THAT(value, &previousValue);
}

TEST_F(InitSingletonTest, InitSingletonFail) {
    ObjHeader* location = nullptr;
    ObjHeader* stackLocation = nullptr;
    constexpr int kException = 42;

    EXPECT_CALL(constructor(), Call(_)).WillOnce([]() { throw kException; });
    try {
        InitSingleton(&location, &stackLocation);
        ASSERT_TRUE(false); // Cannot be reached.
    } catch (int exception) {
        EXPECT_THAT(exception, kException);
    }
    EXPECT_THAT(stackLocation, nullptr);
    EXPECT_THAT(location, nullptr);
}

TEST_F(InitSingletonTest, InitSingletonRecursive) {
    ObjHeader* location1 = nullptr;
    ObjHeader* stackLocation1 = nullptr;
    ObjHeader* location2 = nullptr;
    ObjHeader* stackLocation2 = nullptr;

    EXPECT_CALL(constructor(), Call(_))
            .Times(2)
            .WillRepeatedly([this, &location1, &stackLocation1, &location2, &stackLocation2](ObjHeader* value) {
                if (value == stackLocation1) {
                    ObjHeader* result = InitSingleton(&location2, &stackLocation2);
                    EXPECT_THAT(result, stackLocation2);
                    EXPECT_THAT(result, location2);
                    EXPECT_THAT(result, testing::Ne(reinterpret_cast<ObjHeader*>(1)));
                } else {
                    ObjHeader* result = InitSingleton(&location1, &stackLocation1);
                    EXPECT_THAT(result, stackLocation1);
                    EXPECT_THAT(result, testing::Ne(location1));
                    EXPECT_THAT(location1, reinterpret_cast<ObjHeader*>(1));
                }
            });
    ObjHeader* value = InitSingleton(&location1, &stackLocation1);
    EXPECT_THAT(value, stackLocation1);
    EXPECT_THAT(value, location1);
}

// TODO: multithreading tests.
