//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/graph/ImmutableGraph.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonGraphImmutableGraph")
#ifdef RESTRICT_ComGoogleCommonGraphImmutableGraph
#define INCLUDE_ALL_ComGoogleCommonGraphImmutableGraph 0
#else
#define INCLUDE_ALL_ComGoogleCommonGraphImmutableGraph 1
#endif
#undef RESTRICT_ComGoogleCommonGraphImmutableGraph

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonGraphImmutableGraph_) && (INCLUDE_ALL_ComGoogleCommonGraphImmutableGraph || defined(INCLUDE_ComGoogleCommonGraphImmutableGraph))
#define ComGoogleCommonGraphImmutableGraph_

#define RESTRICT_ComGoogleCommonGraphForwardingGraph 1
#define INCLUDE_ComGoogleCommonGraphForwardingGraph 1
#include "com/google/common/graph/ForwardingGraph.h"

@protocol ComGoogleCommonGraphBaseGraph;
@protocol ComGoogleCommonGraphGraph;

/*!
 @brief A <code>Graph</code> whose elements and structural relationships will never change.Instances of this
  class may be obtained with <code>copyOf(Graph)</code>.
 <p>See the Guava User's Guide's <a href="https://github.com/google/guava/wiki/GraphsExplained#immutable-implementations">
 discussion
  of the <code>Immutable*</code> types</a> for more information on the properties and guarantees
  provided by this class.
 @author James Sexton
 @author Joshua O'Madadhain
 @author Omar Darwish
 @since 20.0
 */
@interface ComGoogleCommonGraphImmutableGraph : ComGoogleCommonGraphForwardingGraph

#pragma mark Public

/*!
 @brief Returns an immutable copy of <code>graph</code>.
 */
+ (ComGoogleCommonGraphImmutableGraph *)copyOfWithComGoogleCommonGraphGraph:(id<ComGoogleCommonGraphGraph>)graph OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Simply returns its argument.
 */
+ (ComGoogleCommonGraphImmutableGraph *)copyOfWithComGoogleCommonGraphImmutableGraph:(ComGoogleCommonGraphImmutableGraph *)graph OBJC_METHOD_FAMILY_NONE __attribute__((deprecated));

#pragma mark Protected

- (id<ComGoogleCommonGraphBaseGraph>)delegate;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonGraphBaseGraph:(id<ComGoogleCommonGraphBaseGraph>)backingGraph;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivate NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonGraphImmutableGraph)

FOUNDATION_EXPORT void ComGoogleCommonGraphImmutableGraph_initWithComGoogleCommonGraphBaseGraph_(ComGoogleCommonGraphImmutableGraph *self, id<ComGoogleCommonGraphBaseGraph> backingGraph);

FOUNDATION_EXPORT ComGoogleCommonGraphImmutableGraph *new_ComGoogleCommonGraphImmutableGraph_initWithComGoogleCommonGraphBaseGraph_(id<ComGoogleCommonGraphBaseGraph> backingGraph) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonGraphImmutableGraph *create_ComGoogleCommonGraphImmutableGraph_initWithComGoogleCommonGraphBaseGraph_(id<ComGoogleCommonGraphBaseGraph> backingGraph);

FOUNDATION_EXPORT ComGoogleCommonGraphImmutableGraph *ComGoogleCommonGraphImmutableGraph_copyOfWithComGoogleCommonGraphGraph_(id<ComGoogleCommonGraphGraph> graph);

FOUNDATION_EXPORT ComGoogleCommonGraphImmutableGraph *ComGoogleCommonGraphImmutableGraph_copyOfWithComGoogleCommonGraphImmutableGraph_(ComGoogleCommonGraphImmutableGraph *graph);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonGraphImmutableGraph)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonGraphImmutableGraph")
