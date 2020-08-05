//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/transformer/KeyManager.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTransformerKeyManager")
#ifdef RESTRICT_OrgApacheXalanTransformerKeyManager
#define INCLUDE_ALL_OrgApacheXalanTransformerKeyManager 0
#else
#define INCLUDE_ALL_OrgApacheXalanTransformerKeyManager 1
#endif
#undef RESTRICT_OrgApacheXalanTransformerKeyManager

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTransformerKeyManager_) && (INCLUDE_ALL_OrgApacheXalanTransformerKeyManager || defined(INCLUDE_OrgApacheXalanTransformerKeyManager))
#define OrgApacheXalanTransformerKeyManager_

@class OrgApacheXmlUtilsQName;
@class OrgApacheXpathObjectsXNodeSet;
@class OrgApacheXpathXPathContext;
@protocol OrgApacheXmlUtilsPrefixResolver;
@protocol OrgApacheXmlUtilsXMLString;

/*!
 @brief This class manages the key tables.
 */
@interface OrgApacheXalanTransformerKeyManager : NSObject

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Given a valid element key, return the corresponding node list.
 @param xctxt The XPath runtime state
 @param doc The document node
 @param name The key element name
 @param ref The key value we're looking for
 @param nscontext The prefix resolver for the execution context
 @return A nodelist of nodes mathing the given key
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXpathObjectsXNodeSet *)getNodeSetDTMByKeyWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt
                                                                            withInt:(jint)doc
                                                         withOrgApacheXmlUtilsQName:(OrgApacheXmlUtilsQName *)name
                                                     withOrgApacheXmlUtilsXMLString:(id<OrgApacheXmlUtilsXMLString>)ref
                                                withOrgApacheXmlUtilsPrefixResolver:(id<OrgApacheXmlUtilsPrefixResolver>)nscontext;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTransformerKeyManager)

FOUNDATION_EXPORT void OrgApacheXalanTransformerKeyManager_init(OrgApacheXalanTransformerKeyManager *self);

FOUNDATION_EXPORT OrgApacheXalanTransformerKeyManager *new_OrgApacheXalanTransformerKeyManager_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTransformerKeyManager *create_OrgApacheXalanTransformerKeyManager_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTransformerKeyManager)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTransformerKeyManager")
