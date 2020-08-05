//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xpath/functions/FuncBoolean.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXpathFunctionsFuncBoolean")
#ifdef RESTRICT_OrgApacheXpathFunctionsFuncBoolean
#define INCLUDE_ALL_OrgApacheXpathFunctionsFuncBoolean 0
#else
#define INCLUDE_ALL_OrgApacheXpathFunctionsFuncBoolean 1
#endif
#undef RESTRICT_OrgApacheXpathFunctionsFuncBoolean

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXpathFunctionsFuncBoolean_) && (INCLUDE_ALL_OrgApacheXpathFunctionsFuncBoolean || defined(INCLUDE_OrgApacheXpathFunctionsFuncBoolean))
#define OrgApacheXpathFunctionsFuncBoolean_

#define RESTRICT_OrgApacheXpathFunctionsFunctionOneArg 1
#define INCLUDE_OrgApacheXpathFunctionsFunctionOneArg 1
#include "org/apache/xpath/functions/FunctionOneArg.h"

@class OrgApacheXpathObjectsXObject;
@class OrgApacheXpathXPathContext;

/*!
 @brief Execute the Boolean() function.
 */
@interface OrgApacheXpathFunctionsFuncBoolean : OrgApacheXpathFunctionsFunctionOneArg
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Execute the function.The function must return
  a valid object.
 @param xctxt The current execution context.
 @return A valid XObject.
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXpathObjectsXObject *)executeWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXpathFunctionsFuncBoolean)

inline jlong OrgApacheXpathFunctionsFuncBoolean_get_serialVersionUID(void);
#define OrgApacheXpathFunctionsFuncBoolean_serialVersionUID 4328660760070034592LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXpathFunctionsFuncBoolean, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXpathFunctionsFuncBoolean_init(OrgApacheXpathFunctionsFuncBoolean *self);

FOUNDATION_EXPORT OrgApacheXpathFunctionsFuncBoolean *new_OrgApacheXpathFunctionsFuncBoolean_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXpathFunctionsFuncBoolean *create_OrgApacheXpathFunctionsFuncBoolean_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXpathFunctionsFuncBoolean)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXpathFunctionsFuncBoolean")
