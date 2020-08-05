//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/graph/ForwardingValueGraph.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonGraphForwardingValueGraph")
#ifdef RESTRICT_ComGoogleCommonGraphForwardingValueGraph
#define INCLUDE_ALL_ComGoogleCommonGraphForwardingValueGraph 0
#else
#define INCLUDE_ALL_ComGoogleCommonGraphForwardingValueGraph 1
#endif
#undef RESTRICT_ComGoogleCommonGraphForwardingValueGraph

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonGraphForwardingValueGraph_) && (INCLUDE_ALL_ComGoogleCommonGraphForwardingValueGraph || defined(INCLUDE_ComGoogleCommonGraphForwardingValueGraph))
#define ComGoogleCommonGraphForwardingValueGraph_

#define RESTRICT_ComGoogleCommonGraphAbstractValueGraph 1
#define INCLUDE_ComGoogleCommonGraphAbstractValueGraph 1
#include "com/google/common/graph/AbstractValueGraph.h"

@class ComGoogleCommonGraphElementOrder;
@protocol ComGoogleCommonGraphValueGraph;
@protocol JavaUtilSet;

/*!
 @brief A class to allow <code>ValueGraph</code> implementations to be backed by a provided delegate.This is
  not currently planned to be released as a general-purpose forwarding class.
 @author James Sexton
 @author Joshua O'Madadhain
 */
@interface ComGoogleCommonGraphForwardingValueGraph : ComGoogleCommonGraphAbstractValueGraph

#pragma mark Public

- (id<JavaUtilSet>)adjacentNodesWithId:(id __nonnull)node;

- (jboolean)allowsSelfLoops;

- (jint)degreeWithId:(id __nonnull)node;

- (id __nullable)edgeValueOrDefaultWithId:(id __nonnull)nodeU
                                   withId:(id __nonnull)nodeV
                                   withId:(id __nullable)defaultValue;

- (jboolean)hasEdgeConnectingWithId:(id __nonnull)nodeU
                             withId:(id __nonnull)nodeV;

- (jint)inDegreeWithId:(id __nonnull)node;

- (jboolean)isDirected;

- (ComGoogleCommonGraphElementOrder *)nodeOrder;

- (id<JavaUtilSet>)nodes;

- (jint)outDegreeWithId:(id __nonnull)node;

- (id<JavaUtilSet>)predecessorsWithId:(id __nonnull)node;

- (id<JavaUtilSet>)successorsWithId:(id __nonnull)node;

#pragma mark Protected

- (id<ComGoogleCommonGraphValueGraph>)delegate;

/*!
 @brief Defer to <code>AbstractValueGraph.edges()</code> (based on <code>successors(Object)</code>) for full
  edges() implementation.
 */
- (jlong)edgeCount;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivate;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonGraphForwardingValueGraph)

FOUNDATION_EXPORT void ComGoogleCommonGraphForwardingValueGraph_initPackagePrivate(ComGoogleCommonGraphForwardingValueGraph *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonGraphForwardingValueGraph)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonGraphForwardingValueGraph")
