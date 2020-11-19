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

// TODO: Consider different locking mechanisms.
template <typename Value, typename Mutex = SimpleMutex>
class SingleLockList : private Pinned {
private:
    struct NodeImpl;

public:
    using Node = NodeImpl;

    class Iterator {
    public:
        explicit Iterator(NodeImpl* node) noexcept : node_(node) {}

        Value& operator*() noexcept { return node_->value; }

        Iterator& operator++() noexcept {
            node_ = node_->next.get();
            return *this;
        }

        bool operator==(const Iterator& rhs) const noexcept { return node_ == rhs.node_; }

        bool operator!=(const Iterator& rhs) const noexcept { return node_ != rhs.node_; }

    private:
        NodeImpl* node_;
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
    Node* Emplace(Args... args) noexcept {
        auto node = std_support::make_unique<NodeImpl>(args...);
        auto* result = node.get();
        LockGuard<Mutex> guard(mutex_);
        if (root_) {
            root_->previous = node.get();
        }
        node->next = std::move(root_);
        root_ = std::move(node);
        return result;
    }

    // Using `node` including its referred `Value` after `Erase` is undefined behaviour.
    void Erase(Node* node) noexcept {
        LockGuard<Mutex> guard(mutex_);
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

    static Value* ValueForNode(Node* node) noexcept { return &node->value; }

private:
    struct NodeImpl {
        template <typename... Args>
        NodeImpl(Args... args) noexcept : value(args...) {}

        Value value;
        // TODO: Consider adding a marker for checks in debug mode if Value was constructed inside the Node.
        std::unique_ptr<Node> next;
        Node* previous = nullptr; // weak
    };

    std::unique_ptr<NodeImpl> root_;
    Mutex mutex_;
};

} // namespace kotlin

#endif // RUNTIME_SINGLE_LOCK_LIST_H
