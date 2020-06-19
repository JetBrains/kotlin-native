/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#import "Types.h"
#import "Memory.h"
#import "MemorySharedRefs.hpp"

#if KONAN_OBJC_INTEROP

#import <Foundation/NSObject.h>
#import <Foundation/NSValue.h>
#import <Foundation/NSString.h>
#import <Foundation/NSException.h>
#import <Foundation/NSDecimalNumber.h>
#import <objc/runtime.h>
#import <objc/objc-exception.h>
#import <dispatch/dispatch.h>

#import "ObjCExport.h"
#import "ObjCExportInit.h"
#import "ObjCExportPrivate.h"
#import "MemoryPrivate.hpp"
#import "Runtime.h"
#import "Utils.h"
#import "Exceptions.h"

extern "C" id objc_retainAutoreleaseReturnValue(id self);
extern "C" id objc_autoreleaseReturnValue(id self);

@interface NSObject (NSObjectPrivateMethods)
// Implemented for NSObject in libobjc/NSObject.mm
-(BOOL)_tryRetain;
@end;

static void injectToRuntime();

// Note: `KotlinBase`'s `toKotlin` and `_tryRetain` methods will terminate if
// called with non-frozen object on a wrong worker. `retain` will also terminate
// in these conditions if backref's refCount is zero.

@implementation KotlinBase {
  BackRefFromAssociatedObject refHolder;
}

-(KRef)toKotlin:(KRef*)OBJ_RESULT {
  RETURN_OBJ(refHolder.ref<ErrorPolicy::kTerminate>());
}

+(void)load {
  injectToRuntime();
}

+(void)initialize {
  if (self == [KotlinBase class]) {
    Kotlin_ObjCExport_initialize();
  }
  Kotlin_ObjCExport_initializeClass(self);
}

+(instancetype)allocWithZone:(NSZone*)zone {
  Kotlin_initRuntimeIfNeeded();

  KotlinBase* result = [super allocWithZone:zone];

  const TypeInfo* typeInfo = Kotlin_ObjCExport_getAssociatedTypeInfo(self);
  if (typeInfo == nullptr) {
    [NSException raise:NSGenericException
          format:@"%s is not allocatable or +[KotlinBase initialize] method wasn't called on it",
          class_getName(object_getClass(self))];
  }

  if (typeInfo->instanceSize_ < 0) {
    [NSException raise:NSGenericException
          format:@"%s must be allocated and initialized with a factory method",
          class_getName(object_getClass(self))];
  }
  ObjHolder holder;
  AllocInstanceWithAssociatedObject(typeInfo, result, holder.slot());
  result->refHolder.initAndAddRef(holder.obj());
  return result;
}

+(instancetype)createWrapper:(ObjHeader*)obj {
  KotlinBase* candidate = [super allocWithZone:nil];
  // TODO: should we call NSObject.init ?
  candidate->refHolder.initAndAddRef(obj);

  if (!obj->permanent()) { // TODO: permanent objects should probably be supported as custom types.
    if (!obj->container()->shareable()) {
      SetAssociatedObject(obj, candidate);
    } else {
      id old = AtomicCompareAndSwapAssociatedObject(obj, nullptr, candidate);
      if (old != nullptr) {
        candidate->refHolder.releaseRef();
        [candidate releaseAsAssociatedObject];
        return objc_retainAutoreleaseReturnValue(old);
      }
    }
  }

  return objc_autoreleaseReturnValue(candidate);
}

-(instancetype)retain {
  if (refHolder.permanent()) { // TODO: consider storing `isPermanent` to self field.
    [super retain];
  } else {
    refHolder.addRef<ErrorPolicy::kTerminate>();
  }
  return self;
}

-(BOOL)_tryRetain {
  if (refHolder.permanent()) {
    return [super _tryRetain];
  } else {
    return refHolder.tryAddRef<ErrorPolicy::kTerminate>();
  }
}

-(oneway void)release {
  if (refHolder.permanent()) {
    [super release];
  } else {
    refHolder.releaseRef();
  }
}

-(void)releaseAsAssociatedObject {
  [super release];
}

- (instancetype)copyWithZone:(NSZone *)zone {
  // TODO: write documentation.
  return [self retain];
}

@end;

@interface NSObject (NSObjectToKotlin)
@end;

@implementation NSObject (NSObjectToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}

-(void)releaseAsAssociatedObject {
  objc_release(self);
}
@end;

@interface NSString (NSStringToKotlin)
@end;

@implementation NSString (NSStringToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_Interop_CreateKStringFromNSString, self);
}
@end;

extern "C" {

OBJ_GETTER(Kotlin_boxByte, KByte value);
OBJ_GETTER(Kotlin_boxShort, KShort value);
OBJ_GETTER(Kotlin_boxInt, KInt value);
OBJ_GETTER(Kotlin_boxLong, KLong value);
OBJ_GETTER(Kotlin_boxUByte, KUByte value);
OBJ_GETTER(Kotlin_boxUShort, KUShort value);
OBJ_GETTER(Kotlin_boxUInt, KUInt value);
OBJ_GETTER(Kotlin_boxULong, KULong value);
OBJ_GETTER(Kotlin_boxFloat, KFloat value);
OBJ_GETTER(Kotlin_boxDouble, KDouble value);

}

@interface NSNumber (NSNumberToKotlin)
@end;

@implementation NSNumber (NSNumberToKotlin)
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  const char* type = self.objCType;

  // TODO: the code below makes some assumption on char, short, int and long sizes.

  switch (type[0]) {
    case 'c': RETURN_RESULT_OF(Kotlin_boxByte, self.charValue);
    case 's': RETURN_RESULT_OF(Kotlin_boxShort, self.shortValue);
    case 'i': RETURN_RESULT_OF(Kotlin_boxInt, self.intValue);
    case 'q': RETURN_RESULT_OF(Kotlin_boxLong, self.longLongValue);
    case 'C': RETURN_RESULT_OF(Kotlin_boxUByte, self.unsignedCharValue);
    case 'S': RETURN_RESULT_OF(Kotlin_boxUShort, self.unsignedShortValue);
    case 'I': RETURN_RESULT_OF(Kotlin_boxUInt, self.unsignedIntValue);
    case 'Q': RETURN_RESULT_OF(Kotlin_boxULong, self.unsignedLongLongValue);
    case 'f': RETURN_RESULT_OF(Kotlin_boxFloat, self.floatValue);
    case 'd': RETURN_RESULT_OF(Kotlin_boxDouble, self.doubleValue);

    default:  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
  }
}
@end;

@interface NSDecimalNumber (NSDecimalNumberToKotlin)
@end;

@implementation NSDecimalNumber (NSDecimalNumberToKotlin)
// Overrides [NSNumber toKotlin:] implementation.
-(ObjHeader*)toKotlin:(ObjHeader**)OBJ_RESULT {
  RETURN_RESULT_OF(Kotlin_ObjCExport_convertUnmappedObjCObject, self);
}
@end;

static void injectToRuntime() {
  RuntimeCheck(Kotlin_ObjCExport_toKotlinSelector == nullptr, "runtime injected twice");
  Kotlin_ObjCExport_toKotlinSelector = @selector(toKotlin:);

  RuntimeCheck(Kotlin_ObjCExport_releaseAsAssociatedObjectSelector == nullptr, "runtime injected twice");
  Kotlin_ObjCExport_releaseAsAssociatedObjectSelector = @selector(releaseAsAssociatedObject);
}

#endif // KONAN_OBJC_INTEROP
