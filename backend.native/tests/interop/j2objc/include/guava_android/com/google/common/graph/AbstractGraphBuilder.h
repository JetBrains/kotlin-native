//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/graph/AbstractGraphBuilder.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractGraphBuilder")
#ifdef RESTRICT_ComGoogleCommonGraphAbstractGraphBuilder
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractGraphBuilder 0
#else
#define INCLUDE_ALL_ComGoogleCommonGraphAbstractGraphBuilder 1
#endif
#undef RESTRICT_ComGoogleCommonGraphAbstractGraphBuilder

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonGraphAbstractGraphBuilder_) && (INCLUDE_ALL_ComGoogleCommonGraphAbstractGraphBuilder || defined(INCLUDE_ComGoogleCommonGraphAbstractGraphBuilder))
#define ComGoogleCommonGraphAbstractGraphBuilder_

@class ComGoogleCommonBaseOptional;
@class ComGoogleCommonGraphElementOrder;

/*!
 @brief A base class for builders that construct graphs with user-defined properties.
 @author James Sexton
 */
@interface ComGoogleCommonGraphAbstractGraphBuilder : NSObject {
 @public
  jboolean directed_;
  jboolean allowsSelfLoops_;
  ComGoogleCommonGraphElementOrder *nodeOrder_;
  ComGoogleCommonBaseOptional *expectedNodeCount_;
}

#pragma mark Package-Private

/*!
 @brief Creates a new instance with the specified edge directionality.
 @param directed if true, creates an instance for graphs whose edges are each directed; if      false, creates an instance for graphs whose edges are each undirected.
 */
- (instancetype __nonnull)initPackagePrivateWithBoolean:(jboolean)directed;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonGraphAbstractGraphBuilder)

J2OBJC_FIELD_SETTER(ComGoogleCommonGraphAbstractGraphBuilder, nodeOrder_, ComGoogleCommonGraphElementOrder *)
J2OBJC_FIELD_SETTER(ComGoogleCommonGraphAbstractGraphBuilder, expectedNodeCount_, ComGoogleCommonBaseOptional *)

FOUNDATION_EXPORT void ComGoogleCommonGraphAbstractGraphBuilder_initPackagePrivateWithBoolean_(ComGoogleCommonGraphAbstractGraphBuilder *self, jboolean directed);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonGraphAbstractGraphBuilder)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonGraphAbstractGraphBuilder")
