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
//  NSNumber+JavaNumber.h
//  JreEmulation
//
//  Created by Tom Ball on 12/6/13.
//

#ifndef _JavaLangNumber_H_
#define _JavaLangNumber_H_

#include "J2ObjC_common.h"
#include "java/io/Serializable.h"

//
// Adds the java.io.Serializable marker interface to NSNumber.
//
@interface NSNumber (JavaNumber) <JavaIoSerializable>

@end

__attribute__((always_inline)) inline void NSNumber_init(NSObject *self) {
  #pragma unused(self)
}

J2OBJC_EMPTY_STATIC_INIT(NSNumber)

J2OBJC_TYPE_LITERAL_HEADER(NSNumber)

// Empty class to force category to be loaded.
@interface JreNumberCategoryDummy : NSObject
@end

#endif // _JavaLangNumber_H_
