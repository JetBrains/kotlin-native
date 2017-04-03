# _Kotlin/Native_ interoperability #

## Introduction ##

 _Kotlin/Native_ follows general tradition of Kotlin to provide excellent
existing platform software interoperability. In case of native platform
most important interoperability target is a C library. Thus _Kotlin/Native_
comes with an `cinterop` tool, which could be used to quickly generate
everything needed to interact with an external library.

 Following workflow is expected when interacting with the native library.
   * create `.def` file describing what to include into bindings
   * use `cinterop` tool to produce Kotlin bindings
   * run _Kotlin/Native_ compiler on an application to produce the final executable

 Interoperability tool analyses C headers and produces "natural" mapping of
types, function and constants into the Kotlin world. Generated stubs can be
imported into an IDE for purposes of code completion and navigation.

## Simple example ##

Build the dependencies and the compiler (see `README.md`).

Prepare stubs for the system sockets library:

    cd samples/socket
    ../../dist/bin/cinterop -def sockets.def -o sockets.kt.bc

Compile the echo server:

    ../../dist/bin/kotlinc EchoServer.kt -library sockets.kt.bc \
     -o EchoServer.kexe

This whole process is automated in `build.sh` script, which also support cross-compilation
to supported cross-targets with `TARGET=raspberrypi ./build.sh` (`cross_dist` target must
be executed first).

Run the server:

    ./EchoServer.kexe 3000 &

Test the server by conecting to it, for example with telnet:

    telnet localhost 3000

Write something to console and watch server echoing it back.

## Creating bindings for a new library ##

 To create bindings for a new library, start by creating `.def` file.
Structurally it's a simple property file, looking like this:


    header = zlib.h
    compilerOpts = -std=c99

Then run `cinterop` tool with something like (note that for host libraries not included
in sysroot search paths for headers may be needed):

    cinterop -def zlib.def -copt -I/opt/local/include -o zlib.kt.bc

This command will produce `zlib.kt.bc` compiled library and
`zlib.kt.bc-build/kotlin` directory containing Kotlin source code for the library.
``
If behavior for certain platform shall be modified, one may use format like
`compilerOpts.osx` or `compilerOpts.linux` to provide platform-specific values
to options.

Note, that generated bindings are generally platform-specific, so if developing for
multiple targets, bindings need to be regenerated.

After generation of bindings they could be used by IDE as proxy view of the
native library.

For typical Unix library with config script `compilerOpts` will likely contain
output of config script with `--cflags` flag (maybe without exact paths).

Output of config script with `--libs` shall be passed as `-linkedArgs`  `kotlinc`
flag value (quoted) when compiling.

## Using bindings ##

### Basic interop types ###

All supported C types have corresponding representations in Kotlin:

*   Singed, unsigned integral and floating point types are mapped to their
    Kotlin counterpart with the same width.
*   Pointers and arrays are mapped to `CPointer<T>?`.
*   Enums can be mapped to either Kotlin enum or integral values, depending on
    heuristics and definition file hints (see "Definition file hints" below).
*   Structs are mapped to types having fields available via dot notation,
    i.e. `someStructInstance.field1`.
*   `typedef`s are represented as `typealias`es.

Also any C type has the Kotlin type representing the lvalue of this type,
i.e. the value located in memory rather than simple immutable self-contained
value. Think C++ references, as similar concept.
For structs (and `typedef`s to structs) this representation is the main one
and has the same name as the struct itself, for Kotlin enums it is named
`${type}.Var`, for `CPointer<T>` it is `CPointerVar<T>`, and for most other
types it is `${type}Var`.

For those types that have both representations, the "lvalue" one has mutable
`.value` property for accessing value.

#### Pointer types ####

The type argument `T` of `CPointer<T>` must be one of the "lvalue" types
described above, e.g. the C type `struct S*` is mapped to `CPointer<S>`,
`int8_t*` is mapped to `CPointer<int_8tVar>`, and `char**` is mapped to
`CPointer<CPointerVar<ByteVar>>`.

C null pointer is represented as Kotlin's `null`, and the pointer type
`CPointer<T>` is not nullable, but the `CPointer<T>?` is. The values of this
type support all Kotlin operations related to handling `null`, e.g. `?:`, `?.`,
`!!` etc:
```
val path = getenv("PATH")?.toKString() ?: ""
```

Since the arrays are also mapped to `CPointer<T>`, it supports `[]` operator
for accessing values by index:

```
fun shift(ptr: CPointer<BytePtr>, length: Int) {
    for (index in 0 .. length - 2) {
        ptr[index] = ptr[index + 1]
    }
}
```

The `.pointed` property for `CPointer<T>` returns the lvalue of type `T`,
pointed by this pointer. The reverse operation is `.ptr`: it takes the lvalue
and returns the pointer to it.

`void*` is mapped to `COpaquePointer` – the special pointer type which is the
supertype for any other pointer type. So if the C function takes `void*`, then
the Kotlin binding accepts any `CPointer`.

Casting any pointer (including `COpaquePointer`) can be done with
`.reinterpret<T>`, e.g.:
```
val intPtr = bytePtr.reinterpret<IntVar>()
```
or
```
val intPtr: CPointer<IntVar> = bytePtr.reinterpret()
```

