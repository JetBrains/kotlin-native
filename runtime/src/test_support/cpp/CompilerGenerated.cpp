/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include "TestSupportCompilerGenerated.hpp"

#include "Types.h"

namespace {

TypeInfo theAnyTypeInfoImpl = {};
TypeInfo theArrayTypeInfoImpl = {};
TypeInfo theBooleanArrayTypeInfoImpl = {};
TypeInfo theByteArrayTypeInfoImpl = {};
TypeInfo theCharArrayTypeInfoImpl = {};
TypeInfo theDoubleArrayTypeInfoImpl = {};
TypeInfo theFloatArrayTypeInfoImpl = {};
TypeInfo theForeignObjCObjectTypeInfoImpl = {};
TypeInfo theFreezableAtomicReferenceTypeInfoImpl = {};
TypeInfo theIntArrayTypeInfoImpl = {};
TypeInfo theLongArrayTypeInfoImpl = {};
TypeInfo theNativePtrArrayTypeInfoImpl = {};
TypeInfo theObjCObjectWrapperTypeInfoImpl = {};
TypeInfo theOpaqueFunctionTypeInfoImpl = {};
TypeInfo theShortArrayTypeInfoImpl = {};
TypeInfo theStringTypeInfoImpl = {};
TypeInfo theThrowableTypeInfoImpl = {};
TypeInfo theUnitTypeInfoImpl = {};
TypeInfo theWorkerBoundReferenceTypeInfoImpl = {};
TypeInfo theCleanerImplTypeInfoImpl = {};

ArrayHeader theEmptyStringImpl = { &theStringTypeInfoImpl, /* element count */ 0 };

template <class T>
struct KBox {
    ObjHeader header;
    const T value;
};

testing::StrictMock<testing::MockFunction<KInt()>>* createCleanerWorkerMock = nullptr;
testing::StrictMock<testing::MockFunction<void(KInt, bool)>>* shutdownCleanerWorkerMock = nullptr;

} // namespace

