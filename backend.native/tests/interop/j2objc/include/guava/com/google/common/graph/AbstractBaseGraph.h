//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/graph/AbstractBaseGraph.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractBaseGraph")
#ifdef RESTRICT_ComGoogleCommonGraphAbstractBaseGraph
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractBaseGraph 0
#else
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractBaseGraph 1
#endif
#undef RESTRICT_ComGoogleCommonGraphAbstractBaseGraph

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonGraphAbstractBaseGraph_) && (INCLUDE_ALL_ComGoogleCommonGraphAbstractBaseGraph || defined(INCLUDE_ComGoogleCommonGraphAbstractBaseGraph))
#define ComGoogleCommonGraphAbstractBaseGraph_

#define RESTRICT_ComGoogleCommonGraphBaseGraph 1
#define INCLUDE_ComGoogleCommonGraphBaseGraph 1
#include "com/google/common/graph/BaseGraph.h"

@protocol JavaUtilSet;

/*!
 @brief This class provides a skeletal implementation of <code>BaseGraph</code>.
 <p>The methods implemented in this class should not be overridden unless the subclass admits a
  more efficient implementation.
 @author James Sexton
 */
@interface ComGoogleCommonGraphAbstractBaseGraph : NSObject < ComGoogleCommonGraphBaseGraph >

#pragma mark Public

- (jint)degreeWithId:(id)node;

/*!
 @brief An implementation of <code>BaseGraph.edges()</code> defined in terms of <code>nodes()</code> and <code>successors(Object)</code>
 .
 */
- (id<JavaUtilSet>)edges;

- (jboolean)hasEdgeConnectingWithId:(id)nodeU
                             withId:(id)nodeV;

- (id<JavaUtilSet>)incidentEdgesWithId:(id)node;

- (jint)inDegreeWithId:(id)node;

- (jint)outDegreeWithId:(id)node;

#pragma mark Protected

/*!
 @brief Returns the number of edges in this graph; used to calculate the size of <code>edges()</code>.This
  implementation requires O(|N|) time.
 Classes extending this one may manually keep track of the
  number of edges as the graph is updated, and override this method for better performance.
 */
- (jlong)edgeCount;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivate;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonGraphAbstractBaseGraph)

FOUNDATION_EXPORT void ComGoogleCommonGraphAbstractBaseGraph_initPackagePrivate(ComGoogleCommonGraphAbstractBaseGraph *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonGraphAbstractBaseGraph)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractBaseGraph")
