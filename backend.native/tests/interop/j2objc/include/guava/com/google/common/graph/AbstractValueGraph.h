//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/graph/AbstractValueGraph.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractValueGraph")
#ifdef RESTRICT_ComGoogleCommonGraphAbstractValueGraph
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractValueGraph 0
#else
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractValueGraph 1
#endif
#undef RESTRICT_ComGoogleCommonGraphAbstractValueGraph

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonGraphAbstractValueGraph_) && (INCLUDE_ALL_ComGoogleCommonGraphAbstractValueGraph || defined(INCLUDE_ComGoogleCommonGraphAbstractValueGraph))
#define ComGoogleCommonGraphAbstractValueGraph_

#define RESTRICT_ComGoogleCommonGraphAbstractBaseGraph 1
#define INCLUDE_ComGoogleCommonGraphAbstractBaseGraph 1
#include "com/google/common/graph/AbstractBaseGraph.h"

#define RESTRICT_ComGoogleCommonGraphValueGraph 1
#define INCLUDE_ComGoogleCommonGraphValueGraph 1
#include "com/google/common/graph/ValueGraph.h"

@class JavaUtilOptional;
@protocol ComGoogleCommonGraphGraph;

/*!
 @brief This class provides a skeletal implementation of <code>ValueGraph</code>.It is recommended to extend
  this class rather than implement <code>ValueGraph</code> directly.
 <p>The methods implemented in this class should not be overridden unless the subclass admits a
  more efficient implementation.
 @author James Sexton
 @since 20.0
 */
@interface ComGoogleCommonGraphAbstractValueGraph : ComGoogleCommonGraphAbstractBaseGraph < ComGoogleCommonGraphValueGraph >

#pragma mark Public

- (instancetype __nonnull)init;

- (id<ComGoogleCommonGraphGraph>)asGraph;

- (JavaUtilOptional *)edgeValueWithId:(id)nodeU
                               withId:(id)nodeV;

- (jboolean)isEqual:(id __nullable)obj;

- (NSUInteger)hash;

/*!
 @brief Returns a string representation of this graph.
 */
- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivate NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonGraphAbstractValueGraph)

FOUNDATION_EXPORT void ComGoogleCommonGraphAbstractValueGraph_init(ComGoogleCommonGraphAbstractValueGraph *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonGraphAbstractValueGraph)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractValueGraph")
