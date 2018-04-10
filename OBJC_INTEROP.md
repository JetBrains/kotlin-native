# _Kotlin/Native_ interoperability with Swift/Objective-C

This documents covers some details of Kotlin/Native interoperability with
Swift/Objective-C.

## Usage

Kotlin/Native provides bidirectional interoperability with Objective-C.
Objective-C frameworks and libraries can be used in Kotlin code if
properly attached to the build (system frameworks are attached by default).
See e.g. "Interop libraries" in
[Gradle plugin documentation](GRADLE_PLUGIN.md#building-artifacts).
Swift library can be used in Kotlin code if its API is exported to Objective-C
with `@objc`.

Kotlin module can be used in Swift/Objective-C code if compiled into a
[framework](GRADLE_PLUGIN.md#framework). See [calculator sample](samples/calculator)
as an example.

## Mappings

The table below shows how Kotlin concepts are mapped to Swift/Objective-C and vice versa.

| Kotlin | Swift | Objective-C | Notes |
| ------ | ----- |------------ | ----- |
| `class` | `class` | `@interface` | [note](#name-translation) |
| `interface` | `protocol` | `@protocol` | |
| `constructor` | Initializer | Initializer | |
| Property | Property | Property | [note](#top-level-functions-and-properties) |
| Method | Method | Method | [note](#top-level-functions-and-properties) |
| `@Throws` | `throws` | `error:(NSError**)error` | [note](#errors-and-exceptions) |
| Extension | Extension | Category member | [note](#category-members) |
| Companion object member <- | Class method or property | Class method or property |  |
| `Singleton` | `Singleton()`  | `[Singleton singleton]` | |
| Primitive type | Primitive type / `NSNumber` | | [note](#number) |
| `String` | `String` | `NSString` | |
| `String` | `NSMutableString` | `NSMutableString` | [note](#nsmutablestring) |
| `List` | `Array` | `NSArray` | |
| `MutableList` | `NSMutableArray` | `NSMutableArray` | |
| `Set` | `Set` | `NSSet` | |
| `MutableSet` | `NSMutableSet` | `NSMutableSet` | [note](#mutable-collections) |
| `Map` | `Dictionary` | `NSDictionary` | |
| `MutableMap` | `NSMutableDictionary` | `NSMutableDictionary` | [note](#mutable-collections) |
| Function type | Function type | Block pointer type | |

### Name translation

Objective-C classes are imported into Kotlin with their original names.
Protocols are imported as interfaces with `Protocol` name suffix,
i.e. `@protocol Foo` -> `interface FooProtocol`.
These classes and interfaces are placed into package [specified in build configuration](#usage)
(`platform.*` packages for preconfigured system frameworks).

Names of Kotlin classes and interfaces are prefixed when imported to Swift/Objective-C.
The prefix is derived from the framework name.

### Top-level functions and properties

Top-level Kotlin functions and properties are accessible as members of a special class.
Each Kotlin package is translated into such a class.

### Errors and exceptions

Kotlin has no concept of checked exceptions, all Kotlin exceptions are unchecked.
Swift has only checked errors. So if Swift or Objective-C code calls Kotlin method
which throws an exception to be handled, then Kotlin method should be marked
with `@Throws` annotation. In this case all Kotlin exceptions
(except for instances of `Error`, `RuntimeException` and subclasses) are translated to
Swift error/`NSError`.

Note that the opposite translation is not implemented yet:
Swift/Objective-C error-throwing methods aren't imported to Kotlin as
exception-throwing.

### Category members

Members of Objective-C categories and Swift extensions are imported to Kotlin
as extensions. That's why these declarations can't be overridden in Kotlin.
And extension initializers aren't available as Kotlin constructors.

### Number

`NSNumber` is not automatically translated to Kotlin primitive types
when used as Swift/Objective-C parameter type or return value.

### NSMutableString

`NSMutableString` Objective-C class is not available from Kotlin.
All instances of `NSMutableString` are copied when passed to Kotlin.

### Mutable collections

Every Kotlin `MutableSet` is `NSMutableSet`, however the opposite is not true.
To pass an object for Kotlin `MutableSet`, one can create this kind of Kotlin collection
explicitly by either creating it in Kotlin with e.g. `mutableSetOf()`,
or using `${prefix}MutableSet` class in Swift/Objective-C, where `prefix`
is the framework names prefix.

The same holds for `MutableMap`.

## Casting between mapped types

When writing Kotlin code, an object may require to be converted from Kotlin type
to equivalent Swift/Objective-C type (or vice versa). In this case plain old
Kotlin cast can be used, e.g.
```
val nsArray = listOf(1, 2, 3) as NSArray
val string = nsString as String
val nsNumber = 42 as NSNumber
```

## Subclassing

Kotlin classes and interfaces can be subclassed by Swift/Objective-C classes
and protocols.
Currently a class that adopts Kotlin protocol should inherit `NSObject`
(either directly or indirectly). Note that all Kotlin classes do inherit `NSObject`.

Swift/Objective-C classes can be subclassed with Kotlin `final` class.

## C features

See [INTEROP.md](INTEROP.md) for the case when library uses some plain C features
(e.g. unsafe pointers, structs etc.).
