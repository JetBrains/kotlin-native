//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/collect/Interner.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectInterner")
#ifdef RESTRICT_ComGoogleCommonCollectInterner
#define INCLUDE_ALL_ComGoogleCommonCollectInterner 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectInterner 1
#endif
#undef RESTRICT_ComGoogleCommonCollectInterner

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectInterner_) && (INCLUDE_ALL_ComGoogleCommonCollectInterner || defined(INCLUDE_ComGoogleCommonCollectInterner))
#define ComGoogleCommonCollectInterner_

/*!
 @brief Provides equivalent behavior to <code>String.intern</code> for other immutable types.Common
  implementations are available from the <code>Interners</code> class.
 @author Kevin Bourrillion
 @since 3.0
 */
@protocol ComGoogleCommonCollectInterner < JavaObject >

/*!
 @brief Chooses and returns the representative instance for any of a collection of instances that are
  equal to each other.If two equal inputs are given to this method,
  both calls will return the same instance.
 That is, <code>intern(a).equals(a)</code> always holds,
  and <code>intern(a) == intern(b)</code> if and only if <code>a.equals(b)</code>. Note that <code>intern(a)</code>
  is permitted to return one instance now and a different instance later if the
  original interned instance was garbage-collected. 
 <p><b>Warning:</b> do not use with mutable objects.
 @throw NullPointerExceptionif <code>sample</code> is null
 */
- (id)internWithId:(id __nonnull)sample;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectInterner)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectInterner)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectInterner")