As in C, those reinterpret casts are unsafe and could potentially lead to
subtle memory problems in an application.

### Memory allocation ###

The native memory can be allocated using `NativePlacement` interface, e.g.
```
val byteVar = placement.alloc<ByteVar>()
```
or
```
val bytePtr = placement.allocArray<ByteVar>(5):
```

The most "natural" placement is object `nativeHeap`.
It corresponds to allocating native memory with `malloc` and provides additional
`.free()` operation to free allocated memory:

```
val buffer = nativeHeap.allocArray<ByteVar>(size)
<use buffer>
nativeHeap.free(buffer)
```

However the lifetime of allocated memory is often bound to lexical scope.
It is possible to define such scope with `memScoped { ... }`.
Inside the braces the temporary placement is available as implicit receiver,
so it is possible to allocate native memory with `alloc` and `allocArray`,
and the allocated memory will be automatically freed after leaving the scope.

For example, the C function returning values through pointer parameters can be
used like
```
val fileSize = memScoped {
    val statBuf = alloc<statStruct>()
    val error = stat("/", statBuf.ptr)
    statBuf.st_size
}
```

### Passing pointers to bindings ###

Although C pointers are mapped to `CPointer<T>` type, the C function
pointer-typed parameters are mapped to `CValuesRef<T>`. When passing
`CPointer<T>` as the value of such parameter, it is passed to C function as is.
However, the sequence of values can be passed instead of pointer. In this case
the sequence is passed "by value", i.e. the C function receives the pointer to
the temporary copy of that sequence, which is valid only until the function returns.

The `CValuesRef<T>` representation of pointer parameters is designed to support
C array literals without explicit native memory allocation.
To construct the immutable self-contained sequence of C values, the following
methods are provided:

*   `${type}Array.toCValues()`, where `type` is the Kotlin primitive type
*   `Array<CPointer<T>?>.toCValues()`, `List<CPointer<T>?>.toCValues()`
*   `cValuesOf(vararg elements: ${type})`, where `type` is primitive or pointer

For example:

C:
```
void foo(int* elements, int count);
...
int elements[] = {1, 2, 3};
foo(elements, 3);
```

Kotlin:
```
foo(cValuesOf(1, 2, 3), 3)
```

### Working with the strings ###

Unlike other pointers, the parameters of type `const char*` are represented as
Kotlin `String`. So it is possible to pass any Kotlin string to the binding
expecting C string.

There are also available some tools to convert between Kotlin and C strings
manually:

*   `fun CPointer<ByteRef>.toKString(): String`
*   `val String.cstr: CValuesRef<ByteRef>`.

    To get the pointer, `.cstr` should be allocated in native memory, e.g.
    ```
    val cString = kotlinString.cstr.getPointer(nativeHeap)
    ```

In all cases the C string is supposed to be encoded as UTF-8.

### Passing and receiving structs by value ###

When C function takes or returns a struct `T` by value, the corresponding
argument type or return type is represented as `CValue<T>`.

### Callbacks ###

To convert Kotlin function to pointer to C function,
`staticCFunction(::kotlinFunction)` can be used. Currently `staticCFunction`
heavily relies on type inference, so the expression `staticCFunction(...)`
should be either assigned to the variable having proper type explicitly
specified, or passed to the function, e.g.

```
glutDisplayFunc(staticCFunction(::display))
```

Note that some function types are not supported currently. For example,
it is not possible to get pointer to function that receives or returns structs
by value.

### Definition file hints ###

The `.def` file supports several options for adjusting generated bindings.

*   `excludedFunctions` property value specifies a space-separated list of names
    of functions that should be ignored. This may be required because a function
    declared in C header is not generally guaranteed to be really callable, and
    it is often hard or impossible to figure this out automatically. This option
    can also be used to workaround a bug in the interop itself.

*   `strictEnums` and `nonStrictEnums` properties values are space-separated
    lists of the enums that should be generated as Kotlin enum or as integral
    values correspondingly. If the enum is not included into any of these lists,
    than it is generated according to the heuristics.

### Portability ###

Sometimes the C libraries have function parameters or struct fields of
platform-dependent type, e.g. `long` or `size_t`. Kotlin itself doesn't provide
neither implicit integer casts nor C-style integer casts (e.g.
`(size_t) intValue`), so to make writing portable code in such cases easier,
the following methods are provided:

*   `fun ${type1}.sizeExtend<${type2}>(): ${type2}`
*   `fun ${type1}.narrow<${type2}>(): ${type2}`

where each of `type1` and `type2` must be an integral type.

The `signExtend` converts the integer value to more wide, i.e. the result must
have the same or greater size.
The `narrow` converts the integer value to smaller one (possibly changing the
value due to loosing significant bits), so the result must have the same or
less size.

Any allowed `.signExtend<${type}>` or `.narrow<${type}>` have the same
semantics as one of the `.toByte`, `.toShort`, `.toInt` or `.toLong` methods,
depending on `type`.

The example of using `signExtend`:

```
fun zeroMemory(buffer: COpaquePointer, size: Int) {
    memset(buffer, 0, size.signExtend<size_t>())
}
```

Also the type parameter can be inferred automatically and thus may be omitted
in some cases.