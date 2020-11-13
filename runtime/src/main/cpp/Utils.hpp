/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_UTILS_H
#define RUNTIME_UTILS_H

namespace kotlin {

// A helper for implementing classes with disabled copy constructor and copy assignment.
// Usage:
// class A: private NoCopy {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `NoCopy`.
class NoCopy {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `NoCopy`.
protected:
    NoCopy() = default;
    NoCopy(const NoCopy&) = delete;
    NoCopy(NoCopy&&) = default;

    NoCopy& operator=(const NoCopy&) = delete;
    NoCopy& operator=(NoCopy&&) = default;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `NoCopy` directly, so this destructor is never called in a virtual manner.
    ~NoCopy() = default;
};

// A helper for implementing classes with disabled copy and move constructors, and copy and move assignments.
// Usage:
// class A: private NoCopyOrMove {
//     ...
// };
// Prefer private inheritance to discourage casting instances of `A` to instances
// of `NoCopyOrMove`.
class NoCopyOrMove {
    // Hide constructors, assignments and destructor, to discourage operating on an instance of `NoCopyOrMove`.
protected:
    NoCopyOrMove() = default;
    NoCopyOrMove(const NoCopyOrMove&) = delete;
    NoCopyOrMove(NoCopyOrMove&&) = delete;

    NoCopyOrMove& operator=(const NoCopyOrMove&) = delete;
    NoCopyOrMove& operator=(NoCopyOrMove&&) = delete;

    // Not virtual by design. Since this class hides this destructor, no one can destroy an
    // instance of `NoCopyOrMove` directly, so this destructor is never called in a virtual manner.
    ~NoCopyOrMove() = default;
};

} // namespace kotlin

#endif // RUNTIME_UTILS_H
