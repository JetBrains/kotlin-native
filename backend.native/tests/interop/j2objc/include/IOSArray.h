// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

//
//  IOSArray.h
//  JreEmulation
//

#ifndef IOSARRAY_H
#define IOSARRAY_H

#import "J2ObjC_common.h"
#import "NSObject+JavaObject.h"

@class IOSClass;

/**
 * An abstract class that represents a Java array.  Like a Java array,
 * an IOSArray is fixed-size but its elements are mutable.
 */
@interface IOSArray : NSObject < NSCopying > {
 @public
  /**
   * Size of the array. This field is read-only, visible only for
   * performance reasons. DO NOT MODIFY.
   */
  jint size_;
}

/** Returns the size of this array. */
- (jint)length;

/** Returns the description of a specified element in the array. */
- (NSString *)descriptionOfElementAtIndex:(jint)index;

/** Returns the element type of this array. */
- (IOSClass *)elementType;

/** Creates and returns an array containing the values from this array. */
- (id)java_clone;

/**
 * @brief Returns a pointer to the underlying array of elements.
 * The returned value is a raw pointer, so there are no index or
 * range checks. It is equivalent to the "IOS*Array_GetRef(array, 0)"
 * functions.
 */
- (void *)buffer;

@end

CF_EXTERN_C_BEGIN
void IOSArray_throwOutOfBoundsWithMsg(jint size, jint index);
void IOSArray_throwRangeOutOfBounds(jint size, jint offset, jint length);
CF_EXTERN_C_END

/** Implements the IOSArray |checkIndex| method as a C function. This
 * allows IOSArray index checks to be completely removed via the
 * J2OBJC_DISABLE_ARRAY_CHECKS macro to improve performance.
 */
__attribute__((always_inline)) inline void IOSArray_checkIndex(jint size, jint index) {
#if !defined(J2OBJC_DISABLE_ARRAY_BOUND_CHECKS)
  if (__builtin_expect(index < 0 || index >= size, 0)) {
    IOSArray_throwOutOfBoundsWithMsg(size, index);
  }
#endif
}

/** Implements the IOSArray |checkRange| method as a C function. This
 * allows IOSArray range checks to be completely removed via the
 * J2OBJC_DISABLE_ARRAY_CHECKS macro to improve performance.
 */
__attribute__((always_inline)) inline void IOSArray_checkRange(
    jint size, jint offset, jint length) {
#if !defined(J2OBJC_DISABLE_ARRAY_BOUND_CHECKS)
  if (__builtin_expect(length < 0 || offset < 0 || offset + length > size, 0)) {
    IOSArray_throwRangeOutOfBounds(size, offset, length);
  }
#endif
}

#endif // IOSARRAY_H
