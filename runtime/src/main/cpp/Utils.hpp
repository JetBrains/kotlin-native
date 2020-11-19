/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_UTILS_H
#define RUNTIME_UTILS_H

namespace kotlin {

// A helper for implementing classes with disabled copy constructor and copy assignment.
// Usage:
// class A: private MoveOnly {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `MoveOnly`.
class MoveOnly {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `MoveOnly`.
protected:
    MoveOnly() noexcept = default;
    MoveOnly(const MoveOnly&) = delete;
    MoveOnly(MoveOnly&&) noexcept = default;

    MoveOnly& operator=(const MoveOnly&) = delete;
    MoveOnly& operator=(MoveOnly&&) noexcept = default;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `MoveOnly` directly, so this destructor is never called in a virtual manner.
    ~MoveOnly() = default;
};

// A helper for implementing classes with disabled copy and move constructors, and copy and move assignments.
// Usage:
// class A: private Pinned {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `Pinned`.
class Pinned {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `Pinned`.
protected:
    Pinned() noexcept = default;
    Pinned(const Pinned&) = delete;
    Pinned(Pinned&&) = delete;

    Pinned& operator=(const Pinned&) = delete;
    Pinned& operator=(Pinned&&) = delete;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `Pinned` directly, so this destructor is never called in a virtual manner.
    ~Pinned() = default;
};

// Given
// struct SomeWrapper {
//     SomeType value;
//     ... // (possibly) some other fields
// };
// allows to cast from `SomeValue*` to `SomeWrapper*` as no-op. It only works
// if `SomeWrapper` is standard layout and `value` is the first non-static data member.
// See https://en.cppreference.com/w/cpp/language/data_members#Standard_layout
//
// Useful for exporting SomeType under a different name (e.g. exporting inner C++ class
// as public C struct).
//
// Also useful for implementing a form of private inheritance. It allows inheriting from
// primitive types, however there're no virtual functions (including virtual destructor).
// For example:
// // Module A
// struct SomeType { ... };
// // Module B API
// SomeType* constructSomeType(Args...);
// void useSomeType(SomeType*);
// // Module B
// struct InternalType {
//     SomeType value;
//     InternalData extra;
// };
// SomeType* constructSomeType(Args... args) {
//     auto* internalType = new InternalType{ SomeType(args...), InternalData(...) };
//     return &internalType->value;
// }
// void useSomeType(SomeType* value) {
//     InternalType* internalType = wrapper_cast(InternalType, value, value);
//     // Do something with InternalType
// }
#define wrapper_cast(Wrapper, inner, field) \
    /* With -O2 this lambda does not exist in the bitcode. */ \
    []() { \
        static_assert(std::is_standard_layout<Wrapper>::value, #Wrapper " must be standard layout"); \
        static_assert(offsetof(Wrapper, field) == 0, #field " must be at 0 offset"); \
    }(), \
            reinterpret_cast<Wrapper*>(inner)

} // namespace kotlin

#endif // RUNTIME_UTILS_H
