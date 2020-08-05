//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/processor/ProcessorTemplateElem.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanProcessorProcessorTemplateElem")
#ifdef RESTRICT_OrgApacheXalanProcessorProcessorTemplateElem
#define INCLUDE_ALL_OrgApacheXalanProcessorProcessorTemplateElem 0
#else
#define INCLUDE_ALL_OrgApacheXalanProcessorProcessorTemplateElem 1
#endif
#undef RESTRICT_OrgApacheXalanProcessorProcessorTemplateElem

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanProcessorProcessorTemplateElem_) && (INCLUDE_ALL_OrgApacheXalanProcessorProcessorTemplateElem || defined(INCLUDE_OrgApacheXalanProcessorProcessorTemplateElem))
#define OrgApacheXalanProcessorProcessorTemplateElem_

#define RESTRICT_OrgApacheXalanProcessorXSLTElementProcessor 1
#define INCLUDE_OrgApacheXalanProcessorXSLTElementProcessor 1
#include "org/apache/xalan/processor/XSLTElementProcessor.h"

@class OrgApacheXalanProcessorStylesheetHandler;
@class OrgApacheXalanTemplatesElemTemplateElement;
@protocol OrgXmlSaxAttributes;

/*!
 @brief This class processes parse events for an XSLT template element.
 - seealso: <a href="http://www.w3.org/TR/xslt#dtd">XSLT DTD</a>
 - seealso: <a href="http://www.w3.org/TR/xslt#section-Creating-the-Result-Tree">section-Creating-the-Result-Tree in XSLT Specification</a>
 */
@interface OrgApacheXalanProcessorProcessorTemplateElem : OrgApacheXalanProcessorXSLTElementProcessor
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Receive notification of the end of an element.
 @param handler non-null reference to current StylesheetHandler that is constructing the Templates.
 @param uri The Namespace URI, or an empty string.
 @param localName The local name (without prefix), or empty string if not namespace processing.
 @param rawName The qualified name (with prefix).
 */
- (void)endElementWithOrgApacheXalanProcessorStylesheetHandler:(OrgApacheXalanProcessorStylesheetHandler *)handler
                                                  withNSString:(NSString *)uri
                                                  withNSString:(NSString *)localName
                                                  withNSString:(NSString *)rawName;

/*!
 @brief Receive notification of the start of an element.
 @param handler non-null reference to current StylesheetHandler that is constructing the Templates.
 @param uri The Namespace URI, or an empty string.
 @param localName The local name (without prefix), or empty string if not namespace processing.
 @param rawName The qualified name (with prefix).
 @param attributes The specified or defaulted attributes.
 */
- (void)startElementWithOrgApacheXalanProcessorStylesheetHandler:(OrgApacheXalanProcessorStylesheetHandler *)handler
                                                    withNSString:(NSString *)uri
                                                    withNSString:(NSString *)localName
                                                    withNSString:(NSString *)rawName
                                         withOrgXmlSaxAttributes:(id<OrgXmlSaxAttributes>)attributes;

#pragma mark Protected

/*!
 @brief Append the current template element to the current
  template element, and then push it onto the current template
  element stack.
 @param handler non-null reference to current StylesheetHandler that is constructing the Templates.
 @param elem non-null reference to a the current template element.
 @throw org.xml.sax.SAXExceptionAny SAX exception, possibly
             wrapping another exception.
 */
- (void)appendAndPushWithOrgApacheXalanProcessorStylesheetHandler:(OrgApacheXalanProcessorStylesheetHandler *)handler
                   withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanProcessorProcessorTemplateElem)

inline jlong OrgApacheXalanProcessorProcessorTemplateElem_get_serialVersionUID(void);
#define OrgApacheXalanProcessorProcessorTemplateElem_serialVersionUID 8344994001943407235LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanProcessorProcessorTemplateElem, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXalanProcessorProcessorTemplateElem_init(OrgApacheXalanProcessorProcessorTemplateElem *self);

FOUNDATION_EXPORT OrgApacheXalanProcessorProcessorTemplateElem *new_OrgApacheXalanProcessorProcessorTemplateElem_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorProcessorTemplateElem *create_OrgApacheXalanProcessorProcessorTemplateElem_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanProcessorProcessorTemplateElem)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanProcessorProcessorTemplateElem")
