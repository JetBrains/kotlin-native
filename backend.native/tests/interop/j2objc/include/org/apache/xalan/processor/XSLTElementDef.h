//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/processor/XSLTElementDef.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanProcessorXSLTElementDef")
#ifdef RESTRICT_OrgApacheXalanProcessorXSLTElementDef
#define INCLUDE_ALL_OrgApacheXalanProcessorXSLTElementDef 0
#else
#define INCLUDE_ALL_OrgApacheXalanProcessorXSLTElementDef 1
#endif
#undef RESTRICT_OrgApacheXalanProcessorXSLTElementDef

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanProcessorXSLTElementDef_) && (INCLUDE_ALL_OrgApacheXalanProcessorXSLTElementDef || defined(INCLUDE_OrgApacheXalanProcessorXSLTElementDef))
#define OrgApacheXalanProcessorXSLTElementDef_

@class IOSClass;
@class IOSObjectArray;
@class JavaUtilHashtable;
@class OrgApacheXalanProcessorXSLTAttributeDef;
@class OrgApacheXalanProcessorXSLTElementProcessor;
@class OrgApacheXalanProcessorXSLTSchema;

/*!
 @brief This class defines the allowed structure for an element in a XSLT stylesheet,
  is meant to reflect the structure defined in http://www.w3.org/TR/xslt#dtd, and the
  mapping between Xalan classes and the markup elements in the XSLT instance.
 This actually represents both text nodes and elements.
 */
@interface OrgApacheXalanProcessorXSLTElementDef : NSObject {
 @public
  JavaUtilHashtable *m_requiredFound_;
  jboolean m_isOrdered_;
}
@property (readonly, class) jint T_ELEMENT NS_SWIFT_NAME(T_ELEMENT);
@property (readonly, class) jint T_PCDATA NS_SWIFT_NAME(T_PCDATA);
@property (readonly, class) jint T_ANY NS_SWIFT_NAME(T_ANY);

+ (jint)T_ELEMENT;

+ (jint)T_PCDATA;

+ (jint)T_ANY;

#pragma mark Public

/*!
 @brief Return the XSLTElementProcessor for this element.
 @return The element processor for this element.
 */
- (OrgApacheXalanProcessorXSLTElementProcessor *)getElementProcessor;

/*!
 @brief Get the allowed elements for this type.
 @return An array of allowed child element defs, or null.
 */
- (IOSObjectArray *)getElements;

/*!
 @brief Set the XSLTElementProcessor for this element.
 @param handler The element processor for this element.
 */
- (void)setElementProcessorWithOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)handler;

#pragma mark Package-Private

/*!
 @brief Construct an instance of XSLTElementDef.This must be followed by a
  call to build().
 */
- (instancetype __nonnull)init;

/*!
 @brief Construct an instance of XSLTElementDef that represents text.
 @param classObject The class of the object that this element def should produce.
 @param contentHandler The element processor for this element.
 @param type Content type, one of T_ELEMENT, T_PCDATA, or T_ANY.
 */
- (instancetype __nonnull)initWithIOSClass:(IOSClass *)classObject
withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                   withInt:(jint)type;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param has_required true if this element has required elements by the XSLT specification.
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                        withBoolean:(jboolean)has_required;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param has_required true if this element has required elements by the XSLT specification.
 @param required true if this element is required by the XSLT specification.
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                        withBoolean:(jboolean)has_required
                                                        withBoolean:(jboolean)required;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param has_required true if this element has required elements by the XSLT specification.
 @param required true if this element is required by the XSLT specification.
 @param has_order whether this element has ordered child elements
 @param order the order this element should appear according to the XSLT specification.
 @param multiAllowed whether this element is allowed more than once
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                        withBoolean:(jboolean)has_required
                                                        withBoolean:(jboolean)required
                                                        withBoolean:(jboolean)has_order
                                                            withInt:(jint)order
                                                        withBoolean:(jboolean)multiAllowed;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param has_required true if this element has required elements by the XSLT specification.
 @param required true if this element is required by the XSLT specification.
 @param order the order this element should appear according to the XSLT specification.
 @param multiAllowed whether this element is allowed more than once
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                        withBoolean:(jboolean)has_required
                                                        withBoolean:(jboolean)required
                                                            withInt:(jint)order
                                                        withBoolean:(jboolean)multiAllowed;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param has_order whether this element has ordered child elements
 @param order the order this element should appear according to the XSLT specification.
 @param multiAllowed whether this element is allowed more than once
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                        withBoolean:(jboolean)has_order
                                                            withInt:(jint)order
                                                        withBoolean:(jboolean)multiAllowed;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 @param order the order this element should appear according to the XSLT specification.
 @param multiAllowed whether this element is allowed more than once
 */
