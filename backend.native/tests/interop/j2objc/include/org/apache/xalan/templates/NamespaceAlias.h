//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/NamespaceAlias.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTemplatesNamespaceAlias")
#ifdef RESTRICT_OrgApacheXalanTemplatesNamespaceAlias
#define INCLUDE_ALL_OrgApacheXalanTemplatesNamespaceAlias 0
#else
#define INCLUDE_ALL_OrgApacheXalanTemplatesNamespaceAlias 1
#endif
#undef RESTRICT_OrgApacheXalanTemplatesNamespaceAlias

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTemplatesNamespaceAlias_) && (INCLUDE_ALL_OrgApacheXalanTemplatesNamespaceAlias || defined(INCLUDE_OrgApacheXalanTemplatesNamespaceAlias))
#define OrgApacheXalanTemplatesNamespaceAlias_

#define RESTRICT_OrgApacheXalanTemplatesElemTemplateElement 1
#define INCLUDE_OrgApacheXalanTemplatesElemTemplateElement 1
#include "org/apache/xalan/templates/ElemTemplateElement.h"

@class OrgApacheXalanTemplatesStylesheetRoot;

/*!
 @brief Object to hold an xsl:namespace element.
 A stylesheet can use the xsl:namespace-alias element to declare
  that one namespace URI is an alias for another namespace URI.
 - seealso: <a href="http://www.w3.org/TR/xslt#literal-result-element">literal-result-element in XSLT Specification</a>
 */
@interface OrgApacheXalanTemplatesNamespaceAlias : OrgApacheXalanTemplatesElemTemplateElement
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

/*!
 @brief Constructor NamespaceAlias
 @param docOrderNumber The document order number
 */
- (instancetype __nonnull)initWithInt:(jint)docOrderNumber;

/*!
 @brief Get the result namespace value.
 @return non-null namespace value.
 */
- (NSString *)getResultNamespace;

/*!
 @brief Get the "result-prefix" attribute.
 @return non-null prefix value.
 */
- (NSString *)getResultPrefix;

/*!
 @brief Get the value for the stylesheet namespace.
 @return non-null prefix value.
 */
- (NSString *)getStylesheetNamespace;

/*!
 @brief Get the "stylesheet-prefix" attribute.
 @return non-null prefix value.
 */
- (NSString *)getStylesheetPrefix;

/*!
 @brief This function is called to recompose() all of the namespace alias properties elements.
 @param root The owning root stylesheet
 */
- (void)recomposeWithOrgApacheXalanTemplatesStylesheetRoot:(OrgApacheXalanTemplatesStylesheetRoot *)root;

/*!
 @brief Set the result namespace.
 @param v non-null namespace value
 */
- (void)setResultNamespaceWithNSString:(NSString *)v;

/*!
 @brief Set the "result-prefix" attribute.
 @param v non-null prefix value.
 */
- (void)setResultPrefixWithNSString:(NSString *)v;

/*!
 @brief Set the value for the stylesheet namespace.
 @param v non-null prefix value.
 */
- (void)setStylesheetNamespaceWithNSString:(NSString *)v;

/*!
 @brief Set the "stylesheet-prefix" attribute.
 @param v non-null prefix value.
 */
- (void)setStylesheetPrefixWithNSString:(NSString *)v;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesNamespaceAlias)

inline jlong OrgApacheXalanTemplatesNamespaceAlias_get_serialVersionUID(void);
#define OrgApacheXalanTemplatesNamespaceAlias_serialVersionUID 456173966637810718LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesNamespaceAlias, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesNamespaceAlias_initWithInt_(OrgApacheXalanTemplatesNamespaceAlias *self, jint docOrderNumber);

FOUNDATION_EXPORT OrgApacheXalanTemplatesNamespaceAlias *new_OrgApacheXalanTemplatesNamespaceAlias_initWithInt_(jint docOrderNumber) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesNamespaceAlias *create_OrgApacheXalanTemplatesNamespaceAlias_initWithInt_(jint docOrderNumber);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesNamespaceAlias)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTemplatesNamespaceAlias")
