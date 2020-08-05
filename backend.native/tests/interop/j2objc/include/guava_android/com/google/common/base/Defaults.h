//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/base/Defaults.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonBaseDefaults")
#ifdef RESTRICT_ComGoogleCommonBaseDefaults
#define INCLUDE_ALL_ComGoogleCommonBaseDefaults 0
#else
#define INCLUDE_ALL_ComGoogleCommonBaseDefaults 1
#endif
#undef RESTRICT_ComGoogleCommonBaseDefaults

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonBaseDefaults_) && (INCLUDE_ALL_ComGoogleCommonBaseDefaults || defined(INCLUDE_ComGoogleCommonBaseDefaults))
#define ComGoogleCommonBaseDefaults_

@class IOSClass;

/*!
 @brief This class provides default values for all Java types, as defined by the JLS.
 @author Ben Yu
 @since 1.0
 */
@interface ComGoogleCommonBaseDefaults : NSObject

#pragma mark Public

/*!
 @brief Returns the default value of <code>type</code> as defined by JLS --- <code>0</code> for numbers, <code>false</code>
  for <code>boolean</code> and <code>'\0'</code> for <code>char</code>.For non-primitive types and 
 <code>void</code>, <code>null</code> is returned.
 */
+ (id __nullable)defaultValueWithIOSClass:(IOSClass * __nonnull)type;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonBaseDefaults)

FOUNDATION_EXPORT id ComGoogleCommonBaseDefaults_defaultValueWithIOSClass_(IOSClass *type);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseDefaults)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonBaseDefaults")
