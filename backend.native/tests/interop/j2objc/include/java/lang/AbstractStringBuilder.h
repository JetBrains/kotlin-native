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

#ifndef _JavaLangAbstractStringBuilder_H_
#define _JavaLangAbstractStringBuilder_H_

#import "J2ObjC_header.h"
#import "java/lang/Appendable.h"
#import "java/lang/CharSequence.h"

@class IOSCharArray;
@class JavaLangStringBuffer;

// Defines a string builder struct so that J2ObjC string concatenation does not
// need to allocate a new ObjC string builder object.
typedef struct JreStringBuilder {
  jchar *buffer_;
  jint bufferSize_;
  jint count_;
} JreStringBuilder;

@interface JavaLangAbstractStringBuilder : NSObject < JavaLangAppendable, JavaLangCharSequence > {
 @package
  JreStringBuilder delegate_;
}

- (IOSCharArray *)getValue;

- (instancetype)initPackagePrivate;

- (instancetype)initPackagePrivateWithInt:(jint)capacity;

- (instancetype)initWithNSString:(NSString *)string;

- (jint)capacity;

- (jchar)charAtWithInt:(jint)index;

- (void)ensureCapacityWithInt:(jint)min;

- (void)getCharsWithInt:(jint)start
                withInt:(jint)end
          withCharArray:(IOSCharArray *)dst
                withInt:(jint)dstStart;

- (jint)java_length;

- (void)setCharAtWithInt:(jint)index
                withChar:(jchar)ch;

- (void)setLengthWithInt:(jint)length;

- (NSString *)substringWithInt:(jint)start;

- (NSString *)substringWithInt:(jint)start
                       withInt:(jint)end;

- (NSString *)description;

- (id<JavaLangCharSequence>)subSequenceFrom:(jint)start
                                         to:(jint)end;

- (jint)indexOfWithNSString:(NSString *)string;

- (jint)indexOfWithNSString:(NSString *)subString
                    withInt:(jint)start;

- (jint)lastIndexOfWithNSString:(NSString *)string;

- (jint)lastIndexOfWithNSString:(NSString *)subString
                        withInt:(jint)start;

- (void)trimToSize;

- (jint)codePointAtWithInt:(jint)index;

- (jint)codePointBeforeWithInt:(jint)index;

- (jint)codePointCountWithInt:(jint)start
                      withInt:(jint)end;

- (jint)offsetByCodePointsWithInt:(jint)index
                          withInt:(jint)codePointOffset;

@end

CF_EXTERN_C_BEGIN

void JavaLangAbstractStringBuilder_initPackagePrivate(JavaLangAbstractStringBuilder *self);
void JavaLangAbstractStringBuilder_initPackagePrivateWithInt_(
    JavaLangAbstractStringBuilder *self, jint capacity);
void JavaLangAbstractStringBuilder_initWithNSString_(
    JavaLangAbstractStringBuilder *self, NSString *string);

void JreStringBuilder_initWithCapacity(JreStringBuilder *sb, jint capacity);

void JreStringBuilder_appendNull(JreStringBuilder *sb);
void JreStringBuilder_appendBuffer(JreStringBuilder *sb, const unichar *buffer, int length);
void JreStringBuilder_appendStringBuffer(JreStringBuilder *sb, JavaLangStringBuffer *toAppend);
void JreStringBuilder_appendCharArray(JreStringBuilder *sb, IOSCharArray *chars);
void JreStringBuilder_appendCharArraySubset(
    JreStringBuilder *sb, IOSCharArray *chars, jint offset, jint length);
void JreStringBuilder_appendChar(JreStringBuilder *sb, jchar ch);
void JreStringBuilder_appendString(JreStringBuilder *sb, NSString *string);
void JreStringBuilder_appendCharSequence(JreStringBuilder *sb, id<JavaLangCharSequence> s);
void JreStringBuilder_appendCharSequenceSubset(
    JreStringBuilder *sb, id<JavaLangCharSequence> s, jint start, jint end);
void JreStringBuilder_appendInt(JreStringBuilder *sb, jint i);
void JreStringBuilder_appendLong(JreStringBuilder *sb, jlong l);
void JreStringBuilder_appendDouble(JreStringBuilder *sb, jdouble d);
void JreStringBuilder_appendFloat(JreStringBuilder *sb, jfloat f);

void JreStringBuilder_delete(JreStringBuilder *sb, jint start, jint end);
void JreStringBuilder_deleteCharAt(JreStringBuilder *sb, jint index);

void JreStringBuilder_insertCharArray(JreStringBuilder *sb, jint index, IOSCharArray *chars);
void JreStringBuilder_insertCharArraySubset(
    JreStringBuilder *sb, jint index, IOSCharArray *chars, jint start, jint length);
void JreStringBuilder_insertChar(JreStringBuilder *sb, jint index, jchar ch);
void JreStringBuilder_insertString(JreStringBuilder *sb, jint index, NSString *string);
void JreStringBuilder_insertCharSequence(
    JreStringBuilder *sb, jint index, id<JavaLangCharSequence> s, jint start, jint end);

void JreStringBuilder_replace(JreStringBuilder *sb, jint start, jint end, NSString *string);

void JreStringBuilder_reverse(JreStringBuilder *sb);

NSString *JreStringBuilder_toString(JreStringBuilder *sb);
NSString *JreStringBuilder_toStringAndDealloc(JreStringBuilder *sb);

CF_EXTERN_C_END

#endif // _JavaLangAbstractStringBuilder_H_
