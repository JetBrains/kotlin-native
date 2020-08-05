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
// Prefix header for all source files of the 'JreEmulation' framework.
//

#ifndef _JreEmulation_H_
#define _JreEmulation_H_

#ifdef __OBJC__
#import "J2ObjC_common.h"
#import "JavaObject.h"
#import "IOSClass.h"
#import "IOSMetadata.h"
#import "IOSObjectArray.h"
#import "IOSPrimitiveArray.h"
#import "NSCopying+JavaCloneable.h"
#import "NSNumber+JavaNumber.h"
#import "NSObject+JavaObject.h"
#import "NSString+JavaString.h"
#endif // __OBJC__

#endif // _JreEmulation_H_
