/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "ObjectFactory.hpp"

#include <atomic>
#include <thread>

#include "gmock/gmock.h"
#include "gtest/gtest.h"

#include "CppSupport.hpp"
#include "TestSupport.hpp"

using namespace kotlin;

using mm::internal::ObjectFactoryStorage;

namespace {

std::vector<void*> Collect(ObjectFactoryStorage& storage) {
    std::vector<void*> result;
    for (auto& node : storage.Iter()) {
        result.push_back(node.Data());
    }
    return result;
}

template <typename T>
std::vector<T> Collect(ObjectFactoryStorage& storage) {
    std::vector<T> result;
    for (auto& node : storage.Iter()) {
        result.push_back(*static_cast<T*>(node.Data()));
    }
    return result;
}

template <typename T, typename... Args>
ObjectFactoryStorage::Node& Insert(ObjectFactoryStorage::Producer& producer, Args&&... args) {
    auto& node = producer.Insert(sizeof(T), alignof(T));
    new (node.Data()) T(std::forward<Args>(args)...);
    return node;
}

template <typename T>
T& GetData(ObjectFactoryStorage::Iterator& iterator) {
    return *static_cast<T*>((*iterator).Data());
}

struct MoveOnlyImpl : private MoveOnly {
    MoveOnlyImpl(int value1, int value2) : value1(value1), value2(value2) {}

    int value1;
    int value2;
};

struct PinnedImpl : private Pinned {
    PinnedImpl(int value1, int value2, int value3) : value1(value1), value2(value2), value3(value3) {}

    int value1;
    int value2;
    int value3;
};

} // namespace

TEST(ObjectFactoryStorageTest, Empty) {
    ObjectFactoryStorage storage;

    auto actual = Collect(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ObjectFactoryStorageTest, DoNotPublish) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<int>(producer, 2);

    auto actual = Collect(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ObjectFactoryStorageTest, Publish) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer1(storage);
    ObjectFactoryStorage::Producer producer2(storage);

    Insert<int>(producer1, 1);
    Insert<int>(producer1, 2);
    Insert<int>(producer2, 10);
    Insert<int>(producer2, 20);

    producer1.Publish();
    producer2.Publish();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 10, 20));
}

TEST(ObjectFactoryStorageTest, PublishDifferentTypes) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<size_t>(producer, 2);
    Insert<MoveOnlyImpl>(producer, 3, 4);
    Insert<PinnedImpl>(producer, 5, 6, 7);

    producer.Publish();

    auto actual = storage.Iter();
    auto it = actual.begin();
    EXPECT_THAT(GetData<int>(it), 1);
    ++it;
    EXPECT_THAT(GetData<size_t>(it), 2);
    ++it;
    auto& moveOnly = GetData<MoveOnlyImpl>(it);
    EXPECT_THAT(moveOnly.value1, 3);
    EXPECT_THAT(moveOnly.value2, 4);
    ++it;
    auto& pinned = GetData<PinnedImpl>(it);
    EXPECT_THAT(pinned.value1, 5);
    EXPECT_THAT(pinned.value2, 6);
    EXPECT_THAT(pinned.value3, 7);
    ++it;
    EXPECT_THAT(it, actual.end());
}

TEST(ObjectFactoryStorageTest, PublishSeveralTimes) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    // Add 2 elements and publish.
    Insert<int>(producer, 1);
    Insert<int>(producer, 2);
    producer.Publish();

    // Add another element and publish.
    Insert<int>(producer, 3);
    producer.Publish();

    // Publish without adding elements.
    producer.Publish();

    // Add yet another two elements and publish.
    Insert<int>(producer, 4);
    Insert<int>(producer, 5);
    producer.Publish();

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2, 3, 4, 5));
}

TEST(ObjectFactoryStorageTest, PublishInDestructor) {
    ObjectFactoryStorage storage;

    {
        ObjectFactoryStorage::Producer producer(storage);
        Insert<int>(producer, 1);
        Insert<int>(producer, 2);
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
}

TEST(ObjectFactoryStorageTest, EraseFirst) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<int>(producer, 2);
    Insert<int>(producer, 3);

    producer.Publish();

    {
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (GetData<int>(it) == 1) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(2, 3));
}

