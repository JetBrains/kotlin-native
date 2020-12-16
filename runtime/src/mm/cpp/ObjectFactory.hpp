/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_OBJECT_FACTORY_H
#define RUNTIME_MM_OBJECT_FACTORY_H

#include <algorithm>
#include <memory>
#include <mutex>

#include "Alignment.hpp"
#include "Alloc.h"
#include "Memory.h"
#include "Mutex.hpp"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

namespace internal {

// A queue that is constructed by collecting subqueues from several `Producer`s.
// This is essentially a heterogeneous `MultiSourceQueue` on top of a singly linked list that
// uses `konanAllocMemory` and `konanFreeMemory`
// TODO: Consider merging with `MultiSourceQueue` somehow.
class ObjectFactoryStorage : private Pinned {
public:
    // This class does not know its size at compile-time.
    class Node : private Pinned {
    public:
        ~Node() = default;

        static void operator delete(void* ptr) noexcept { konanFreeMemory(ptr); }

        // Note: This can only be trivially destructible data, as nobody can invoke its destructor.
        void* Data() noexcept { return data_; }

    private:
        friend class ObjectFactoryStorage;

        explicit Node(void* data) noexcept : data_(data) {}

        static std::unique_ptr<Node> Create(size_t dataSize, size_t dataAlignment) noexcept {
            RuntimeAssert(IsValidAlignment(dataAlignment), "dataAlignment=%zu is not a valid alignment", dataAlignment);
            RuntimeAssert(IsAligned(dataSize, dataAlignment), "dataSize=%zu must be aligned to dataAlignment=%zu", dataSize, dataAlignment);
            size_t alignment = std::max(alignof(Node), dataAlignment);
            size_t size = sizeof(Node) + dataSize;
            size_t allocSize = AlignUp(size, alignment);
            void* ptr = konanAllocMemory(allocSize);
            if (!ptr) {
                // TODO: Try doing GC first.
                konan::consoleErrorf("Out of memory trying to allocate %zu. Aborting.\n", allocSize);
                konan::abort();
            }
            void* alignedPtr = AlignUp(ptr, alignment);
            RuntimeAssert(
                    reinterpret_cast<uintptr_t>(alignedPtr) + size <= reinterpret_cast<uintptr_t>(ptr) + allocSize,
                    "Aligning %p (with size %zu) to %p overflowed allocated size %zu", ptr, size, alignedPtr, allocSize);
            size_t dataOffset = AlignUp(sizeof(Node), dataAlignment);
            void* data = static_cast<uint8_t*>(ptr) + dataOffset;
            RuntimeAssert(IsAligned(data, dataAlignment), "data=%p is not aligned to %zu", data, dataAlignment);
            auto* nodePtr = new (ptr) Node(data);
            return std::unique_ptr<Node>(nodePtr);
        }

        std::unique_ptr<Node> next_;
        void* data_; // TODO: In our most common case (the alignment is the same across `Node`s) this is an overkill.
        // There's some more data of an unknown (at compile-time) size here, but it cannot be represented
        // with C++ members.
    };

    class Producer : private MoveOnly {
    public:
        explicit Producer(ObjectFactoryStorage& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        Node& Insert(size_t dataSize, size_t dataAlignment) noexcept {
            AssertCorrect();
            auto node = Node::Create(dataSize, dataAlignment);
            auto* nodePtr = node.get();
            if (!root_) {
                RuntimeAssert(last_ == nullptr, "Unsynchronized root_ and last_");
                root_ = std::move(node);
            } else {
                RuntimeAssert(last_ != nullptr, "Unsynchronized root_ and last_");
                last_->next_ = std::move(node);
            }

            last_ = nodePtr;
            RuntimeAssert(root_ != nullptr, "Must not be empty");
            AssertCorrect();
            return *nodePtr;
        }

        // Merge `this` queue with owning `ObjectFactoryStorage`.
        // `this` will have empty queue after the call.
        // This call is performed without heap allocations. TODO: Test that no allocations are happening.
        void Publish() noexcept {
            AssertCorrect();
            if (!root_) {
                return;
            }

            std::lock_guard<SpinLock> guard(owner_.mutex_);

            owner_.AssertCorrectUnsafe();

            if (!owner_.root_) {
                owner_.root_ = std::move(root_);
            } else {
                owner_.last_->next_ = std::move(root_);
            }

            owner_.last_ = last_;
            last_ = nullptr;

            RuntimeAssert(root_ == nullptr, "Must be empty");
            AssertCorrect();
            RuntimeAssert(owner_.root_ != nullptr, "Must not be empty");
            owner_.AssertCorrectUnsafe();
        }

    private:
        friend class ObjectFactoryStorage;

        ALWAYS_INLINE void AssertCorrect() const noexcept {
            if (root_ == nullptr) {
                RuntimeAssert(last_ == nullptr, "last_ must be null");
            } else {
                RuntimeAssert(last_ != nullptr, "last_ must not be null");
                RuntimeAssert(last_->next_ == nullptr, "last_ must not have next");
            }
        }

        ObjectFactoryStorage& owner_; // weak
        std::unique_ptr<Node> root_;
        Node* last_ = nullptr;
    };

