#include <stdio.h>
#include "testlib_api.h"

#define __ testlib_symbols()->
#define T_(x) testlib_kref_ ## x
#define CAST(T, v) testlib_kref_ ## T { .pinned = v }

int main(void) {
    T_(Base) base = __ kotlin.root.Base.Base();
    T_(Child) child = __ kotlin.root.Child.Child();
    T_(Impl1) impl1 = __ kotlin.root.Impl1.Impl1();
    T_(Impl2) impl2 = __ kotlin.root.Impl2.Impl2();
    T_(Base) casted_child = { .pinned = child.pinned };
    T_(I) casted_impl1 = { .pinned = impl1.pinned };
    T_(I) casted_impl2 = { .pinned = impl2.pinned };
    const char* string = __ kotlin.root.getString();

    __ kotlin.root.hello();
    __ kotlin.root.Base.foo(base);
    __ kotlin.root.Base.fooParam(base, "a", 1);
    __ kotlin.root.Child.fooParam(child, "b", 2);
    __ kotlin.root.Base.fooParam(casted_child, "c", 3);
    __ kotlin.root.I.foo(casted_impl1, "d", 4, casted_impl1);
    __ kotlin.root.I.foo(casted_impl2, "e", 5, casted_impl2);

    printf("String is %s\n", string);

    __ DisposeString(string);
    __ DisposeStablePointer(base.pinned);
    __ DisposeStablePointer(child.pinned);
    __ DisposeStablePointer(impl1.pinned);
    __ DisposeStablePointer(impl2.pinned);

    return 0;
}