- (instancetype __nonnull)initWithOrgApacheXalanProcessorXSLTSchema:(OrgApacheXalanProcessorXSLTSchema *)schema
                                                       withNSString:(NSString *)namespace_
                                                       withNSString:(NSString *)name
                                                       withNSString:(NSString *)nameAlias
                     withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
                   withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
                    withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
                                                       withIOSClass:(IOSClass *)classObject
                                                            withInt:(jint)order
                                                        withBoolean:(jboolean)multiAllowed;

/*!
 @brief Construct an instance of XSLTElementDef.
 @param namespace_ The Namespace URI, "*", or null.
 @param name The local name (without prefix), "*", or null.
 @param nameAlias A potential alias for the name, or null.
 @param elements An array of allowed child element defs, or null.
 @param attributes An array of allowed attribute defs, or null.
 @param contentHandler The element processor for this element.
 @param classObject The class of the object that this element def should produce.
 */
- (void)buildWithNSString:(NSString *)namespace_
             withNSString:(NSString *)name
             withNSString:(NSString *)nameAlias
withOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)elements
withOrgApacheXalanProcessorXSLTAttributeDefArray:(IOSObjectArray *)attributes
withOrgApacheXalanProcessorXSLTElementProcessor:(OrgApacheXalanProcessorXSLTElementProcessor *)contentHandler
             withIOSClass:(IOSClass *)classObject;

/*!
 @brief Given a namespace URI, and a local name, return the element's
  attribute definition, if it has one.
 @param uri The Namespace URI, or an empty string.
 @param localName The local name (without prefix), or empty string if not namespace processing.
 @return The attribute def that matches the arguments, or null.
 */
- (OrgApacheXalanProcessorXSLTAttributeDef *)getAttributeDefWithNSString:(NSString *)uri
                                                            withNSString:(NSString *)localName;

/*!
 @brief Get the allowed attributes for this type.
 @return An array of allowed attribute defs, or null.
 */
- (IOSObjectArray *)getAttributes;

/*!
 @brief Return the class object that should in instantiated for
  a Xalan instance of this element.
 @return The class of the object that this element def should produce, or null.
 */
- (IOSClass *)getClassObject;

/*!
 @brief Get the highest order of child elements have appeared so far .
 @return the highest order of child elements have appeared so far.
 */
- (jint)getLastOrder;

/*!
 @brief Get whether this element can appear multiple times
 @return true if this element can appear multiple times
 */
- (jboolean)getMultiAllowed;

/*!
 @brief Get the local name of this element.
 @return The local name of this element, "*", or null.
 */
- (NSString *)getName;

/*!
 @brief Get the name of this element.
 @return A potential alias for the name, or null.
 */
- (NSString *)getNameAlias;

/*!
 @brief Get the allowed namespace for this element.
 @return The Namespace URI, "*", or null.
 */
- (NSString *)getNamespace;

/*!
 @brief Get the order that this element should appear .
 @return the order that this element should appear.
 */
- (jint)getOrder;

/*!
 @brief Given a namespace URI, and a local name, get the processor
  for the element, or return null if not allowed.
 @param uri The Namespace URI, or an empty string.
 @param localName The local name (without prefix), or empty string if not namespace processing.
 @return The element processor that matches the arguments, or null.
 */
- (OrgApacheXalanProcessorXSLTElementProcessor *)getProcessorForWithNSString:(NSString *)uri
                                                                withNSString:(NSString *)localName;

/*!
 @brief Given an unknown element, get the processor
  for the element.
 @param uri The Namespace URI, or an empty string.
 @param localName The local name (without prefix), or empty string if not namespace processing.
 @return normally a <code>ProcessorUnknown</code> reference.
 - seealso: ProcessorUnknown
 */
- (OrgApacheXalanProcessorXSLTElementProcessor *)getProcessorForUnknownWithNSString:(NSString *)uri
                                                                       withNSString:(NSString *)localName;

/*!
 @brief Get whether or not this is a required element.
 @return true if this is a required element.
 */
- (jboolean)getRequired;

/*!
 @brief Get required elements that were not found.
 @return required elements that were not found.
 */
