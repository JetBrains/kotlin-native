//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/CheckReturnValue.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoCheckReturnValue")
#ifdef RESTRICT_OrgMockitoCheckReturnValue
#define INCLUDE_ALL_OrgMockitoCheckReturnValue 0
#else
#define INCLUDE_ALL_OrgMockitoCheckReturnValue 1
#endif
#undef RESTRICT_OrgMockitoCheckReturnValue

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoCheckReturnValue_) && (INCLUDE_ALL_OrgMockitoCheckReturnValue || defined(INCLUDE_OrgMockitoCheckReturnValue))
#define OrgMockitoCheckReturnValue_

#define RESTRICT_JavaLangAnnotationAnnotation 1
#define INCLUDE_JavaLangAnnotationAnnotation 1
#include "java/lang/annotation/Annotation.h"

@class IOSClass;

/*!
 @brief This annotation is not supposed to be used by Mockito end-users.Instead, we
  use it to annotate methods for Static Analysis tools, including FindBugs and ErrorProne.
 These tools can check whether the return value of our Mockito methods are actually
  used. As such, Mockito State Validation can be performed at compile-time rather than run-time.
  This annotation is public, because we have to use it in multiple packages.
 - seealso: <a href="https://github.com/findbugsproject/findbugs/blob/264ae7baf890d2b347d91805c90057062b5dcb1e/findbugs/src/java/edu/umd/cs/findbugs/detect/BuildCheckReturnAnnotationDatabase.java#L120">Findbugs source code</a>
 - seealso: <a href="http://errorprone.info/bugpattern/CheckReturnValue">ErrorProne check</a>
 @since 2.11.4
 */
@protocol OrgMockitoCheckReturnValue < JavaLangAnnotationAnnotation >

- (jboolean)isEqual:(id)obj;

- (NSUInteger)hash;

@end

@interface OrgMockitoCheckReturnValue : NSObject < OrgMockitoCheckReturnValue >

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoCheckReturnValue)

FOUNDATION_EXPORT id<OrgMockitoCheckReturnValue> create_OrgMockitoCheckReturnValue(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoCheckReturnValue)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoCheckReturnValue")
