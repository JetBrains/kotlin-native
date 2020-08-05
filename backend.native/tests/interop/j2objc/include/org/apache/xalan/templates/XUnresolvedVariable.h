//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/XUnresolvedVariable.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTemplatesXUnresolvedVariable")
#ifdef RESTRICT_OrgApacheXalanTemplatesXUnresolvedVariable
#define INCLUDE_ALL_OrgApacheXalanTemplatesXUnresolvedVariable 0
#else
#define INCLUDE_ALL_OrgApacheXalanTemplatesXUnresolvedVariable 1
#endif
#undef RESTRICT_OrgApacheXalanTemplatesXUnresolvedVariable

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTemplatesXUnresolvedVariable_) && (INCLUDE_ALL_OrgApacheXalanTemplatesXUnresolvedVariable || defined(INCLUDE_OrgApacheXalanTemplatesXUnresolvedVariable))
#define OrgApacheXalanTemplatesXUnresolvedVariable_

#define RESTRICT_OrgApacheXpathObjectsXObject 1
#define INCLUDE_OrgApacheXpathObjectsXObject 1
#include "org/apache/xpath/objects/XObject.h"

@class OrgApacheXalanTemplatesElemVariable;
@class OrgApacheXalanTransformerTransformerImpl;
@class OrgApacheXpathXPathContext;

/*!
 @brief An instance of this class holds unto a variable until 
  it is executed.It is used at this time for global 
  variables which must (we think) forward reference.
 */
@interface OrgApacheXalanTemplatesXUnresolvedVariable : OrgApacheXpathObjectsXObject
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

/*!
 @brief Create an XUnresolvedVariable, that may be executed at a later time.
 This is primarily used so that forward referencing works with 
  global variables.  An XUnresolvedVariable is initially pushed 
  into the global variable stack, and then replaced with the real 
  thing when it is accessed.
 @param obj Must be a non-null reference to an ElemVariable.
 @param sourceNode The node context for execution.
 @param transformer The transformer execution context.
 @param varStackPos An index to the point in the variable stack where we should  begin variable searches for evaluation of expressions.
 @param varStackContext An index into the variable stack where the variable context   ends, i.e. at the point we should terminate the search.
 @param isGlobal true if this is a global variable.
 */
- (instancetype __nonnull)initWithOrgApacheXalanTemplatesElemVariable:(OrgApacheXalanTemplatesElemVariable *)obj
                                                              withInt:(jint)sourceNode
                         withOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer
                                                              withInt:(jint)varStackPos
                                                              withInt:(jint)varStackContext
                                                          withBoolean:(jboolean)isGlobal;

/*!
 @brief For support of literal objects in xpaths.
 @param xctxt The XPath execution context.
 @return This object.
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXpathObjectsXObject *)executeWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt;

/*!
 @brief Tell what kind of class this is.
 @return CLASS_UNRESOLVEDVARIABLE
 */
- (jint)getType;

/*!
 @brief Given a request type, return the equivalent string.
 For diagnostic purposes.
 @return An informational string.
 */
- (NSString *)getTypeString;

/*!
 @brief Set an index into the variable stack where the variable context 
  ends, i.e.at the point we should terminate the search.
 @param bottom The point at which the search should terminate, normally   zero for global variables.
 */
- (void)setVarStackContextWithInt:(jint)bottom;

/*!
 @brief Set an index to the point in the variable stack where we should
  begin variable searches for evaluation of expressions.
 This is -1 if m_isTopLevel is false.
 @param top A valid value that specifies where in the variable   stack the search should begin.
 */
- (void)setVarStackPosWithInt:(jint)top;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

- (instancetype __nonnull)initWithId:(id)arg0 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesXUnresolvedVariable)

inline jlong OrgApacheXalanTemplatesXUnresolvedVariable_get_serialVersionUID(void);
#define OrgApacheXalanTemplatesXUnresolvedVariable_serialVersionUID -256779804767950188LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesXUnresolvedVariable, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesXUnresolvedVariable_initWithOrgApacheXalanTemplatesElemVariable_withInt_withOrgApacheXalanTransformerTransformerImpl_withInt_withInt_withBoolean_(OrgApacheXalanTemplatesXUnresolvedVariable *self, OrgApacheXalanTemplatesElemVariable *obj, jint sourceNode, OrgApacheXalanTransformerTransformerImpl *transformer, jint varStackPos, jint varStackContext, jboolean isGlobal);

FOUNDATION_EXPORT OrgApacheXalanTemplatesXUnresolvedVariable *new_OrgApacheXalanTemplatesXUnresolvedVariable_initWithOrgApacheXalanTemplatesElemVariable_withInt_withOrgApacheXalanTransformerTransformerImpl_withInt_withInt_withBoolean_(OrgApacheXalanTemplatesElemVariable *obj, jint sourceNode, OrgApacheXalanTransformerTransformerImpl *transformer, jint varStackPos, jint varStackContext, jboolean isGlobal) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesXUnresolvedVariable *create_OrgApacheXalanTemplatesXUnresolvedVariable_initWithOrgApacheXalanTemplatesElemVariable_withInt_withOrgApacheXalanTransformerTransformerImpl_withInt_withInt_withBoolean_(OrgApacheXalanTemplatesElemVariable *obj, jint sourceNode, OrgApacheXalanTransformerTransformerImpl *transformer, jint varStackPos, jint varStackContext, jboolean isGlobal);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesXUnresolvedVariable)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTemplatesXUnresolvedVariable")
