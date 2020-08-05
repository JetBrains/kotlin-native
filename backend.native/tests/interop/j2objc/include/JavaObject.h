// Copyright 2011 Google Inc. All Rights Reserved.
//
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
//  JavaObject.h
//  JreEmulation
//
//  Created by Tom Ball on 12/12/11.
//

#ifndef _JavaObject_H_
#define _JavaObject_H_

#import <Foundation/Foundation.h>

@class IOSClass;

/// A protocol that defines Java Object-compatible methods.
@protocol JavaObject <NSObject>

// Returns a copy of the object, if it implements java.lang.Cloneable.
- (id)java_clone;

// Returns the IOSClass of the receiver.
- (IOSClass *)java_getClass;

// Wakes up a waiting thread (if any).
- (void)java_notify;

// Wakes up all waiting threads (if any).
- (void)java_notifyAll;

// Waits until another thread wakes it, or times out.
- (void)java_wait;
- (void)java_waitWithLong:(long long)timeout;
- (void)java_waitWithLong:(long long)timeout withInt:(int)nanos;

// Called upon deallocation of the object.
- (void)java_finalize;

@end

#endif // _JavaObject_H_
