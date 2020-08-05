//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/errorprone/annotations/CanIgnoreReturnValue.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue")
#ifdef RESTRICT_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue
#define INCLUDE_ALL_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue 0
#else
#define INCLUDE_ALL_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue 1
#endif
#undef RESTRICT_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleErrorproneAnnotationsCanIgnoreReturnValue_) && (INCLUDE_ALL_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue || defined(INCLUDE_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue))
#define ComGoogleErrorproneAnnotationsCanIgnoreReturnValue_

#define RESTRICT_JavaLangAnnotationAnnotation 1
#define INCLUDE_JavaLangAnnotationAnnotation 1
#include "java/lang/annotation/Annotation.h"

@class IOSClass;

/*!
 @brief Indicates that the return value of the annotated method can be safely ignored.
 <p>This is the opposite of <code>javax.annotation.CheckReturnValue</code>. It can be used inside
  classes or packages annotated with <code>@@CheckReturnValue</code> to exempt specific methods from the
  default.
 */
@protocol ComGoogleErrorproneAnnotationsCanIgnoreReturnValue < JavaLangAnnotationAnnotation >

- (jboolean)isEqual:(id)obj;

- (NSUInteger)hash;

@end

@interface ComGoogleErrorproneAnnotationsCanIgnoreReturnValue : NSObject < ComGoogleErrorproneAnnotationsCanIgnoreReturnValue >

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleErrorproneAnnotationsCanIgnoreReturnValue)

FOUNDATION_EXPORT id<ComGoogleErrorproneAnnotationsCanIgnoreReturnValue> create_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleErrorproneAnnotationsCanIgnoreReturnValue)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleErrorproneAnnotationsCanIgnoreReturnValue")
