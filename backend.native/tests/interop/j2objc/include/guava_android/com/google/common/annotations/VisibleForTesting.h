//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/annotations/VisibleForTesting.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonAnnotationsVisibleForTesting")
#ifdef RESTRICT_ComGoogleCommonAnnotationsVisibleForTesting
#define INCLUDE_ALL_ComGoogleCommonAnnotationsVisibleForTesting 0
#else
#define INCLUDE_ALL_ComGoogleCommonAnnotationsVisibleForTesting 1
#endif
#undef RESTRICT_ComGoogleCommonAnnotationsVisibleForTesting

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonAnnotationsVisibleForTesting_) && (INCLUDE_ALL_ComGoogleCommonAnnotationsVisibleForTesting || defined(INCLUDE_ComGoogleCommonAnnotationsVisibleForTesting))
#define ComGoogleCommonAnnotationsVisibleForTesting_

#define RESTRICT_JavaLangAnnotationAnnotation 1
#define INCLUDE_JavaLangAnnotationAnnotation 1
#include "java/lang/annotation/Annotation.h"

@class IOSClass;

/*!
 @brief Annotates a program element that exists, or is more widely visible than otherwise necessary, only
  for use in test code.
 @author Johannes Henkel
 */
@protocol ComGoogleCommonAnnotationsVisibleForTesting < JavaLangAnnotationAnnotation >

- (jboolean)isEqual:(id)obj;

- (NSUInteger)hash;

@end

@interface ComGoogleCommonAnnotationsVisibleForTesting : NSObject < ComGoogleCommonAnnotationsVisibleForTesting >

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonAnnotationsVisibleForTesting)

FOUNDATION_EXPORT id<ComGoogleCommonAnnotationsVisibleForTesting> create_ComGoogleCommonAnnotationsVisibleForTesting(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonAnnotationsVisibleForTesting)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonAnnotationsVisibleForTesting")
