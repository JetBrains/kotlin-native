/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "MultiSourceQueue.hpp"

#include "gmock/gmock.h"
#include "gtest/gtest.h"

using namespace kotlin;

using IntQueue = MultiSourceQueue<int>;

TEST(MultiSourceQueueTest, Empty) {
    IntQueue queue;

    std::vector<int> actual;
    for (int element : queue) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(MultiSourceQueueTest, DoNotCollect) {
    IntQueue queue;
    IntQueue::Producer producer;

    producer.add(1);
    producer.add(2);

    std::vector<int> actual;
    for (int element : queue) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(MultiSourceQueueTest, Collect) {
    IntQueue queue;
    IntQueue::Producer producer1;
    IntQueue::Producer producer2;

    producer1.add(1);
    producer1.add(2);
    producer2.add(10);
    producer2.add(20);

    queue.Collect(&producer1);
    queue.Collect(&producer2);

    std::vector<int> actual;
    for (int element : queue) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 10, 20));
}

TEST(MultiSourceQueueTest, CollectSeveralTimes) {
    IntQueue queue;
    IntQueue::Producer producer;

    // Add 2 elements and collect.
    producer.add(1);
    producer.add(2);
    queue.Collect(&producer);

    // Add another element and collect.
    producer.add(3);
    queue.Collect(&producer);

    // Collect without adding elements.
    queue.Collect(&producer);

    // Add yet another two elements and collect.
    producer.add(4);
    producer.add(5);
    queue.Collect(&producer);

    std::vector<int> actual;
    for (int element : queue) {
        actual.push_back(element);
    }

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 3, 4, 5));
}
