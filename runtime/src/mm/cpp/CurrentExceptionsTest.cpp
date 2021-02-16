/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CurrentExceptions.hpp"

#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Memory.h"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "Types.h"

using namespace kotlin;

namespace {

KStdVector<ObjHeader*> Collect(mm::CurrentExceptions& currentExceptions) {
    KStdVector<ObjHeader*> result;
    for (const auto& obj : currentExceptions) {
        result.push_back(obj);
    }
    return result;
}

} // namespace

TEST(CurrentExceptionTest, Basic) {
    mm::CurrentExceptions currentExceptions;

    EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());

    ObjHeader exception;
    currentExceptions.AddException(&exception);

    EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception));

    currentExceptions.RemoveException(&exception);

    EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());
}

TEST(CurrentExceptionTest, AddSeveralExceptions) {
    mm::CurrentExceptions currentExceptions;

    EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());

    ObjHeader exception1;
    ObjHeader exception2;
    currentExceptions.AddException(&exception1);
    currentExceptions.AddException(&exception2);
    currentExceptions.AddException(&exception1);
    currentExceptions.AddException(&exception2);
    currentExceptions.AddException(&exception2);

    EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1, &exception2, &exception1, &exception2, &exception2));

    currentExceptions.RemoveException(&exception1);

    EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception2, &exception1, &exception2, &exception2));

    currentExceptions.RemoveException(&exception2);

    EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1, &exception2, &exception2));

    currentExceptions.RemoveException(&exception2);

    EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1, &exception2));

    currentExceptions.RemoveException(&exception1);
    currentExceptions.RemoveException(&exception2);

    EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());
}

TEST(CurrentExceptionTest, NothingByDefault) {
    mm::RunInNewThread([](mm::ThreadData& threadData) { EXPECT_THAT(Collect(threadData.currentExceptions()), testing::IsEmpty()); });
}

TEST(CurrentExceptionTest, Throw) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentExceptions = threadData.currentExceptions();
        ASSERT_THAT(Collect(currentExceptions), testing::IsEmpty());

        ObjHeader exception;
        try {
            throw ExceptionObjHolder(&exception);
        } catch (...) {
            EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception));
        }
        EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());
    });
}

TEST(CurrentExceptionTest, ThrowInsideCatch) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentExceptions = threadData.currentExceptions();
        ASSERT_THAT(Collect(currentExceptions), testing::IsEmpty());

        ObjHeader exception1;
        try {
            throw ExceptionObjHolder(&exception1);
        } catch (...) {
            ObjHeader exception2;
            try {
                throw ExceptionObjHolder(&exception2);
            } catch (...) {
                EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1, &exception2));
            }
            EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1));
        }
        EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());
    });
}

TEST(CurrentExceptionTest, StoreException) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentExceptions = threadData.currentExceptions();
        ASSERT_THAT(Collect(currentExceptions), testing::IsEmpty());

        ObjHeader exception1;
        std::exception_ptr storedException1;
        try {
            throw ExceptionObjHolder(&exception1);
        } catch (...) {
            storedException1 = std::current_exception();
        }
        EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1));

        ObjHeader exception2;
        std::exception_ptr storedException2;
        try {
            throw ExceptionObjHolder(&exception2);
        } catch (...) {
            storedException2 = std::current_exception();
        }
        EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception1, &exception2));

        storedException1 = std::exception_ptr();
        EXPECT_THAT(Collect(currentExceptions), testing::ElementsAre(&exception2));

        storedException2 = std::exception_ptr();
        EXPECT_THAT(Collect(currentExceptions), testing::IsEmpty());
    });
}
