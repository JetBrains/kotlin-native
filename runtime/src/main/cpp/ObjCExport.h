#ifndef RUNTIME_OBJCEXPORT_H
#define RUNTIME_OBJCEXPORT_H

#if KONAN_OBJC_INTEROP

#import <objc/runtime.h>

#import "Types.h"
#import "Memory.h"

extern "C" id objc_retain(id self);
extern "C" void objc_release(id self);

inline static bool HasAssociatedObjectField(ObjHeader* obj) {
  return HasReservedObjectTail(obj);
}

inline static id GetAssociatedObject(ObjHeader* obj) {
  return *reinterpret_cast<id*>(GetReservedObjectTail(obj));
}

inline static void SetAssociatedObject(ObjHeader* obj, id value) {
  *reinterpret_cast<id*>(GetReservedObjectTail(obj)) = value;
}

extern "C" id Kotlin_ObjCExport_refToObjC(ObjHeader* obj);
extern "C" OBJ_GETTER(Kotlin_ObjCExport_refFromObjC, id obj);

@protocol ConvertibleToKotlin
@required
-(KRef)toKotlin:(KRef*)OBJ_RESULT;
@end;

extern "C" id Kotlin_Interop_CreateNSStringFromKString(KRef str);
extern "C" OBJ_GETTER(Kotlin_Interop_CreateKStringFromNSString, NSString* str);

#endif // KONAN_OBJC_INTEROP

#endif // RUNTIME_OBJCEXPORT_H