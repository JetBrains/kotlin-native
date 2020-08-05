//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/org/checkerframework/framework/qual/ImplicitFor.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgCheckerframeworkFrameworkQualImplicitFor")
#ifdef RESTRICT_OrgCheckerframeworkFrameworkQualImplicitFor
#define INCLUDE_ALL_OrgCheckerframeworkFrameworkQualImplicitFor 0
#else
#define INCLUDE_ALL_OrgCheckerframeworkFrameworkQualImplicitFor 1
#endif
#undef RESTRICT_OrgCheckerframeworkFrameworkQualImplicitFor

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgCheckerframeworkFrameworkQualImplicitFor_) && (INCLUDE_ALL_OrgCheckerframeworkFrameworkQualImplicitFor || defined(INCLUDE_OrgCheckerframeworkFrameworkQualImplicitFor))
#define OrgCheckerframeworkFrameworkQualImplicitFor_

#define RESTRICT_JavaLangAnnotationAnnotation 1
#define INCLUDE_JavaLangAnnotationAnnotation 1
#include "java/lang/annotation/Annotation.h"

@class IOSClass;
@class IOSObjectArray;

/*!
 @brief A meta-annotation that specifies the trees and types for which the framework
  should automatically add that qualifier.These types and trees can be
  specified via any combination of six attributes.
 <p>
  For example, the <code>Nullable</code> annotation is annotated
  with 
 @code

      &#064;ImplicitFor(trees={Tree.Kind.NULL_LITERAL})
   
@endcode
  to denote that
  the framework should automatically apply <code>Nullable</code> to all instances
  of "null."
 */
@protocol OrgCheckerframeworkFrameworkQualImplicitFor < JavaLangAnnotationAnnotation >

@property (readonly) IOSObjectArray *literals;
@property (readonly) IOSObjectArray *types;
@property (readonly) IOSObjectArray *typeNames;
@property (readonly) IOSObjectArray *stringPatterns;

- (jboolean)isEqual:(id)obj;

- (NSUInteger)hash;

@end

@interface OrgCheckerframeworkFrameworkQualImplicitFor : NSObject < OrgCheckerframeworkFrameworkQualImplicitFor > {
 @public
  IOSObjectArray *literals_;
  IOSObjectArray *types_;
  IOSObjectArray *typeNames_;
  IOSObjectArray *stringPatterns_;
}

@end

J2OBJC_EMPTY_STATIC_INIT(OrgCheckerframeworkFrameworkQualImplicitFor)

FOUNDATION_EXPORT id<OrgCheckerframeworkFrameworkQualImplicitFor> create_OrgCheckerframeworkFrameworkQualImplicitFor(IOSObjectArray *literals, IOSObjectArray *stringPatterns, IOSObjectArray *typeNames, IOSObjectArray *types);

J2OBJC_TYPE_LITERAL_HEADER(OrgCheckerframeworkFrameworkQualImplicitFor)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgCheckerframeworkFrameworkQualImplicitFor")
