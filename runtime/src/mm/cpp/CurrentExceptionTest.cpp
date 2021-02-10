/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "CurrentException.hpp"

#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "Memory.h"
#include "TestSupport.hpp"
#include "ThreadData.hpp"
#include "Types.h"

using namespace kotlin;

namespace {

KStdVector<ObjHeader*> Collect(mm::CurrentException& currentException) {
    KStdVector<ObjHeader*> result;
    for (const auto& obj : currentException) {
        result.push_back(obj);
    }
    return result;
}

} // namespace

TEST(CurrentExceptionTest, Basic) {
    mm::CurrentException currentException;

    EXPECT_THAT(Collect(currentException), testing::IsEmpty());

    ObjHeader exception;
    currentException.AddException(&exception);

    EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception));

    currentException.RemoveException(&exception);

    EXPECT_THAT(Collect(currentException), testing::IsEmpty());
}

TEST(CurrentExceptionTest, NothingByDefault) {
    mm::RunInNewThread([](mm::ThreadData& threadData) { EXPECT_THAT(Collect(threadData.currentException()), testing::IsEmpty()); });
}

TEST(CurrentExceptionTest, Throw) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentException = threadData.currentException();
        ASSERT_THAT(Collect(currentException), testing::IsEmpty());

        ObjHeader exception;
        try {
            throw ExceptionObjHolder(&exception);
        } catch (...) {
            EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception));
        }
        EXPECT_THAT(Collect(currentException), testing::IsEmpty());
    });
}

TEST(CurrentExceptionTest, ThrowInsideCatch) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentException = threadData.currentException();
        ASSERT_THAT(Collect(currentException), testing::IsEmpty());

        ObjHeader exception1;
        try {
            throw ExceptionObjHolder(&exception1);
        } catch (...) {
            ObjHeader exception2;
            try {
                throw ExceptionObjHolder(&exception2);
            } catch (...) {
                EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception1, &exception2));
            }
            EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception1));
        }
        EXPECT_THAT(Collect(currentException), testing::IsEmpty());
    });
}

TEST(CurrentExceptionTest, StoreException) {
    mm::RunInNewThread([](mm::ThreadData& threadData) {
        auto& currentException = threadData.currentException();
        ASSERT_THAT(Collect(currentException), testing::IsEmpty());

        ObjHeader exception1;
        std::exception_ptr storedException1;
        try {
            throw ExceptionObjHolder(&exception1);
        } catch (...) {
            storedException1 = std::current_exception();
        }
        EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception1));

        ObjHeader exception2;
        std::exception_ptr storedException2;
        try {
            throw ExceptionObjHolder(&exception2);
        } catch (...) {
            storedException2 = std::current_exception();
        }
        EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception1, &exception2));

        storedException1 = std::exception_ptr();
        EXPECT_THAT(Collect(currentException), testing::ElementsAre(&exception2));

        storedException2 = std::exception_ptr();
        EXPECT_THAT(Collect(currentException), testing::IsEmpty());
    });
}