    class Iterator {
    public:
        Node& operator*() noexcept { return *node_; }

        Iterator& operator++() noexcept {
            previousNode_ = node_;
            node_ = node_->next_.get();
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

    private:
        friend class ObjectFactoryStorage;

        Iterator(Node* previousNode, Node* node) noexcept : previousNode_(previousNode), node_(node) {}

        Node* previousNode_; // Kept for `Iterable::EraseAndAdvance`.
        Node* node_;
    };

    class Iterable : private MoveOnly {
    public:
        explicit Iterable(ObjectFactoryStorage& owner) noexcept : owner_(owner), guard_(owner_.mutex_) {}

        Iterator begin() noexcept { return Iterator(nullptr, owner_.root_.get()); }
        Iterator end() noexcept { return Iterator(owner_.last_, nullptr); }

        void EraseAndAdvance(Iterator& iterator) noexcept { iterator.node_ = owner_.EraseUnsafe(iterator.previousNode_); }

    private:
        ObjectFactoryStorage& owner_; // weak
        std::unique_lock<SpinLock> guard_;
    };

    // Lock `ObjectFactoryStorage` for safe iteration.
    Iterable Iter() noexcept { return Iterable(*this); }

private:
    // Expects `mutex_` to be held by the current thread.
    Node* EraseUnsafe(Node* previousNode) noexcept {
        RuntimeAssert(root_ != nullptr, "Must not be empty");
        AssertCorrectUnsafe();

        if (previousNode == nullptr) {
            // Deleting the root.
            auto newRoot = std::move(root_->next_);
            root_ = std::move(newRoot);
            if (!root_) {
                last_ = nullptr;
            }
            AssertCorrectUnsafe();
            return root_.get();
        }

        auto node = std::move(previousNode->next_);
        previousNode->next_ = std::move(node->next_);
        if (!previousNode->next_) {
            last_ = previousNode;
        }

        AssertCorrectUnsafe();
        return previousNode->next_.get();
    }

    // Expects `mutex_` to be held by the current thread.
    ALWAYS_INLINE void AssertCorrectUnsafe() const noexcept {
        if (root_ == nullptr) {
            RuntimeAssert(last_ == nullptr, "last_ must be null");
        } else {
            RuntimeAssert(last_ != nullptr, "last_ must not be null");
            RuntimeAssert(last_->next_ == nullptr, "last_ must not have next");
        }
    }

    std::unique_ptr<Node> root_;
    Node* last_ = nullptr;
    SpinLock mutex_;
};

} // namespace internal

class ObjectFactory : private Pinned {
public:
    class ThreadQueue : private MoveOnly {
    public:
        explicit ThreadQueue(ObjectFactory& owner) noexcept : producer_(owner.storage_) {}

        ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept;
        ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept;

        void Publish() noexcept { producer_.Publish(); }

    private:
        internal::ObjectFactoryStorage::Producer producer_;
    };

    class Iterator {
    public:
        internal::ObjectFactoryStorage::Node& operator*() noexcept { return *iterator_; }

        Iterator& operator++() noexcept {
            ++iterator_;
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return iterator_ == rhs.iterator_; }

        bool operator!=(const Iterator& rhs) const noexcept { return iterator_ != rhs.iterator_; }

        bool IsArray() noexcept;

        ObjHeader* GetObjHeader() noexcept;
        ArrayHeader* GetArrayHeader() noexcept;

    private:
        friend class ObjectFactory;

        explicit Iterator(internal::ObjectFactoryStorage::Iterator iterator) noexcept : iterator_(std::move(iterator)) {}

        internal::ObjectFactoryStorage::Iterator iterator_;
    };

    class Iterable {
    public:
        Iterable(ObjectFactory& owner) noexcept : iter_(owner.storage_.Iter()) {}

        Iterator begin() noexcept { return Iterator(iter_.begin()); }
        Iterator end() noexcept { return Iterator(iter_.end()); }

        void EraseAndAdvance(Iterator& iterator) noexcept { iter_.EraseAndAdvance(iterator.iterator_); }

    private:
        internal::ObjectFactoryStorage::Iterable iter_;
    };

    ObjectFactory() noexcept;
    ~ObjectFactory();

    static ObjectFactory& Instance() noexcept;

    Iterable Iter() noexcept { return Iterable(*this); }

private:
    internal::ObjectFactoryStorage storage_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_OBJECT_FACTORY_H
