/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_SINGLE_LOCK_LIST_H
#define RUNTIME_SINGLE_LOCK_LIST_H

#include <cstddef>
#include <memory>
#include <mutex>

#include "Alloc.h"
#include "Mutex.hpp"
#include "Types.h"
#include "Utils.hpp"

namespace kotlin {

// TODO: Consider different locking mechanisms.
template <typename Value, typename Mutex = SpinLock>
class SingleLockList : private Pinned {
public:
    class Node : private Pinned, public KonanAllocatorAware<Node> {
    public:
        Value* Get() noexcept { return &value; }

    private:
        friend class SingleLockList;

        template <typename... Args>
        Node(Args... args) noexcept : value(args...) {}

        Value value;
        KStdUniquePtr<Node> next;
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

    class Iterable : private MoveOnly {
    public:
        explicit Iterable(SingleLockList* list) noexcept : list_(list), guard_(list->mutex_) {}

        Iterator begin() noexcept { return Iterator(list_->root_.get()); }

        Iterator end() noexcept { return Iterator(nullptr); }

    private:
        SingleLockList* list_;
        std::unique_lock<Mutex> guard_;
    };

    template <typename... Args>
    Node* Emplace(Args&&... args) noexcept {
        auto* nodePtr = new (Node::Alloc()) Node(std::forward<Args>(args)...);
        KStdUniquePtr<Node> node(nodePtr);
        std::lock_guard<Mutex> guard(mutex_);
        if (root_) {
            root_->previous = node.get();
        }
        node->next = std::move(root_);
        root_ = std::move(node);
        return nodePtr;
    }

    // Using `node` including its referred `Value` after `Erase` is undefined behaviour.
    void Erase(Node* node) noexcept {
        std::lock_guard<Mutex> guard(mutex_);
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

    // Returned value locks `this` to perform safe iteration. `this` unlocks when
    // `Iterable` gets out of scope. Example usage:
    // for (auto& value: list.Iter()) {
    //    // Do something with `value`, there's a guarantee that it'll not be
    //    // destroyed mid-iteration.
    // }
    // // At this point `list` is unlocked.
    Iterable Iter() noexcept { return Iterable(this); }

private:
    KStdUniquePtr<Node> root_;
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_SINGLE_LOCK_LIST_H
