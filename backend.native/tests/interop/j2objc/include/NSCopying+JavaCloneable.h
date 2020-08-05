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
//  NSCopying+JavaCloneable.h
//  JreEmulation
//
//  Created by Keith Stanger on Jan 13, 2015.
//

#ifndef _JavaLangCloneable_H_
#define _JavaLangCloneable_H_

#include "J2ObjC_common.h"

@interface NSCopying : NSObject
@end

J2OBJC_EMPTY_STATIC_INIT(NSCopying)

J2OBJC_TYPE_LITERAL_HEADER(NSCopying)

#endif  // _JavaLangCloneable_H_