- (NSString *)getRequiredElem;

/*!
 @brief Get whether all required elements were found.
 @return true if all required elements were found.
 */
- (jboolean)getRequiredFound;

/*!
 @brief Get the type of this element.
 @return Content type, one of T_ELEMENT, T_PCDATA, or T_ANY.
 */
- (jint)getType;

/*!
 @brief Get whether or not this has a required element.
 @return true if this this has a required element.
 */
- (jboolean)hasRequired;

/*!
 @brief Get whether this element requires ordered children.
 @return true if this element requires ordered children.
 */
- (jboolean)isOrdered;

/*!
 @brief Set the allowed elements for this type.
 @param defs An array of allowed child element defs, or null.
 */
- (void)setElementsWithOrgApacheXalanProcessorXSLTElementDefArray:(IOSObjectArray *)defs;

/*!
 @brief Set the highest order of child elements have appeared so far .
 @param order the highest order of child elements have appeared so far.
 */
- (void)setLastOrderWithInt:(jint)order;

/*!
 @brief Set this required element found.
 */
- (void)setRequiredFoundWithNSString:(NSString *)elem
                         withBoolean:(jboolean)found;

/*!
 @brief Set the type of this element.
 @param t Content type, one of T_ELEMENT, T_PCDATA, or T_ANY.
 */
- (void)setTypeWithInt:(jint)t;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanProcessorXSLTElementDef)

J2OBJC_FIELD_SETTER(OrgApacheXalanProcessorXSLTElementDef, m_requiredFound_, JavaUtilHashtable *)

/*!
 @brief Content type enumerations
 */
inline jint OrgApacheXalanProcessorXSLTElementDef_get_T_ELEMENT(void);
#define OrgApacheXalanProcessorXSLTElementDef_T_ELEMENT 1
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanProcessorXSLTElementDef, T_ELEMENT, jint)

/*!
 @brief Content type enumerations
 */
inline jint OrgApacheXalanProcessorXSLTElementDef_get_T_PCDATA(void);
#define OrgApacheXalanProcessorXSLTElementDef_T_PCDATA 2
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanProcessorXSLTElementDef, T_PCDATA, jint)

/*!
 @brief Content type enumerations
 */
inline jint OrgApacheXalanProcessorXSLTElementDef_get_T_ANY(void);
#define OrgApacheXalanProcessorXSLTElementDef_T_ANY 3
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanProcessorXSLTElementDef, T_ANY, jint)

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_init(OrgApacheXalanProcessorXSLTElementDef *self);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_init(void);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jint order, jboolean multiAllowed) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jboolean has_order, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jboolean has_order, jint order, jboolean multiAllowed) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withBoolean_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_required, jboolean required, jboolean has_order, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_order, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_order, jint order, jboolean multiAllowed) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withBoolean_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jboolean has_order, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withInt_withBoolean_(OrgApacheXalanProcessorXSLTElementDef *self, OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jint order, jboolean multiAllowed) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithOrgApacheXalanProcessorXSLTSchema_withNSString_withNSString_withNSString_withOrgApacheXalanProcessorXSLTElementDefArray_withOrgApacheXalanProcessorXSLTAttributeDefArray_withOrgApacheXalanProcessorXSLTElementProcessor_withIOSClass_withInt_withBoolean_(OrgApacheXalanProcessorXSLTSchema *schema, NSString *namespace_, NSString *name, NSString *nameAlias, IOSObjectArray *elements, IOSObjectArray *attributes, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, IOSClass *classObject, jint order, jboolean multiAllowed);

FOUNDATION_EXPORT void OrgApacheXalanProcessorXSLTElementDef_initWithIOSClass_withOrgApacheXalanProcessorXSLTElementProcessor_withInt_(OrgApacheXalanProcessorXSLTElementDef *self, IOSClass *classObject, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, jint type);

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *new_OrgApacheXalanProcessorXSLTElementDef_initWithIOSClass_withOrgApacheXalanProcessorXSLTElementProcessor_withInt_(IOSClass *classObject, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, jint type) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanProcessorXSLTElementDef *create_OrgApacheXalanProcessorXSLTElementDef_initWithIOSClass_withOrgApacheXalanProcessorXSLTElementProcessor_withInt_(IOSClass *classObject, OrgApacheXalanProcessorXSLTElementProcessor *contentHandler, jint type);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanProcessorXSLTElementDef)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanProcessorXSLTElementDef")
