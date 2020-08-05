//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/math/MathPreconditions.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonMathMathPreconditions")
#ifdef RESTRICT_ComGoogleCommonMathMathPreconditions
#define INCLUDE_ALL_ComGoogleCommonMathMathPreconditions 0
#else
#define INCLUDE_ALL_ComGoogleCommonMathMathPreconditions 1
#endif
#undef RESTRICT_ComGoogleCommonMathMathPreconditions

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonMathMathPreconditions_) && (INCLUDE_ALL_ComGoogleCommonMathMathPreconditions || defined(INCLUDE_ComGoogleCommonMathMathPreconditions))
#define ComGoogleCommonMathMathPreconditions_

@class JavaMathBigInteger;

/*!
 @brief A collection of preconditions for math functions.
 @author Louis Wasserman
 */
@interface ComGoogleCommonMathMathPreconditions : NSObject

#pragma mark Package-Private

+ (void)checkInRangeWithBoolean:(jboolean)condition;

+ (JavaMathBigInteger *)checkNonNegativeWithNSString:(NSString * __nullable)role
                              withJavaMathBigInteger:(JavaMathBigInteger * __nonnull)x;

+ (jdouble)checkNonNegativeWithNSString:(NSString * __nullable)role
                             withDouble:(jdouble)x;

+ (jint)checkNonNegativeWithNSString:(NSString * __nullable)role
                             withInt:(jint)x;

+ (jlong)checkNonNegativeWithNSString:(NSString * __nullable)role
                             withLong:(jlong)x;

+ (void)checkNoOverflowWithBoolean:(jboolean)condition
                      withNSString:(NSString * __nonnull)methodName
                           withInt:(jint)a
                           withInt:(jint)b;

+ (void)checkNoOverflowWithBoolean:(jboolean)condition
                      withNSString:(NSString * __nonnull)methodName
                          withLong:(jlong)a
                          withLong:(jlong)b;

+ (JavaMathBigInteger *)checkPositiveWithNSString:(NSString * __nullable)role
                           withJavaMathBigInteger:(JavaMathBigInteger * __nonnull)x;

+ (jint)checkPositiveWithNSString:(NSString * __nullable)role
                          withInt:(jint)x;

+ (jlong)checkPositiveWithNSString:(NSString * __nullable)role
                          withLong:(jlong)x;

+ (void)checkRoundingUnnecessaryWithBoolean:(jboolean)condition;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonMathMathPreconditions)

FOUNDATION_EXPORT jint ComGoogleCommonMathMathPreconditions_checkPositiveWithNSString_withInt_(NSString *role, jint x);

FOUNDATION_EXPORT jlong ComGoogleCommonMathMathPreconditions_checkPositiveWithNSString_withLong_(NSString *role, jlong x);

FOUNDATION_EXPORT JavaMathBigInteger *ComGoogleCommonMathMathPreconditions_checkPositiveWithNSString_withJavaMathBigInteger_(NSString *role, JavaMathBigInteger *x);

FOUNDATION_EXPORT jint ComGoogleCommonMathMathPreconditions_checkNonNegativeWithNSString_withInt_(NSString *role, jint x);

FOUNDATION_EXPORT jlong ComGoogleCommonMathMathPreconditions_checkNonNegativeWithNSString_withLong_(NSString *role, jlong x);

FOUNDATION_EXPORT JavaMathBigInteger *ComGoogleCommonMathMathPreconditions_checkNonNegativeWithNSString_withJavaMathBigInteger_(NSString *role, JavaMathBigInteger *x);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathMathPreconditions_checkNonNegativeWithNSString_withDouble_(NSString *role, jdouble x);

FOUNDATION_EXPORT void ComGoogleCommonMathMathPreconditions_checkRoundingUnnecessaryWithBoolean_(jboolean condition);

FOUNDATION_EXPORT void ComGoogleCommonMathMathPreconditions_checkInRangeWithBoolean_(jboolean condition);

FOUNDATION_EXPORT void ComGoogleCommonMathMathPreconditions_checkNoOverflowWithBoolean_withNSString_withInt_withInt_(jboolean condition, NSString *methodName, jint a, jint b);

FOUNDATION_EXPORT void ComGoogleCommonMathMathPreconditions_checkNoOverflowWithBoolean_withNSString_withLong_withLong_(jboolean condition, NSString *methodName, jlong a, jlong b);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonMathMathPreconditions)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonMathMathPreconditions")
