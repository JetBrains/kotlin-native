//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/base/PatternCompiler.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonBasePatternCompiler")
#ifdef RESTRICT_ComGoogleCommonBasePatternCompiler
#define INCLUDE_ALL_ComGoogleCommonBasePatternCompiler 0
#else
#define INCLUDE_ALL_ComGoogleCommonBasePatternCompiler 1
#endif
#undef RESTRICT_ComGoogleCommonBasePatternCompiler

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonBasePatternCompiler_) && (INCLUDE_ALL_ComGoogleCommonBasePatternCompiler || defined(INCLUDE_ComGoogleCommonBasePatternCompiler))
#define ComGoogleCommonBasePatternCompiler_

@class ComGoogleCommonBaseCommonPattern;

/*!
 @brief Pluggable interface for compiling a regex pattern.By default this package uses the <code>java.util.regex</code>
  library, but an alternate implementation can be supplied using the <code>java.util.ServiceLoader</code>
  mechanism.
 */
@protocol ComGoogleCommonBasePatternCompiler < JavaObject >

/*!
 @brief Compiles the given pattern.
 @throw IllegalArgumentExceptionif the pattern is invalid
 */
- (ComGoogleCommonBaseCommonPattern *)compileWithNSString:(NSString * __nonnull)pattern;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonBasePatternCompiler)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBasePatternCompiler)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonBasePatternCompiler")
