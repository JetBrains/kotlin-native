//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/RedundentExprEliminator.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator")
#ifdef RESTRICT_OrgApacheXalanTemplatesRedundentExprEliminator
#define INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator 0
#else
#define INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator 1
#endif
#undef RESTRICT_OrgApacheXalanTemplatesRedundentExprEliminator

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTemplatesRedundentExprEliminator_) && (INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator || defined(INCLUDE_OrgApacheXalanTemplatesRedundentExprEliminator))
#define OrgApacheXalanTemplatesRedundentExprEliminator_

#define RESTRICT_OrgApacheXalanTemplatesXSLTVisitor 1
#define INCLUDE_OrgApacheXalanTemplatesXSLTVisitor 1
#include "org/apache/xalan/templates/XSLTVisitor.h"

@class JavaUtilVector;
@class OrgApacheXalanTemplatesAbsPathChecker;
@class OrgApacheXalanTemplatesElemTemplateElement;
@class OrgApacheXalanTemplatesElemVariable;
@class OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder;
@class OrgApacheXalanTemplatesStylesheetRoot;
@class OrgApacheXalanTemplatesVarNameCollector;
@class OrgApacheXmlUtilsQName;
@class OrgApacheXpathAxesLocPathIterator;
@class OrgApacheXpathAxesWalkingIterator;
@class OrgApacheXpathExpression;
@protocol OrgApacheXpathExpressionNode;
@protocol OrgApacheXpathExpressionOwner;

/*!
 @brief This class eleminates redundent XPaths from a given subtree, 
  and also collects all absolute paths within the subtree.First 
  it must be called as a visitor to the subtree, and then 
  eleminateRedundent must be called.
 */
@interface OrgApacheXalanTemplatesRedundentExprEliminator : OrgApacheXalanTemplatesXSLTVisitor {
 @public
  JavaUtilVector *m_paths_;
  JavaUtilVector *m_absPaths_;
  jboolean m_isSameContext_;
  OrgApacheXalanTemplatesAbsPathChecker *m_absPathChecker_;
  /*!
   @brief So we can reuse it over and over again.
   */
  OrgApacheXalanTemplatesVarNameCollector *m_varNameCollector_;
}
@property (readonly, copy, class) NSString *PSUEDOVARNAMESPACE NS_SWIFT_NAME(PSUEDOVARNAMESPACE);
@property (readonly, class) jboolean DEBUG_ NS_SWIFT_NAME(DEBUG_);
@property (readonly, class) jboolean DIAGNOSE_NUM_PATHS_REDUCED NS_SWIFT_NAME(DIAGNOSE_NUM_PATHS_REDUCED);
@property (readonly, class) jboolean DIAGNOSE_MULTISTEPLIST NS_SWIFT_NAME(DIAGNOSE_MULTISTEPLIST);

+ (NSString *)PSUEDOVARNAMESPACE;

+ (jboolean)DEBUG_;

+ (jboolean)DIAGNOSE_NUM_PATHS_REDUCED;

+ (jboolean)DIAGNOSE_MULTISTEPLIST;

#pragma mark Public

/*!
 @brief Construct a RedundentExprEliminator.
 */
- (instancetype __nonnull)init;

/*!
 @brief Method to be called after the all global expressions within a stylesheet 
  have been collected.It eliminates redundent 
  expressions by creating a variable in the psuedoVarRecipient 
  for each redundent expression, and then rewriting the redundent 
  expression to be a variable reference.
 */
- (void)eleminateRedundentGlobalsWithOrgApacheXalanTemplatesStylesheetRoot:(OrgApacheXalanTemplatesStylesheetRoot *)stylesheet;

/*!
 @brief Method to be called after the all expressions within an  
  node context have been visited.It eliminates redundent 
  expressions by creating a variable in the psuedoVarRecipient 
  for each redundent expression, and then rewriting the redundent 
  expression to be a variable reference.
 @param psuedoVarRecipient The recipient of the psuedo vars.  The   variables will be inserted as first children of the element, before 
   any existing variables.
 */
- (void)eleminateRedundentLocalsWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient;

/*!
 @brief Tell if the given LocPathIterator is relative to an absolute path, i.e.
 in not dependent on the context.
 @return true if the LocPathIterator is not dependent on the context node.
 */
- (jboolean)isAbsoluteWithOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)path;

/*!
 @brief Visit an XSLT instruction.Any element that isn't called by one 
  of the other visit methods, will be called by this method.
 @param elem The xsl instruction element object.
 @return true if the sub expressions should be traversed.
 */
- (jboolean)visitInstructionWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

/*!
 @brief Visit a LocationPath.
 @param owner The owner of the expression, to which the expression can                be reset if rewriting takes place.
 @param path The LocationPath object.
 @return true if the sub expressions should be traversed.
 */
- (jboolean)visitLocationPathWithOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)owner
                         withOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)path;

/*!
 @brief Visit a predicate within a location path.Note that there isn't a 
  proper unique component for predicates, and that the expression will 
  be called also for whatever type Expression is.
 @param owner The owner of the expression, to which the expression can                be reset if rewriting takes place.
 @param pred The predicate object.
 @return true if the sub expressions should be traversed.
 */
- (jboolean)visitPredicateWithOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)owner
                               withOrgApacheXpathExpression:(OrgApacheXpathExpression *)pred;

/*!
 @brief Visit an XSLT top-level instruction.
 @param elem The xsl instruction element object.
 @return true if the sub expressions should be traversed.
 */
- (jboolean)visitTopLevelInstructionWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

#pragma mark Protected

/*!
 @brief Add the given variable to the psuedoVarRecipient.
 */
- (OrgApacheXalanTemplatesElemVariable *)addVarDeclToElemWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                                                                  withOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)lpi
                                                                withOrgApacheXalanTemplatesElemVariable:(OrgApacheXalanTemplatesElemVariable *)psuedoVar;

/*!
 @brief Simple assertion.
 */
+ (void)assertionWithBoolean:(jboolean)b
                withNSString:(NSString *)msg;

/*!
 @brief Change a given number of steps to a single variable reference.
 @param uniquePseudoVarName The name of the variable reference.
 @param wi The walking iterator that is to be changed.
 @param numSteps The number of steps to be changed.
 @param isGlobal true if this will be a global reference.
 */
- (OrgApacheXpathAxesLocPathIterator *)changePartToRefWithOrgApacheXmlUtilsQName:(OrgApacheXmlUtilsQName *)uniquePseudoVarName
                                           withOrgApacheXpathAxesWalkingIterator:(OrgApacheXpathAxesWalkingIterator *)wi
                                                                         withInt:(jint)numSteps
                                                                     withBoolean:(jboolean)isGlobal;

/*!
 @brief Change the expression owned by the owner argument to a variable reference 
  of the given name.
 Warning: For global vars, this function relies on the variable declaration 
  to which it refers having been added just prior to this function being called,
  so that the reference index can be determined from the size of the global variables 
  list minus one.
 @param varName The name of the variable which will be referenced.
 @param owner The owner of the expression which will be replaced by a variable ref.
 @param paths The paths list that the iterator came from, mainly to determine               if this is a local or global reduction.
 @param psuedoVarRecipient The element within whose scope the variable is                             being inserted, possibly a StylesheetRoot.
 */
- (void)changeToVarRefWithOrgApacheXmlUtilsQName:(OrgApacheXmlUtilsQName *)varName
               withOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)owner
                              withJavaUtilVector:(JavaUtilVector *)paths
  withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient;

/*!
 @brief Count the number of ancestors that a ElemTemplateElement has.
 @param elem An representation of an element in an XSLT stylesheet.
 @return The number of ancestors of elem (including the element itself).
 */
