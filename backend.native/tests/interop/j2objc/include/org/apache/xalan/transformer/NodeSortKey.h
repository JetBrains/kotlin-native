//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/transformer/NodeSortKey.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTransformerNodeSortKey")
#ifdef RESTRICT_OrgApacheXalanTransformerNodeSortKey
#define INCLUDE_ALL_OrgApacheXalanTransformerNodeSortKey 0
#else
#define INCLUDE_ALL_OrgApacheXalanTransformerNodeSortKey 1
#endif
#undef RESTRICT_OrgApacheXalanTransformerNodeSortKey

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTransformerNodeSortKey_) && (INCLUDE_ALL_OrgApacheXalanTransformerNodeSortKey || defined(INCLUDE_OrgApacheXalanTransformerNodeSortKey))
#define OrgApacheXalanTransformerNodeSortKey_

@class JavaTextCollator;
@class JavaUtilLocale;
@class OrgApacheXalanTransformerTransformerImpl;
@class OrgApacheXpathXPath;
@protocol OrgApacheXmlUtilsPrefixResolver;

/*!
 @brief Data structure for use by the NodeSorter class.
 */
@interface OrgApacheXalanTransformerNodeSortKey : NSObject {
 @public
  /*!
   @brief Select pattern for this sort key
   */
  OrgApacheXpathXPath *m_selectPat_;
  /*!
   @brief Flag indicating whether to treat thee result as a number
   */
  jboolean m_treatAsNumbers_;
  /*!
   @brief Flag indicating whether to sort in descending order
   */
  jboolean m_descending_;
  /*!
   @brief Flag indicating by case
   */
  jboolean m_caseOrderUpper_;
  /*!
   @brief Collator instance
   */
  JavaTextCollator *m_col_;
  /*!
   @brief Locale we're in
   */
  JavaUtilLocale *m_locale_;
  /*!
   @brief Prefix resolver to use
   */
  id<OrgApacheXmlUtilsPrefixResolver> m_namespaceContext_;
  /*!
   @brief Transformer instance
   */
  OrgApacheXalanTransformerTransformerImpl *m_processor_;
}

#pragma mark Package-Private

/*!
 @brief Constructor NodeSortKey
 @param transformer non null transformer instance
 @param selectPat Select pattern for this key
 @param treatAsNumbers Flag indicating whether the result will be a number
 @param descending Flag indicating whether to sort in descending order
 @param langValue Lang value to use to get locale
 @param caseOrderUpper Flag indicating whether case is relevant
 @param namespaceContext Prefix resolver
 @throw javax.xml.transform.TransformerException
 */
- (instancetype __nonnull)initPackagePrivateWithOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer
                                                                 withOrgApacheXpathXPath:(OrgApacheXpathXPath *)selectPat
                                                                             withBoolean:(jboolean)treatAsNumbers
                                                                             withBoolean:(jboolean)descending
                                                                            withNSString:(NSString *)langValue
                                                                             withBoolean:(jboolean)caseOrderUpper
                                                     withOrgApacheXmlUtilsPrefixResolver:(id<OrgApacheXmlUtilsPrefixResolver>)namespaceContext;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTransformerNodeSortKey)

J2OBJC_FIELD_SETTER(OrgApacheXalanTransformerNodeSortKey, m_selectPat_, OrgApacheXpathXPath *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTransformerNodeSortKey, m_col_, JavaTextCollator *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTransformerNodeSortKey, m_locale_, JavaUtilLocale *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTransformerNodeSortKey, m_namespaceContext_, id<OrgApacheXmlUtilsPrefixResolver>)
J2OBJC_FIELD_SETTER(OrgApacheXalanTransformerNodeSortKey, m_processor_, OrgApacheXalanTransformerTransformerImpl *)

FOUNDATION_EXPORT void OrgApacheXalanTransformerNodeSortKey_initPackagePrivateWithOrgApacheXalanTransformerTransformerImpl_withOrgApacheXpathXPath_withBoolean_withBoolean_withNSString_withBoolean_withOrgApacheXmlUtilsPrefixResolver_(OrgApacheXalanTransformerNodeSortKey *self, OrgApacheXalanTransformerTransformerImpl *transformer, OrgApacheXpathXPath *selectPat, jboolean treatAsNumbers, jboolean descending, NSString *langValue, jboolean caseOrderUpper, id<OrgApacheXmlUtilsPrefixResolver> namespaceContext);

FOUNDATION_EXPORT OrgApacheXalanTransformerNodeSortKey *new_OrgApacheXalanTransformerNodeSortKey_initPackagePrivateWithOrgApacheXalanTransformerTransformerImpl_withOrgApacheXpathXPath_withBoolean_withBoolean_withNSString_withBoolean_withOrgApacheXmlUtilsPrefixResolver_(OrgApacheXalanTransformerTransformerImpl *transformer, OrgApacheXpathXPath *selectPat, jboolean treatAsNumbers, jboolean descending, NSString *langValue, jboolean caseOrderUpper, id<OrgApacheXmlUtilsPrefixResolver> namespaceContext) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTransformerNodeSortKey *create_OrgApacheXalanTransformerNodeSortKey_initPackagePrivateWithOrgApacheXalanTransformerTransformerImpl_withOrgApacheXpathXPath_withBoolean_withBoolean_withNSString_withBoolean_withOrgApacheXmlUtilsPrefixResolver_(OrgApacheXalanTransformerTransformerImpl *transformer, OrgApacheXpathXPath *selectPat, jboolean treatAsNumbers, jboolean descending, NSString *langValue, jboolean caseOrderUpper, id<OrgApacheXmlUtilsPrefixResolver> namespaceContext);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTransformerNodeSortKey)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTransformerNodeSortKey")
