//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xpath/FoundIndex.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXpathFoundIndex")
#ifdef RESTRICT_OrgApacheXpathFoundIndex
#define INCLUDE_ALL_OrgApacheXpathFoundIndex 0
#else
#define INCLUDE_ALL_OrgApacheXpathFoundIndex 1
#endif
#undef RESTRICT_OrgApacheXpathFoundIndex

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXpathFoundIndex_) && (INCLUDE_ALL_OrgApacheXpathFoundIndex || defined(INCLUDE_OrgApacheXpathFoundIndex))
#define OrgApacheXpathFoundIndex_

#define RESTRICT_JavaLangRuntimeException 1
#define INCLUDE_JavaLangRuntimeException 1
#include "java/lang/RuntimeException.h"

@class JavaLangThrowable;

/*!
 @brief Class to let us know when it's time to do
  a search from the parent because of indexing.
 */
@interface OrgApacheXpathFoundIndex : JavaLangRuntimeException
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

/*!
 @brief Constructor FoundIndex
 */
- (instancetype __nonnull)init;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initWithJavaLangThrowable:(JavaLangThrowable *)arg0 NS_UNAVAILABLE;

- (instancetype __nonnull)initWithNSString:(NSString *)arg0 NS_UNAVAILABLE;

- (instancetype __nonnull)initWithNSString:(NSString *)arg0
                     withJavaLangThrowable:(JavaLangThrowable *)arg1 NS_UNAVAILABLE;

- (instancetype __nonnull)initWithNSString:(NSString *)arg0
                     withJavaLangThrowable:(JavaLangThrowable *)arg1
                               withBoolean:(jboolean)arg2
                               withBoolean:(jboolean)arg3 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXpathFoundIndex)

inline jlong OrgApacheXpathFoundIndex_get_serialVersionUID(void);
#define OrgApacheXpathFoundIndex_serialVersionUID -4643975335243078270LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXpathFoundIndex, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXpathFoundIndex_init(OrgApacheXpathFoundIndex *self);

FOUNDATION_EXPORT OrgApacheXpathFoundIndex *new_OrgApacheXpathFoundIndex_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXpathFoundIndex *create_OrgApacheXpathFoundIndex_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXpathFoundIndex)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXpathFoundIndex")
