/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_MM_OBJECT_FACTORY_H
#define RUNTIME_MM_OBJECT_FACTORY_H

#include <algorithm>
#include <memory>
#include <mutex>
#include <type_traits>

#include "Alignment.hpp"
#include "Alloc.h"
#include "GC.hpp"
#include "Memory.h"
#include "Mutex.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {
namespace mm {

namespace internal {

// A queue that is constructed by collecting subqueues from several `Producer`s.
// This is essentially a heterogeneous `MultiSourceQueue` on top of a singly linked list that
// uses `konanAllocMemory` and `konanFreeMemory`
// TODO: Consider merging with `MultiSourceQueue` somehow.
template <size_t DataAlignment>
class ObjectFactoryStorage : private Pinned {
    static_assert(IsValidAlignment(DataAlignment), "DataAlignment is not a valid alignment");

public:
    // This class does not know its size at compile-time. Does not inherit from `KonanAllocatorAware` because
    // in `KonanAllocatorAware::operator new(size_t size, KonanAllocTag)` `size` would be incorrect.
    class Node : private Pinned {
        constexpr static size_t DataOffset() noexcept { return AlignUp(sizeof(Node), DataAlignment); }

    public:
        ~Node() = default;

        // Note: This can only be trivially destructible data, as nobody can invoke its destructor.
        void* Data() noexcept {
            constexpr size_t kDataOffset = DataOffset();
            void* ptr = reinterpret_cast<uint8_t*>(this) + kDataOffset;
            RuntimeAssert(IsAligned(ptr, DataAlignment), "Data=%p is not aligned to %zu", ptr, DataAlignment);
            return ptr;
        }

        // It's a caller responsibility to know if the underlying data is `T`.
        template <typename T>
        T& Data() noexcept {
            return *static_cast<T*>(Data());
        }

    private:
        friend class ObjectFactoryStorage;

        Node() noexcept = default;

        template <typename Allocator>
        static KStdUniquePtr<Node> Create(Allocator& allocator, size_t dataSize) noexcept {
            size_t dataSizeAligned = AlignUp(dataSize, DataAlignment);
            size_t totalAlignment = std::max(alignof(Node), DataAlignment);
            size_t totalSize = AlignUp(sizeof(Node) + dataSizeAligned, totalAlignment);
            RuntimeAssert(
                    DataOffset() + dataSize <= totalSize, "totalSize %zu is not enough to fit data %zu at offset %zu", totalSize, dataSize,
                    DataOffset());
            void* ptr = allocator.Alloc(totalSize, totalAlignment);
            RuntimeAssert(IsAligned(ptr, totalAlignment), "Allocator returned unaligned to %zu pointer %p", totalAlignment, ptr);
            return KStdUniquePtr<Node>(new (ptr) Node());
        }

        KStdUniquePtr<Node> next_;
        // There's some more data of an unknown (at compile-time) size here, but it cannot be represented
        // with C++ members.
    };

    template <typename Allocator>
    class Producer : private MoveOnly {
    public:
        explicit Producer(ObjectFactoryStorage& owner, Allocator&& allocator) noexcept : owner_(owner), allocator_(std::move(allocator)) {}

        explicit Producer(ObjectFactoryStorage& owner) noexcept : owner_(owner) {}

        ~Producer() { Publish(); }

        Node& Insert(size_t dataSize) noexcept {
            AssertCorrect();
            auto node = Node::Create(allocator_, dataSize);
            auto* nodePtr = node.get();
            if (!root_) {
                root_ = std::move(node);
            } else {
                last_->next_ = std::move(node);
            }

            last_ = nodePtr;
            RuntimeAssert(root_ != nullptr, "Must not be empty");
            AssertCorrect();
            return *nodePtr;
        }

        template <typename T, typename... Args>
        Node& Insert(Args&&... args) noexcept {
            static_assert(alignof(T) <= DataAlignment, "Cannot insert type with alignment bigger than DataAlignment");
            static_assert(std::is_trivially_destructible_v<T>, "Type must be trivially destructible");
            auto& node = Insert(sizeof(T));
            new (node.Data()) T(std::forward<Args>(args)...);
            return node;
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

        void ClearForTests() noexcept {
            // Since it's only for tests, no need to worry about stack overflows.
            root_.reset();
            last_ = nullptr;
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
        Allocator allocator_;
        KStdUniquePtr<Node> root_;
        Node* last_ = nullptr;
    };

    class Iterator {
    public:
        Node& operator*() noexcept { return *node_; }
        Node* operator->() noexcept { return node_; }

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

    ~ObjectFactoryStorage() {
        // Make sure not to blow up the stack by nested `~Node` calls.
        for (auto node = std::move(root_); node != nullptr; node = std::move(node->next_)) {}
    }

    // Lock `ObjectFactoryStorage` for safe iteration.
    Iterable Iter() noexcept { return Iterable(*this); }

private:
    // Expects `mutex_` to be held by the current thread.
    Node* EraseUnsafe(Node* previousNode) noexcept {
        RuntimeAssert(root_ != nullptr, "Must not be empty");
        AssertCorrectUnsafe();

        if (previousNode == nullptr) {
            // Deleting the root.
            root_ = std::move(root_->next_);
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

    KStdUniquePtr<Node> root_;
    Node* last_ = nullptr;
    SpinLock mutex_;
};

template <typename BaseAllocator, typename GC>
class AllocatorWithGC {
public:
    AllocatorWithGC(BaseAllocator base, GC& gc) noexcept : base_(std::move(base)), gc_(gc) {}

    void* Alloc(size_t size, size_t alignment) noexcept {
        gc_.SafePointAllocation(size);
        void* ptr = base_.Alloc(size, alignment);
        if (ptr) {
            return ptr;
        }
        gc_.OnOOM(size);
        ptr = base_.Alloc(size, alignment);
        if (ptr) {
            return ptr;
        }
        konan::consoleErrorf("Out of memory trying to allocate %zu bytes. Aborting.\n", size);
        konan::abort();
    }

private:
    BaseAllocator base_;
    GC& gc_;
};

} // namespace internal

class ObjectFactory : private Pinned {
public:
    class Allocator {
    public:
        void* Alloc(size_t size, size_t alignment) noexcept { return konanAllocAlignedMemory(size, alignment); }
    };

    using Storage = internal::ObjectFactoryStorage<kObjectAlignment>;

    template <typename GC>
    class ThreadQueue : private MoveOnly {
    public:
        ThreadQueue(ObjectFactory& owner, GC& gc) noexcept : producer_(owner.storage_, internal::AllocatorWithGC(Allocator(), gc)) {}

        ObjHeader* CreateObject(const TypeInfo* typeInfo) noexcept {
            RuntimeAssert(!typeInfo->IsArray(), "Must not be an array");
            size_t allocSize = typeInfo->instanceSize_;
            auto& node = producer_.Insert(allocSize);
            auto* object = static_cast<ObjHeader*>(node.Data());
            object->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
            return object;
        }

        ArrayHeader* CreateArray(const TypeInfo* typeInfo, uint32_t count) noexcept {
            RuntimeAssert(typeInfo->IsArray(), "Must be an array");
            uint32_t arraySize = static_cast<uint32_t>(-typeInfo->instanceSize_) * count;
            // Note: array body is aligned, but for size computation it is enough to align the sum.
            size_t allocSize = AlignUp(sizeof(ArrayHeader) + arraySize, kObjectAlignment);
            auto& node = producer_.Insert(allocSize);
            auto* array = static_cast<ArrayHeader*>(node.Data());
            array->typeInfoOrMeta_ = const_cast<TypeInfo*>(typeInfo);
            array->count_ = count;
            return array;
        }

        void Publish() noexcept { producer_.Publish(); }

        void ClearForTests() noexcept { producer_.ClearForTests(); }

    private:
        Storage::Producer<internal::AllocatorWithGC<Allocator, GC>> producer_;
    };

    class Iterator {
    public:
        Storage::Node& operator*() noexcept { return *iterator_; }

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

        explicit Iterator(Storage::Iterator iterator) noexcept : iterator_(std::move(iterator)) {}

        Storage::Iterator iterator_;
    };

    class Iterable {
    public:
        Iterable(ObjectFactory& owner) noexcept : iter_(owner.storage_.Iter()) {}

        Iterator begin() noexcept { return Iterator(iter_.begin()); }
        Iterator end() noexcept { return Iterator(iter_.end()); }

        void EraseAndAdvance(Iterator& iterator) noexcept { iter_.EraseAndAdvance(iterator.iterator_); }

    private:
        Storage::Iterable iter_;
    };

    ObjectFactory() noexcept;
    ~ObjectFactory();

    static ObjectFactory& Instance() noexcept;

    Iterable Iter() noexcept { return Iterable(*this); }

private:
    Storage storage_;
};

} // namespace mm
} // namespace kotlin

#endif // RUNTIME_MM_OBJECT_FACTORY_H
