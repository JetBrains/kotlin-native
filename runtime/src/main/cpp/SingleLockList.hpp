/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SINGLE_LOCK_LIST_H
#define RUNTIME_SINGLE_LOCK_LIST_H

#include <cstddef>
#include <memory>
#include <mutex>

#include "CppSupport.hpp"
#include "Mutex.hpp"
#include "Utils.hpp"

namespace kotlin {

template <typename, typename>
class SingleLockList;

namespace internal {

template <typename Value>
class SingleLockListBase : private Pinned {
protected:
    // Do not allow to construct and destruct `this` directly. This class does not
    // have any fields, so destructor is not virtual.
    SingleLockListBase() noexcept = default;
    ~SingleLockListBase() = default;

    class Iterator;

    class Node : Pinned {
    public:
        Value* Get() noexcept { return &value; }

    private:
        friend class Iterator;

        template <typename, typename>
        friend class ::kotlin::SingleLockList;

        template <typename... Args>
        Node(Args... args) noexcept : value(args...) {}

        Value value;
        std::unique_ptr<Node> next;
        Node* previous = nullptr; // weak
    };

    class Iterator {
    public:
        explicit Iterator(Node* node) noexcept : node_(node) {}

        Value& operator*() noexcept { return node_->value; }

        Iterator& operator++() noexcept {
            node_ = node_->next.get();
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

    private:
        Node* node_;
    };

    // NOTE: Do not add any fields here, this is an interface. Also, the destructor is non-virtual.
};

} // namespace internal

// TODO: Consider different locking mechanisms.
template <typename Value, typename Mutex = SimpleMutex>
class SingleLockList : private internal::SingleLockListBase<Value> {
public:
    using Node = typename internal::SingleLockListBase<Value>::Node;
    using Iterator = typename internal::SingleLockListBase<Value>::Iterator;

    class Iterable : private MoveOnly {
    public:
        explicit Iterable(SingleLockList* list) noexcept : list_(list), guard_(list->mutex_) {}

        Iterator begin() noexcept { return Iterator(list_->root_.get()); }

        Iterator end() noexcept { return Iterator(nullptr); }

    private:
        SingleLockList* list_;
        std::unique_lock<Mutex> guard_;
    };

    SingleLockList() noexcept = default;
    ~SingleLockList() = default;

    template <typename... Args>
    Node* EmplaceBack(Args... args) noexcept {
        auto* nodePtr = new Node(args...);
        std::unique_ptr<Node> node(nodePtr);
        LockGuard<Mutex> guard(mutex_);
        if (Empty()) {
            root_ = std::move(node);
        } else {
            last_->next = std::move(node);
            nodePtr->previous = last_;
        }
        last_ = nodePtr;
        return nodePtr;
    }

    // Using `node` including its referred `Value` after `Erase` is undefined behaviour.
    void Erase(Node* node) noexcept {
        LockGuard<Mutex> guard(mutex_);
        if (last_ == node) {
            last_ = node->previous;
        }
        if (root_.get() == node) {
            root_ = std::move(node->next);
            if (root_) {
                root_->previous = nullptr;
            }
            return;
        }
        auto* previous = node->previous;
        RuntimeAssert(previous != nullptr, "Only the root node doesn't have the previous node");
        auto ownedNode = std::move(previous->next);
        previous->next = std::move(node->next);
        if (auto& next = previous->next) {
            next->previous = previous;
        }
    }

    // Move all nodes from `other` to `this`. `other` will remain valid but empty.
    // This call is performed without heap allocations. TODO: Test that no allocations are happening.
    template <typename OtherMutex>
    void SpliceBack(SingleLockList<Value, OtherMutex>& other) noexcept {
        LockGuard<Mutex> guardThis(mutex_);
        LockGuard<OtherMutex> guardOther(other.mutex_);

        if (other.Empty()) {
            return;
        }

        if (Empty()) {
            root_ = std::move(other.root_);
            last_ = other.last_;
            other.last_ = nullptr;
            return;
        }

        RuntimeAssert(last_->next == nullptr, "last_ cannot have next");
        last_->next = std::move(other.root_);
        last_->next->previous = last_;
        last_ = other.last_;
        other.last_ = nullptr;
    }

    // Returned value locks `this` to perform safe iteration. `this` unlocks when
    // `Iterable` gets out of scope. Example usage:
    // for (auto& value: list.Iter()) {
    //    // Do something with `value`, there's a guarantee that it'll not be
    //    // destroyed mid-iteration.
    // }
    // // At this point `list` is unlocked.
    Iterable Iter() noexcept { return Iterable(this); }

private:
    // Allow access to private data regardless of `Value` and `Mutex`.
    template <typename, typename>
    friend class SingleLockList;

    bool Empty() const noexcept {
        bool empty = root_ == nullptr;
        RuntimeAssert((last_ == nullptr) == empty, "last_ is desynchronized with root_");
        return empty;
    }

    std::unique_ptr<Node> root_;
    Node* last_ = nullptr; // weak
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_SINGLE_LOCK_LIST_H