TEST(ObjectFactoryStorageTest, EraseMiddle) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<int>(producer, 2);
    Insert<int>(producer, 3);

    producer.Publish();

    {
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (GetData<int>(it) == 2) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 3));
}

TEST(ObjectFactoryStorageTest, EraseLast) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<int>(producer, 2);
    Insert<int>(producer, 3);

    producer.Publish();

    {
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (GetData<int>(it) == 3) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::ElementsAre(1, 2));
}

TEST(ObjectFactoryStorageTest, EraseAll) {
    ObjectFactoryStorage storage;
    ObjectFactoryStorage::Producer producer(storage);

    Insert<int>(producer, 1);
    Insert<int>(producer, 2);
    Insert<int>(producer, 3);

    producer.Publish();

    {
        auto iter = storage.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            iter.EraseAndAdvance(it);
        }
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::IsEmpty());
}

TEST(ObjectFactoryStorageTest, ConcurrentPublish) {
    ObjectFactoryStorage storage;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::vector<int> expected;

    for (int i = 0; i < kThreadCount; ++i) {
        expected.push_back(i);
        threads.emplace_back([i, &storage, &canStart, &readyCount]() {
            ObjectFactoryStorage::Producer producer(storage);
            Insert<int>(producer, i);
            ++readyCount;
            while (!canStart) {
            }
            producer.Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}

TEST(ObjectFactoryStorageTest, IterWhileConcurrentPublish) {
    ObjectFactoryStorage storage;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedBefore;
    std::vector<int> expectedAfter;
    ObjectFactoryStorage::Producer producer(storage);
    for (int i = 0; i < kStartCount; ++i) {
        expectedBefore.push_back(i);
        expectedAfter.push_back(i);
        Insert<int>(producer, i);
    }
    producer.Publish();

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::atomic<int> startedCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &storage, &canStart, &startedCount, &readyCount]() {
            ObjectFactoryStorage::Producer producer(storage);
            Insert<int>(producer, j);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            producer.Publish();
        });
    }

    std::vector<int> actualBefore;
    {
        auto iter = storage.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (auto& node : iter) {
            int element = *static_cast<int*>(node.Data());
            actualBefore.push_back(element);
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    EXPECT_THAT(actualBefore, testing::ElementsAreArray(expectedBefore));

    auto actualAfter = Collect<int>(storage);

    EXPECT_THAT(actualAfter, testing::UnorderedElementsAreArray(expectedAfter));
}

TEST(ObjectFactoryStorageTest, EraseWhileConcurrentPublish) {
    ObjectFactoryStorage storage;
    constexpr int kStartCount = 50;
    constexpr int kThreadCount = kDefaultThreadCount;

    std::vector<int> expectedAfter;
    ObjectFactoryStorage::Producer producer(storage);
    for (int i = 0; i < kStartCount; ++i) {
        if (i % 2 == 0) {
            expectedAfter.push_back(i);
        }
        Insert<int>(producer, i);
    }
    producer.Publish();

    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::atomic<int> startedCount(0);
    std::vector<std::thread> threads;
    for (int i = 0; i < kThreadCount; ++i) {
        int j = i + kStartCount;
        expectedAfter.push_back(j);
        threads.emplace_back([j, &storage, &canStart, &startedCount, &readyCount]() {
            ObjectFactoryStorage::Producer producer(storage);
            Insert<int>(producer, j);
            ++readyCount;
            while (!canStart) {
            }
            ++startedCount;
            producer.Publish();
        });
    }

    {
        auto iter = storage.Iter();
        while (readyCount < kThreadCount) {
        }
        canStart = true;
        while (startedCount < kThreadCount) {
        }

        for (auto it = iter.begin(); it != iter.end();) {
            if (GetData<int>(it) % 2 != 0) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    for (auto& t : threads) {
        t.join();
    }

    auto actual = Collect<int>(storage);

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expectedAfter));
}

using mm::ObjectFactory;

namespace {

std::unique_ptr<TypeInfo> MakeObjectTypeInfo(int32_t size) {
    auto typeInfo = std_support::make_unique<TypeInfo>();
    typeInfo->typeInfo_ = typeInfo.get();
    typeInfo->instanceSize_ = size;
    return typeInfo;
}

std::unique_ptr<TypeInfo> MakeArrayTypeInfo(int32_t elementSize) {
    auto typeInfo = std_support::make_unique<TypeInfo>();
    typeInfo->typeInfo_ = typeInfo.get();
    typeInfo->instanceSize_ = -elementSize;
    return typeInfo;
}

} // namespace

TEST(ObjectFactoryTest, CreateObject) {
    auto typeInfo = MakeObjectTypeInfo(24);
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory);

    auto* object = threadQueue.CreateObject(typeInfo.get());
    threadQueue.Publish();

    auto iter = objectFactory.Iter();
    auto it = iter.begin();
    EXPECT_FALSE(it.IsArray());
    EXPECT_THAT(it.GetObjHeader(), object);
    ++it;
    EXPECT_THAT(it, iter.end());
}

TEST(ObjectFactoryTest, CreateArray) {
    auto typeInfo = MakeArrayTypeInfo(24);
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory);

    auto* array = threadQueue.CreateArray(typeInfo.get(), 3);
    threadQueue.Publish();

    auto iter = objectFactory.Iter();
    auto it = iter.begin();
    EXPECT_TRUE(it.IsArray());
    EXPECT_THAT(it.GetArrayHeader(), array);
    ++it;
    EXPECT_THAT(it, iter.end());
}

