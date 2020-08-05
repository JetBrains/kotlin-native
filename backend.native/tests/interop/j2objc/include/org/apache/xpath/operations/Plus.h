//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xpath/operations/Plus.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXpathOperationsPlus")
#ifdef RESTRICT_OrgApacheXpathOperationsPlus
#define INCLUDE_ALL_OrgApacheXpathOperationsPlus 0
#else
#define INCLUDE_ALL_OrgApacheXpathOperationsPlus 1
#endif
#undef RESTRICT_OrgApacheXpathOperationsPlus

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXpathOperationsPlus_) && (INCLUDE_ALL_OrgApacheXpathOperationsPlus || defined(INCLUDE_OrgApacheXpathOperationsPlus))
#define OrgApacheXpathOperationsPlus_

#define RESTRICT_OrgApacheXpathOperationsOperation 1
#define INCLUDE_OrgApacheXpathOperationsOperation 1
#include "org/apache/xpath/operations/Operation.h"

@class OrgApacheXpathObjectsXObject;
@class OrgApacheXpathXPathContext;

/*!
 @brief The '+' operation expression executer.
 */
@interface OrgApacheXpathOperationsPlus : OrgApacheXpathOperationsOperation
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Evaluate this operation directly to a double.
 @param xctxt The runtime execution context.
 @return The result of the operation as a double.
 @throw javax.xml.transform.TransformerException
 */
- (jdouble)numWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt;

/*!
 @brief Apply the operation to two operands, and return the result.
 @param left non-null reference to the evaluated left operand.
 @param right non-null reference to the evaluated right operand.
 @return non-null reference to the XObject that represents the result of the operation.
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXpathObjectsXObject *)operateWithOrgApacheXpathObjectsXObject:(OrgApacheXpathObjectsXObject *)left
                                         withOrgApacheXpathObjectsXObject:(OrgApacheXpathObjectsXObject *)right;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXpathOperationsPlus)

inline jlong OrgApacheXpathOperationsPlus_get_serialVersionUID(void);
#define OrgApacheXpathOperationsPlus_serialVersionUID -4492072861616504256LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXpathOperationsPlus, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXpathOperationsPlus_init(OrgApacheXpathOperationsPlus *self);

FOUNDATION_EXPORT OrgApacheXpathOperationsPlus *new_OrgApacheXpathOperationsPlus_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXpathOperationsPlus *create_OrgApacheXpathOperationsPlus_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXpathOperationsPlus)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXpathOperationsPlus")