- (jint)countAncestorsWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

/*!
 @brief Count the steps in a given location path.
 @param lpi The location path iterator that owns the steps.
 @return The number of steps in the given location path.
 */
- (jint)countStepsWithOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)lpi;

/*!
 @brief Create a psuedo variable reference that will represent the 
  shared redundent XPath, for a local reduction.
 @param uniquePseudoVarName The name of the new variable.
 @param stylesheetRoot The broadest scope of where the variable          should be inserted, which must be a StylesheetRoot element in this case.
 @param lpi The LocationPathIterator that the variable should represent.
 @return null if the decl was not created, otherwise the new Pseudo var  
               element.
 */
- (OrgApacheXalanTemplatesElemVariable *)createGlobalPseudoVarDeclWithOrgApacheXmlUtilsQName:(OrgApacheXmlUtilsQName *)uniquePseudoVarName
                                                   withOrgApacheXalanTemplatesStylesheetRoot:(OrgApacheXalanTemplatesStylesheetRoot *)stylesheetRoot
                                                       withOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)lpi;

/*!
 @brief Create a new WalkingIterator from the steps in another WalkingIterator.
 @param wi The iterator from where the steps will be taken.
 @param numSteps The number of steps from the first to copy into the new                   iterator.
 @return The new iterator.
 */
- (OrgApacheXpathAxesWalkingIterator *)createIteratorFromStepsWithOrgApacheXpathAxesWalkingIterator:(OrgApacheXpathAxesWalkingIterator *)wi
                                                                                            withInt:(jint)numSteps;

/*!
 @brief Create a psuedo variable reference that will represent the 
  shared redundent XPath, for a local reduction.
 @param uniquePseudoVarName The name of the new variable.
 @param psuedoVarRecipient The broadest scope of where the variable   should be inserted, usually an xsl:template or xsl:for-each.
 @param lpi The LocationPathIterator that the variable should represent.
 @return null if the decl was not created, otherwise the new Pseudo var  
               element.
 */
- (OrgApacheXalanTemplatesElemVariable *)createLocalPseudoVarDeclWithOrgApacheXmlUtilsQName:(OrgApacheXmlUtilsQName *)uniquePseudoVarName
                                             withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                                                      withOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)lpi;

/*!
 @brief For the reduction of location path parts, create a list of all 
  the multistep paths with more than one step, sorted by the 
  number of steps, with the most steps occuring earlier in the list.
 If the list is only one member, don't bother returning it.
 @param paths Vector of ExpressionOwner objects, which may contain null entries.                The ExpressionOwner objects must own LocPathIterator objects.
 @return null if no multipart paths are found or the list is only of length 1, 
  otherwise the first MultistepExprHolder in a linked list of these objects.
 */
- (OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)createMultistepExprListWithJavaUtilVector:(JavaUtilVector *)paths;

/*!
 @brief Create a psuedo variable reference that will represent the 
  shared redundent XPath, and add it to the stylesheet.
 @param psuedoVarRecipient The broadest scope of where the variable   should be inserted, usually an xsl:template or xsl:for-each.
 @param lpi The LocationPathIterator that the variable should represent.
 @param isGlobal true if the paths are global.
 @return The new psuedo var element.
 */
- (OrgApacheXalanTemplatesElemVariable *)createPseudoVarDeclWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                                                                     withOrgApacheXpathAxesLocPathIterator:(OrgApacheXpathAxesLocPathIterator *)lpi
                                                                                               withBoolean:(jboolean)isGlobal;

/*!
 @brief Tell what line number belongs to a given expression.
 */
- (void)diagnoseLineNumberWithOrgApacheXpathExpression:(OrgApacheXpathExpression *)expr;

/*!
 @brief Print out diagnostics about partial multistep evaluation.
 */
- (void)diagnoseMultistepListWithInt:(jint)matchCount
                             withInt:(jint)lengthToTest
                         withBoolean:(jboolean)isGlobal;

/*!
 @brief Print out to std err the number of paths reduced.
 */
- (void)diagnoseNumPathsWithJavaUtilVector:(JavaUtilVector *)paths
                                   withInt:(jint)numPathsEliminated
                                   withInt:(jint)numUniquePathsEliminated;

/*!
 @brief Method to be called after the all expressions within an  
  node context have been visited.It eliminates redundent 
  expressions by creating a variable in the psuedoVarRecipient 
  for each redundent expression, and then rewriting the redundent 
  expression to be a variable reference.
 @param psuedoVarRecipient The owner of the subtree from where the                             paths were collected.
 @param paths A vector of paths that hold ExpressionOwner objects,                which must yield LocationPathIterators.
 */
- (void)eleminateRedundentWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                                                      withJavaUtilVector:(JavaUtilVector *)paths;

/*!
 @brief Eliminate the shared partial paths in the expression list.
 @param psuedoVarRecipient The recipient of the psuedo vars.
 @param paths A vector of paths that hold ExpressionOwner objects,                which must yield LocationPathIterators.
 */
- (void)eleminateSharedPartialPathsWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                                                               withJavaUtilVector:(JavaUtilVector *)paths;

/*!
 @brief Look through the vector from start point, looking for redundant occurances.
 When one or more are found, create a psuedo variable declaration, insert 
  it into the stylesheet, and replace the occurance with a reference to 
  the psuedo variable.  When a redundent variable is found, it's slot in 
  the vector will be replaced by null.
 @param start The position to start looking in the vector.
 @param firstOccuranceIndex The position of firstOccuranceOwner.
 @param firstOccuranceOwner The owner of the expression we are looking for.
 @param psuedoVarRecipient Where to put the psuedo variables.
 @return The number of expression occurances that were modified.
 */
- (jint)findAndEliminateRedundantWithInt:(jint)start
                                 withInt:(jint)firstOccuranceIndex
       withOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)firstOccuranceOwner
withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                      withJavaUtilVector:(JavaUtilVector *)paths;

/*!
 @brief Given a linked list of expressions, find the common ancestor that is 
  suitable for holding a psuedo variable for shared access.
 */
- (OrgApacheXalanTemplatesElemTemplateElement *)findCommonAncestorWithOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)head;

/*!
 @brief From an XPath expression component, get the ElemTemplateElement 
  owner.
 @param expr Should be static expression with proper parentage.
 @return Valid ElemTemplateElement, or throw a runtime exception 
  if it is not found.
 */
- (OrgApacheXalanTemplatesElemTemplateElement *)getElemFromExpressionWithOrgApacheXpathExpression:(OrgApacheXpathExpression *)expr;

/*!
 @brief Get the previous sibling or parent of the given template, stopping at 
  xsl:for-each, xsl:template, or xsl:stylesheet.
 @param elem Should be non-null template element.
 @return previous sibling or parent, or null if previous is xsl:for-each, 
  xsl:template, or xsl:stylesheet.
 */
- (OrgApacheXalanTemplatesElemTemplateElement *)getPrevElementWithinContextWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

/*!
 @brief Find the previous occurance of a xsl:variable.Stop 
  the search when a xsl:for-each, xsl:template, or xsl:stylesheet is 
  encountered.
 @param elem Should be non-null template element.
 @return The first previous occurance of an xsl:variable or xsl:param, 
  or null if none is found.
 */
- (OrgApacheXalanTemplatesElemVariable *)getPrevVariableElemWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)elem;

/*!
 @brief Find out if the given ElemTemplateElement is not the same as one of 
  the ElemTemplateElement owners of the expressions.
 @param head Head of linked list of expression owners.
 @param ete The ElemTemplateElement that is a candidate for a psuedo   variable parent.
 @return true if the given ElemTemplateElement is not the same as one of 
  the ElemTemplateElement owners of the expressions.  This is to make sure 
  we find an ElemTemplateElement that is in a viable position to hold 
  psuedo variables that are visible to the references.
 */
- (jboolean)isNotSameAsOwnerWithOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)head
                                                    withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)ete;

/*!
 @brief Tell if the expr param is contained within an xsl:param.
 */
- (jboolean)isParamWithOrgApacheXpathExpressionNode:(id<OrgApacheXpathExpressionNode>)expr;

/*!
 @brief For a given path, see if there are any partitial matches in the list, 
  and, if there are, replace those partial paths with psuedo variable refs,
  and create the psuedo variable decl.
 @return The head of the list, which may have changed.
 */
- (OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)matchAndEliminatePartialPathsWithOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)testee
                                                                                                     withOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)head
                                                                                                                                                                withBoolean:(jboolean)isGlobal
                                                                                                                                                                    withInt:(jint)lengthToTest
                                                                                                                             withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)varScope;

/*!
 @brief To be removed.
 */
- (jint)oldFindAndEliminateRedundantWithInt:(jint)start
                                    withInt:(jint)firstOccuranceIndex
          withOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)firstOccuranceOwner
withOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)psuedoVarRecipient
                         withJavaUtilVector:(JavaUtilVector *)paths;

/*!
 @brief Compare a given number of steps between two iterators, to see if they are equal.
 @param iter1 The first iterator to compare.
 @param iter2 The second iterator to compare.
 @param numSteps The number of steps to compare.
 @return true If the given number of steps are equal.
 */
- (jboolean)stepsEqualWithOrgApacheXpathAxesWalkingIterator:(OrgApacheXpathAxesWalkingIterator *)iter1
                      withOrgApacheXpathAxesWalkingIterator:(OrgApacheXpathAxesWalkingIterator *)iter2
                                                    withInt:(jint)numSteps;

#pragma mark Package-Private

/*!
 @brief Check if results of partial reduction will just be a variable, in 
  which case, skip it.
 */
- (jboolean)partialIsVariableWithOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)testee
                                                                                            withInt:(jint)lengthToTest;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesRedundentExprEliminator)

J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator, m_paths_, JavaUtilVector *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator, m_absPaths_, JavaUtilVector *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator, m_absPathChecker_, OrgApacheXalanTemplatesAbsPathChecker *)
J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator, m_varNameCollector_, OrgApacheXalanTemplatesVarNameCollector *)

inline NSString *OrgApacheXalanTemplatesRedundentExprEliminator_get_PSUEDOVARNAMESPACE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT NSString *OrgApacheXalanTemplatesRedundentExprEliminator_PSUEDOVARNAMESPACE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(OrgApacheXalanTemplatesRedundentExprEliminator, PSUEDOVARNAMESPACE, NSString *)

inline jboolean OrgApacheXalanTemplatesRedundentExprEliminator_get_DEBUG(void);
#define OrgApacheXalanTemplatesRedundentExprEliminator_DEBUG false
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesRedundentExprEliminator, DEBUG, jboolean)

inline jboolean OrgApacheXalanTemplatesRedundentExprEliminator_get_DIAGNOSE_NUM_PATHS_REDUCED(void);
#define OrgApacheXalanTemplatesRedundentExprEliminator_DIAGNOSE_NUM_PATHS_REDUCED false
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesRedundentExprEliminator, DIAGNOSE_NUM_PATHS_REDUCED, jboolean)

inline jboolean OrgApacheXalanTemplatesRedundentExprEliminator_get_DIAGNOSE_MULTISTEPLIST(void);
#define OrgApacheXalanTemplatesRedundentExprEliminator_DIAGNOSE_MULTISTEPLIST false
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesRedundentExprEliminator, DIAGNOSE_MULTISTEPLIST, jboolean)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesRedundentExprEliminator_init(OrgApacheXalanTemplatesRedundentExprEliminator *self);