TEST(ObjectFactoryTest, Erase) {
    auto objectTypeInfo = MakeObjectTypeInfo(24);
    auto arrayTypeInfo = MakeArrayTypeInfo(24);
    ObjectFactory objectFactory;
    ObjectFactory::ThreadQueue threadQueue(objectFactory);

    for (int i = 0; i < 10; ++i) {
        threadQueue.CreateObject(objectTypeInfo.get());
        threadQueue.CreateArray(arrayTypeInfo.get(), 3);
    }

    threadQueue.Publish();

    {
        auto iter = objectFactory.Iter();
        for (auto it = iter.begin(); it != iter.end();) {
            if (it.IsArray()) {
                iter.EraseAndAdvance(it);
            } else {
                ++it;
            }
        }
    }

    {
        auto iter = objectFactory.Iter();
        int count = 0;
        for (auto it = iter.begin(); it != iter.end(); ++it, ++count) {
            EXPECT_FALSE(it.IsArray());
        }
        EXPECT_THAT(count, 10);
    }
}

TEST(ObjectFactoryTest, ConcurrentPublish) {
    auto typeInfo = MakeObjectTypeInfo(24);
    ObjectFactory objectFactory;
    constexpr int kThreadCount = kDefaultThreadCount;
    std::atomic<bool> canStart(false);
    std::atomic<int> readyCount(0);
    std::vector<std::thread> threads;
    std::mutex expectedMutex;
    std::vector<ObjHeader*> expected;

    for (int i = 0; i < kThreadCount; ++i) {
        threads.emplace_back([&typeInfo, &objectFactory, &canStart, &readyCount, &expected, &expectedMutex]() {
            ObjectFactory::ThreadQueue threadQueue(objectFactory);
            auto* object = threadQueue.CreateObject(typeInfo.get());
            {
                std::lock_guard<std::mutex> guard(expectedMutex);
                expected.push_back(object);
            }
            ++readyCount;
            while (!canStart) {
            }
            threadQueue.Publish();
        });
    }

    while (readyCount < kThreadCount) {
    }
    canStart = true;
    for (auto& t : threads) {
        t.join();
    }

    auto iter = objectFactory.Iter();
    std::vector<ObjHeader*> actual;
    for (auto it = iter.begin(); it != iter.end(); ++it) {
        actual.push_back(it.GetObjHeader());
    }

    EXPECT_THAT(actual, testing::UnorderedElementsAreArray(expected));
}