extern "C" {

// Set to 1 to enable runtime assertions.
extern const int KonanNeedDebugInfo = 1;

extern const TypeInfo* theAnyTypeInfo = &theAnyTypeInfoImpl;
extern const TypeInfo* theArrayTypeInfo = &theArrayTypeInfoImpl;
extern const TypeInfo* theBooleanArrayTypeInfo = &theBooleanArrayTypeInfoImpl;
extern const TypeInfo* theByteArrayTypeInfo = &theByteArrayTypeInfoImpl;
extern const TypeInfo* theCharArrayTypeInfo = &theCharArrayTypeInfoImpl;
extern const TypeInfo* theDoubleArrayTypeInfo = &theDoubleArrayTypeInfoImpl;
extern const TypeInfo* theFloatArrayTypeInfo = &theFloatArrayTypeInfoImpl;
extern const TypeInfo* theForeignObjCObjectTypeInfo = &theForeignObjCObjectTypeInfoImpl;
extern const TypeInfo* theFreezableAtomicReferenceTypeInfo = &theFreezableAtomicReferenceTypeInfoImpl;
extern const TypeInfo* theIntArrayTypeInfo = &theIntArrayTypeInfoImpl;
extern const TypeInfo* theLongArrayTypeInfo = &theLongArrayTypeInfoImpl;
extern const TypeInfo* theNativePtrArrayTypeInfo = &theNativePtrArrayTypeInfoImpl;
extern const TypeInfo* theObjCObjectWrapperTypeInfo = &theObjCObjectWrapperTypeInfoImpl;
extern const TypeInfo* theOpaqueFunctionTypeInfo = &theOpaqueFunctionTypeInfoImpl;
extern const TypeInfo* theShortArrayTypeInfo = &theShortArrayTypeInfoImpl;
extern const TypeInfo* theStringTypeInfo = &theStringTypeInfoImpl;
extern const TypeInfo* theThrowableTypeInfo = &theThrowableTypeInfoImpl;
extern const TypeInfo* theUnitTypeInfo = &theUnitTypeInfoImpl;
extern const TypeInfo* theWorkerBoundReferenceTypeInfo = &theWorkerBoundReferenceTypeInfoImpl;
extern const TypeInfo* theCleanerImplTypeInfo = &theCleanerImplTypeInfoImpl;

extern const ArrayHeader theEmptyArray = { &theArrayTypeInfoImpl, /* element count */0 };

OBJ_GETTER0(TheEmptyString) {
    RETURN_OBJ(theEmptyStringImpl.obj());
}

RUNTIME_NORETURN OBJ_GETTER(makeWeakReferenceCounter, void*) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(makePermanentWeakReferenceImpl, void*) {
    throw std::runtime_error("Not implemented for tests");
}

RUNTIME_NORETURN OBJ_GETTER(makeObjCWeakReferenceImpl, void*) {
    throw std::runtime_error("Not implemented for tests");
}

void checkRangeIndexes(KInt from, KInt to, KInt size) {
    if (from < 0 || to > size) {
        throw std::out_of_range("Index out of bounds: from=" + std::to_string(from)
                + ", to=" + std::to_string(to)
                + ", size=" + std::to_string(size));
    }
    if (from > to) {
        throw std::invalid_argument("Illegal argument: from > to, from=" + std::to_string(from) + ", to=" + std::to_string(to));
    }
}

RUNTIME_NORETURN OBJ_GETTER(WorkerLaunchpad, KRef) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowWorkerInvalidState() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNullPointerException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowArrayIndexOutOfBoundsException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowClassCastException(const ObjHeader* instance, const TypeInfo* type_info) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowArithmeticException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNumberFormatException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowOutOfMemoryError() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowNotImplementedError() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowCharacterCodingException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalArgumentException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalStateException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowInvalidMutabilityException(KConstRef where) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIncorrectDereferenceException() {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowIllegalObjectSharingException(KConstNativePtr typeInfo, KConstNativePtr address) {
    throw std::runtime_error("Not implemented for tests");
}

void RUNTIME_NORETURN ThrowFreezingException(KRef toFreeze, KRef blocker) {
    throw std::runtime_error("Not implemented for tests");
}

void ReportUnhandledException(KRef throwable) {
    konan::consolePrintf("Uncaught Kotlin exception.");
}

RUNTIME_NORETURN OBJ_GETTER(DescribeObjectForDebugging, KConstNativePtr typeInfo, KConstNativePtr address) {
    throw std::runtime_error("Not implemented for tests");
}

void OnUnhandledException(KRef throwable) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_WorkerBoundReference_freezeHook(KRef thiz) {
    throw std::runtime_error("Not implemented for tests");
}

extern const KBoolean BOOLEAN_RANGE_FROM = false;
extern const KBoolean BOOLEAN_RANGE_TO = true;
extern KBox<KBoolean> BOOLEAN_CACHE[] = {
        {{}, false},
        {{}, true},
};

OBJ_GETTER(Kotlin_boxBoolean, KBoolean value) {
    if (value) {
        RETURN_OBJ(&BOOLEAN_CACHE[1].header);
    } else {
        RETURN_OBJ(&BOOLEAN_CACHE[0].header);
    }
}

extern const KByte BYTE_RANGE_FROM = -1;
extern const KByte BYTE_RANGE_TO = 1;
extern KBox<KByte> BYTE_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KChar CHAR_RANGE_FROM = 0;
extern const KChar CHAR_RANGE_TO = 2;
extern KBox<KChar> CHAR_CACHE[] = {
        {{}, 0},
        {{}, 1},
        {{}, 2},
};

extern const KShort SHORT_RANGE_FROM = -1;
extern const KShort SHORT_RANGE_TO = 1;
extern KBox<KShort> SHORT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KInt INT_RANGE_FROM = -1;
extern const KInt INT_RANGE_TO = 1;
extern KBox<KInt> INT_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

extern const KLong LONG_RANGE_FROM = -1;
extern const KLong LONG_RANGE_TO = 1;
extern KBox<KLong> LONG_CACHE[] = {
        {{}, -1},
        {{}, 0},
        {{}, 1},
};

RUNTIME_NORETURN OBJ_GETTER(Kotlin_Throwable_getMessage, KRef throwable) {
    throw std::runtime_error("Not implemented for tests");
}

void Kotlin_CleanerImpl_shutdownCleanerWorker(KInt worker, bool executeScheduledCleaners) {
    if (!shutdownCleanerWorkerMock) throw std::runtime_error("Not implemented for tests");

    return shutdownCleanerWorkerMock->Call(worker, executeScheduledCleaners);
}

KInt Kotlin_CleanerImpl_createCleanerWorker() {
    if (!createCleanerWorkerMock) throw std::runtime_error("Not implemented for tests");

    return createCleanerWorkerMock->Call();
}

} // extern "C"

ScopedStrictMockFunction<KInt()> ScopedCreateCleanerWorkerMock() {
    return ScopedStrictMockFunction<KInt()>(&createCleanerWorkerMock);
}

ScopedStrictMockFunction<void(KInt, bool)> ScopedShutdownCleanerWorkerMock() {
    return ScopedStrictMockFunction<void(KInt, bool)>(&shutdownCleanerWorkerMock);
}