FOUNDATION_EXPORT OrgApacheXalanTemplatesRedundentExprEliminator *new_OrgApacheXalanTemplatesRedundentExprEliminator_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesRedundentExprEliminator *create_OrgApacheXalanTemplatesRedundentExprEliminator_init(void);

FOUNDATION_EXPORT void OrgApacheXalanTemplatesRedundentExprEliminator_assertionWithBoolean_withNSString_(jboolean b, NSString *msg);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesRedundentExprEliminator)

#endif

#if !defined (OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_) && (INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator || defined(INCLUDE_OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder))
#define OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_

@class OrgApacheXalanTemplatesRedundentExprEliminator;
@protocol OrgApacheXpathExpressionOwner;

/*!
 @brief Since we want to sort multistep expressions by length, use 
  a linked list with elements of type MultistepExprHolder.
 */
@interface OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder : NSObject < NSCopying > {
 @public
  id<OrgApacheXpathExpressionOwner> m_exprOwner_;
  jint m_stepCount_;
  OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *m_next_;
}

#pragma mark Public

/*!
 @brief Clone this object.
 */
- (id)java_clone;

#pragma mark Protected

/*!
 @brief Print diagnostics out for the multistep list.
 */
- (void)diagnose;

#pragma mark Package-Private

/*!
 @brief Create a MultistepExprHolder.
 @param exprOwner the owner of the expression we are holding.                   It must hold a LocationPathIterator.
 @param stepCount The number of steps in the location path.
 */
- (instancetype __nonnull)initWithOrgApacheXalanTemplatesRedundentExprEliminator:(OrgApacheXalanTemplatesRedundentExprEliminator *)outer$
                                               withOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)exprOwner
                                                                         withInt:(jint)stepCount
          withOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)next;

/*!
 @brief Add a new MultistepExprHolder in sorted order in the list.
 @param exprOwner the owner of the expression we are holding.                   It must hold a LocationPathIterator.
 @param stepCount The number of steps in the location path.
 @return The new head of the linked list.
 */
- (OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)addInSortedOrderWithOrgApacheXpathExpressionOwner:(id<OrgApacheXpathExpressionOwner>)exprOwner
                                                                                                                  withInt:(jint)stepCount;

/*!
 @brief Get the number of linked list items.
 */
- (jint)getLength;

/*!
 @brief Remove the given element from the list.'
 this' should 
  be the head of the list.  If the item to be removed is not 
  found, an assertion will be made.
 @param itemToRemove The item to remove from the list.
 @return The head of the list, which may have changed if itemToRemove 
  is the same as this element.  Null if the item to remove is the 
  only item in the list.
 */
- (OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)unlinkWithOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder:(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)itemToRemove;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder)

J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder, m_exprOwner_, id<OrgApacheXpathExpressionOwner>)
J2OBJC_FIELD_SETTER(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder, m_next_, OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_initWithOrgApacheXalanTemplatesRedundentExprEliminator_withOrgApacheXpathExpressionOwner_withInt_withOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *self, OrgApacheXalanTemplatesRedundentExprEliminator *outer$, id<OrgApacheXpathExpressionOwner> exprOwner, jint stepCount, OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *next);

FOUNDATION_EXPORT OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *new_OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_initWithOrgApacheXalanTemplatesRedundentExprEliminator_withOrgApacheXpathExpressionOwner_withInt_withOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_(OrgApacheXalanTemplatesRedundentExprEliminator *outer$, id<OrgApacheXpathExpressionOwner> exprOwner, jint stepCount, OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *next) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *create_OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_initWithOrgApacheXalanTemplatesRedundentExprEliminator_withOrgApacheXpathExpressionOwner_withInt_withOrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder_(OrgApacheXalanTemplatesRedundentExprEliminator *outer$, id<OrgApacheXpathExpressionOwner> exprOwner, jint stepCount, OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder *next);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesRedundentExprEliminator_MultistepExprHolder)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTemplatesRedundentExprEliminator")
