//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/ElemOtherwise.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTemplatesElemOtherwise")
#ifdef RESTRICT_OrgApacheXalanTemplatesElemOtherwise
#define INCLUDE_ALL_OrgApacheXalanTemplatesElemOtherwise 0
#else
#define INCLUDE_ALL_OrgApacheXalanTemplatesElemOtherwise 1
#endif
#undef RESTRICT_OrgApacheXalanTemplatesElemOtherwise

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTemplatesElemOtherwise_) && (INCLUDE_ALL_OrgApacheXalanTemplatesElemOtherwise || defined(INCLUDE_OrgApacheXalanTemplatesElemOtherwise))
#define OrgApacheXalanTemplatesElemOtherwise_

#define RESTRICT_OrgApacheXalanTemplatesElemTemplateElement 1
#define INCLUDE_OrgApacheXalanTemplatesElemTemplateElement 1
#include "org/apache/xalan/templates/ElemTemplateElement.h"

/*!
 @brief Implement xsl:otherwise.
 @code

   <!ELEMENT xsl:otherwise %template;>
   <!ATTLIST xsl:otherwise %space-att;>
   
@endcode
 - seealso: <a href="http://www.w3.org/TR/xslt#section-Conditional-Processing-with-xsl:choose">XXX in XSLT Specification</a>
 */
@interface OrgApacheXalanTemplatesElemOtherwise : OrgApacheXalanTemplatesElemTemplateElement
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Return the node name.
 @return The element's name
 */
- (NSString *)getNodeName;

/*!
 @brief Get an int constant identifying the type of element.
 - seealso: org.apache.xalan.templates.Constants
 @return The token ID for this element
 */
- (jint)getXSLToken;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesElemOtherwise)

inline jlong OrgApacheXalanTemplatesElemOtherwise_get_serialVersionUID(void);
#define OrgApacheXalanTemplatesElemOtherwise_serialVersionUID 1863944560970181395LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesElemOtherwise, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesElemOtherwise_init(OrgApacheXalanTemplatesElemOtherwise *self);

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemOtherwise *new_OrgApacheXalanTemplatesElemOtherwise_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemOtherwise *create_OrgApacheXalanTemplatesElemOtherwise_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesElemOtherwise)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTemplatesElemOtherwise")
