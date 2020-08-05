//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xpath/functions/FuncPosition.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXpathFunctionsFuncPosition")
#ifdef RESTRICT_OrgApacheXpathFunctionsFuncPosition
#define INCLUDE_ALL_OrgApacheXpathFunctionsFuncPosition 0
#else
#define INCLUDE_ALL_OrgApacheXpathFunctionsFuncPosition 1
#endif
#undef RESTRICT_OrgApacheXpathFunctionsFuncPosition

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXpathFunctionsFuncPosition_) && (INCLUDE_ALL_OrgApacheXpathFunctionsFuncPosition || defined(INCLUDE_OrgApacheXpathFunctionsFuncPosition))
#define OrgApacheXpathFunctionsFuncPosition_

#define RESTRICT_OrgApacheXpathFunctionsFunction 1
#define INCLUDE_OrgApacheXpathFunctionsFunction 1
#include "org/apache/xpath/functions/Function.h"

@class JavaUtilVector;
@class OrgApacheXpathCompilerCompiler;
@class OrgApacheXpathObjectsXObject;
@class OrgApacheXpathXPathContext;

/*!
 @brief Execute the Position() function.
 */
@interface OrgApacheXpathFunctionsFuncPosition : OrgApacheXpathFunctionsFunction
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

/*!
 @brief No arguments to process, so this does nothing.
 */
- (void)fixupVariablesWithJavaUtilVector:(JavaUtilVector *)vars
                                 withInt:(jint)globalsSize;

/*!
 @brief Get the position in the current context node list.
 @param xctxt Runtime XPath context.
 @return The current position of the itteration in the context node list, 
          or -1 if there is no active context node list.
 */
- (jint)getPositionInContextNodeListWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt;

/*!
 @brief Figure out if we're executing a toplevel expression.
 If so, we can't be inside of a predicate.
 */
- (void)postCompileStepWithOrgApacheXpathCompilerCompiler:(OrgApacheXpathCompilerCompiler *)compiler;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXpathFunctionsFuncPosition)

inline jlong OrgApacheXpathFunctionsFuncPosition_get_serialVersionUID(void);
#define OrgApacheXpathFunctionsFuncPosition_serialVersionUID -9092846348197271582LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXpathFunctionsFuncPosition, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXpathFunctionsFuncPosition_init(OrgApacheXpathFunctionsFuncPosition *self);

FOUNDATION_EXPORT OrgApacheXpathFunctionsFuncPosition *new_OrgApacheXpathFunctionsFuncPosition_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXpathFunctionsFuncPosition *create_OrgApacheXpathFunctionsFuncPosition_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXpathFunctionsFuncPosition)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXpathFunctionsFuncPosition")
