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
//  JreRetainedWith.h
//  JreEmulation
//
//  Created by Keith Stanger on Mar. 18, 2016.
//
// INTERNAL ONLY. For use by JRE emulation code.

#ifndef JRE_RETAINED_WITH_H_
#define JRE_RETAINED_WITH_H_

#import <Foundation/Foundation.h>

// Called by @RetainedWith assignment functions. Caller must ensure that value
// has a retain count at least two.
FOUNDATION_EXPORT void JreRetainedWithInitialize(id parent, id value);

// Checks the previous value of a @RetainedWith assignment, possibly returning
// it to normal behavior.
FOUNDATION_EXPORT void JreRetainedWithHandlePreviousValue(id parent, id value);

// Called during dealloc of the parent and before releasing the child.
FOUNDATION_EXPORT void JreRetainedWithHandleDealloc(id parent, id child);

// Internal only macro that hacks the @RetainedWith behavior to a child class
// without needing to use class swizzling or associated objects. Must be
// combined with @Weak or @WeakOuter on the parent reference.
// NUM_REFS is the number of direct/indirect references to the child from
// the parent.
#define RETAINED_WITH_CHILD_NUM_REFS(PARENT_REF, NUM_REFS) \
  - (id)retain { \
    @synchronized (self) { \
      if ([self retainCount] == NUM_REFS) { \
        [PARENT_REF retain]; \
      } \
      return [super retain]; \
    } \
  } \
  - (oneway void)release { \
    @synchronized (self) { \
      if ([self retainCount] == NUM_REFS + 1) { \
        [PARENT_REF autorelease]; \
      } \
      [super release]; \
    } \
  }

#define RETAINED_WITH_CHILD(PARENT_REF) \
  RETAINED_WITH_CHILD_NUM_REFS(PARENT_REF, 1)

#endif // JRE_RETAINED_WITH_H_
