/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "SingleLockList.hpp"

#include <atomic>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "TestSupport.hpp"

using namespace kotlin;

namespace {

using IntList = SingleLockList<int>;

} // namespace

TEST(SingleLockListTest, EmplaceBack) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    auto* firstNode = list.EmplaceBack(kFirst);
    auto* secondNode = list.EmplaceBack(kSecond);
    auto* thirdNode = list.EmplaceBack(kThird);
    int* first = firstNode->Get();
    int* second = secondNode->Get();
    int* third = thirdNode->Get();
    EXPECT_THAT(*first, kFirst);
    EXPECT_THAT(*second, kSecond);
    EXPECT_THAT(*third, kThird);
}

TEST(SingleLockListTest, EmplaceAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    list.EmplaceBack(kFirst);
    list.EmplaceBack(kSecond);
    list.EmplaceBack(kThird);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kFirst, kSecond, kThird));
}

TEST(SingleLockListTest, EmplaceEraseAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    list.EmplaceBack(kFirst);
    auto* secondNode = list.EmplaceBack(kSecond);
    list.EmplaceBack(kThird);
    list.Erase(secondNode);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kFirst, kThird));
}

TEST(SingleLockListTest, IterEmpty) {
    IntList list;

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(SingleLockListTest, EraseToEmptyEmplaceAndIter) {
    IntList list;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    constexpr int kFourth = 4;
    auto* firstNode = list.EmplaceBack(kFirst);
    auto* secondNode = list.EmplaceBack(kSecond);
    list.Erase(firstNode);
    list.Erase(secondNode);
    list.EmplaceBack(kThird);
    list.EmplaceBack(kFourth);

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(kThird, kFourth));
}

TEST(SingleLockListTest, SpliceBack) {
    IntList list1;
    IntList list2;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    constexpr int kFourth = 4;

    list1.EmplaceBack(kFirst);
    list1.EmplaceBack(kSecond);

    list2.EmplaceBack(kThird);
    list2.EmplaceBack(kFourth);

    list1.SpliceBack(list2);

    std::vector<int> actual1;
    for (int element : list1.Iter()) {
        actual1.push_back(element);
    }

    std::vector<int> actual2;
    for (int element : list2.Iter()) {
        actual2.push_back(element);
    }

    EXPECT_THAT(actual1, testing::ElementsAre(kFirst, kSecond, kThird, kFourth));
    EXPECT_THAT(actual2, testing::IsEmpty());
}

TEST(SingleLockListTest, SpliceBackDifferentMutex) {
    IntList list1;
    SingleLockList<int, std::mutex> list2;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;
    constexpr int kThird = 3;
    constexpr int kFourth = 4;

    list1.EmplaceBack(kFirst);
    list1.EmplaceBack(kSecond);

    list2.EmplaceBack(kThird);
    list2.EmplaceBack(kFourth);

    list1.SpliceBack(list2);

    std::vector<int> actual1;
    for (int element : list1.Iter()) {
        actual1.push_back(element);
    }

    std::vector<int> actual2;
    for (int element : list2.Iter()) {
        actual2.push_back(element);
    }

    EXPECT_THAT(actual1, testing::ElementsAre(kFirst, kSecond, kThird, kFourth));
    EXPECT_THAT(actual2, testing::IsEmpty());
}

TEST(SingleLockListTest, SpliceBackEmpty) {
    IntList list1;
    IntList list2;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;

    list1.EmplaceBack(kFirst);
    list1.EmplaceBack(kSecond);

    list1.SpliceBack(list2);

    std::vector<int> actual1;
    for (int element : list1.Iter()) {
        actual1.push_back(element);
    }

    std::vector<int> actual2;
    for (int element : list2.Iter()) {
        actual2.push_back(element);
    }

    EXPECT_THAT(actual1, testing::ElementsAre(kFirst, kSecond));
    EXPECT_THAT(actual2, testing::IsEmpty());
}

TEST(SingleLockListTest, SpliceBackOnEmpty) {
    IntList list1;
    IntList list2;
    constexpr int kFirst = 1;
    constexpr int kSecond = 2;

    list2.EmplaceBack(kFirst);
    list2.EmplaceBack(kSecond);

    list1.SpliceBack(list2);

    std::vector<int> actual1;
    for (int element : list1.Iter()) {
        actual1.push_back(element);
    }

    std::vector<int> actual2;
    for (int element : list2.Iter()) {
        actual2.push_back(element);
    }

    EXPECT_THAT(actual1, testing::ElementsAre(kFirst, kSecond));
    EXPECT_THAT(actual2, testing::IsEmpty());
}

TEST(SingleLockListTest, ConcurrentEmplace) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::vector<int> expected;
    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &list, &canStart, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            list.EmplaceBack(i);
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(SingleLockListTest, ConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        items.push_back(list.EmplaceBack(i));
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    for (auto* item : items) {
        threads.emplace_back([item, &list, &canStart, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            list.Erase(item);
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(SingleLockListTest, ConcurrentSpliceBack) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::vector<int> expected;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = 2 * i;
        expected.push_back(j);
        expected.push_back(j + 1);
        threads.emplace_back([j, &list, &canStart, &readyCount]() {
            IntList subList;
            subList.EmplaceBack(j);
            subList.EmplaceBack(j + 1);
            ++readyCount;
            while (!canStart) {
            }
            list.SpliceBack(subList);
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;

    for (auto& t : threads) {
        t.join();
    }

    std::vector<int> actual;
    for (int element : list.Iter()) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(SingleLockListTest, IterWhileConcurrentEmplace) {
    IntList list;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedBefore;
    std::vector<int> expectedAfter;
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_back(i);
        expectedAfter.push_back(i);
        list.EmplaceBack(i);
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &list, &canStart, &startedCount, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            list.EmplaceBack(j);
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = list.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : list.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}

TEST(SingleLockListTest, IterWhileConcurrentErase) {
    IntList list;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedBefore;
    std::vector<IntList::Node*> items;
    for (int i = 0; i < kThreadCount; ++i) {
        expectedBefore.push_back(i);
        items.push_back(list.EmplaceBack(i));
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    for (auto* item : items) {
        threads.emplace_back([item, &list, &canStart, &startedCount, &readyCount]() {
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            list.Erase(item);
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = list.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : list.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}

TEST(SingleLockListTest, IterWhileConcurrentSpliceBack) {
    IntList list;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedBefore;
    std::vector<int> expectedAfter;
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_back(i);
        expectedAfter.push_back(i);
        list.EmplaceBack(i);
    }

    std::atomic<bool> canStart(false);
    std::atomic<int> startedCount(0);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = 2 * i + kStartCount;
        expectedAfter.push_back(j);
        expectedAfter.push_back(j + 1);
        threads.emplace_back([j, &list, &canStart, &startedCount, &readyCount]() {
            IntList subList;
            subList.EmplaceBack(j);
            subList.EmplaceBack(j + 1);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            list.SpliceBack(subList);
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = list.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (int element : iter) {
            actualBefore.push_back(element);
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    std::vector<int> actualAfter;
    for (int element : list.Iter()) {
        actualAfter.push_back(element);
    }

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}

namespace {

class PinnedType : private Pinned {
public:
    PinnedType(int value) : value_(value) {}

    int value() const { return value_; }

private:
    int value_;
};

} // namespace

TEST(SingleLockListTest, PinnedType) {
    SingleLockList<PinnedType> list;
    constexpr int kFirst = 1;

    auto* itemNode = list.EmplaceBack(kFirst);
    PinnedType* item = itemNode->Get();
    EXPECT_THAT(item->value(), kFirst);

    list.Erase(itemNode);

    std::vector<PinnedType*> actualAfter;
    for (auto& element : list.Iter()) {
        actualAfter.push_back(&element);
    }

    EXPECT_THAT(actualAfter, testing::IsEmpty());
}
