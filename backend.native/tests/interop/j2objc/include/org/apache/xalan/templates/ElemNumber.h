//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/xalan/third_party/android/platform/external/apache-xml/src/main/java/org/apache/xalan/templates/ElemNumber.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber")
#ifdef RESTRICT_OrgApacheXalanTemplatesElemNumber
#define INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber 0
#else
#define INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber 1
#endif
#undef RESTRICT_OrgApacheXalanTemplatesElemNumber

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgApacheXalanTemplatesElemNumber_) && (INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber || defined(INCLUDE_OrgApacheXalanTemplatesElemNumber))
#define OrgApacheXalanTemplatesElemNumber_

#define RESTRICT_OrgApacheXalanTemplatesElemTemplateElement 1
#define INCLUDE_OrgApacheXalanTemplatesElemTemplateElement 1
#include "org/apache/xalan/templates/ElemTemplateElement.h"

@class IOSLongArray;
@class JavaUtilLocale;
@class OrgApacheXalanTemplatesAVT;
@class OrgApacheXalanTemplatesStylesheetRoot;
@class OrgApacheXalanTemplatesXSLTVisitor;
@class OrgApacheXalanTransformerTransformerImpl;
@class OrgApacheXmlUtilsFastStringBuffer;
@class OrgApacheXmlUtilsNodeVector;
@class OrgApacheXmlUtilsResCharArrayWrapper;
@class OrgApacheXmlUtilsResXResourceBundle;
@class OrgApacheXpathXPath;
@class OrgApacheXpathXPathContext;

/*!
 @brief Implement xsl:number.
 @code

   <!ELEMENT xsl:number EMPTY>
   <!ATTLIST xsl:number
     level (single|multiple|any) "single"
     count %pattern; #IMPLIED
     from %pattern; #IMPLIED
     value %expr; #IMPLIED
     format %avt; '1'
     lang %avt; #IMPLIED
     letter-value %avt; #IMPLIED
     grouping-separator %avt; #IMPLIED
     grouping-size %avt; #IMPLIED 
  >
   
@endcode
 - seealso: <a href="http://www.w3.org/TR/xslt#number">number in XSLT Specification</a>
 */
@interface OrgApacheXalanTemplatesElemNumber : OrgApacheXalanTemplatesElemTemplateElement
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Add a child to the child list.
 @param newChild Child to add to child list
 @return Child just added to child list
 @throw DOMException
 */
- (OrgApacheXalanTemplatesElemTemplateElement *)appendChildWithOrgApacheXalanTemplatesElemTemplateElement:(OrgApacheXalanTemplatesElemTemplateElement *)newChild;

/*!
 @brief Call the children visitors.
 @param visitor The visitor whose appropriate method will be called.
 */
- (void)callChildVisitorsWithOrgApacheXalanTemplatesXSLTVisitor:(OrgApacheXalanTemplatesXSLTVisitor *)visitor
                                                    withBoolean:(jboolean)callAttrs;

/*!
 @brief This function is called after everything else has been
  recomposed, and allows the template to set remaining
  values that may be based on some other property that
  depends on recomposition.
 */
- (void)composeWithOrgApacheXalanTemplatesStylesheetRoot:(OrgApacheXalanTemplatesStylesheetRoot *)sroot;

/*!
 @brief Execute an xsl:number instruction.The xsl:number element is
  used to insert a formatted number into the result tree.
 @param transformer non-null reference to the the current transform-time state.
 @throw TransformerException
 */
- (void)executeWithOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer;

/*!
 @brief Get the "count" attribute.
 The count attribute is a pattern that specifies what nodes
  should be counted at those levels. If count attribute is not
  specified, then it defaults to the pattern that matches any
  node with the same node type as the current node and, if the
  current node has an expanded-name, with the same expanded-name
  as the current node.
 @return Value of "count" attribute.
 */
- (OrgApacheXpathXPath *)getCount;

/*!
 @brief Get the "format" attribute.
 The "format" attribute is used to control conversion of a list of
  numbers into a string.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @return Value of "format" attribute.
 */
- (OrgApacheXalanTemplatesAVT *)getFormat;

/*!
 @brief Get the "from" attribute.
 For level="single" or level="multiple":
  Only ancestors that are searched are
  those that are descendants of the nearest ancestor that matches
  the from pattern.
  For level="any:
  Only nodes after the first node before the
  current node that match the from pattern are considered.
 @return Value of "from" attribute.
 */
- (OrgApacheXpathXPath *)getFrom;

/*!
 @brief Get the "grouping-separator" attribute.
 The grouping-separator attribute gives the separator
  used as a grouping (e.g. thousands) separator in decimal
  numbering sequences.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @return Value of "grouping-separator" attribute.
 */
- (OrgApacheXalanTemplatesAVT *)getGroupingSeparator;

/*!
 @brief Get the "grouping-size" attribute.
 The optional grouping-size specifies the size (normally 3) of the grouping.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @return Value of "grouping-size" attribute.
 */
- (OrgApacheXalanTemplatesAVT *)getGroupingSize;

/*!
 @brief Get the "lang" attribute.
 When numbering with an alphabetic sequence, the lang attribute
  specifies which language's alphabet is to be used; it has the same
  range of values as xml:lang [XML]; if no lang value is specified,
  the language should be determined from the system environment.
  Implementers should document for which languages they support numbering.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @return Value ofr "lang" attribute.
 */
- (OrgApacheXalanTemplatesAVT *)getLang;

/*!
 @brief Get the "letter-value" attribute.
 The letter-value attribute disambiguates between numbering sequences
  that use letters.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @return Value to set for "letter-value" attribute.
 */
- (OrgApacheXalanTemplatesAVT *)getLetterValue;

/*!
 @brief Get the "level" attribute.
 The level attribute specifies what levels of the source tree should
  be considered; it has the values single, multiple or any. The default
  is single.
 @return Value of "level" attribute.
 */
- (jint)getLevel;

/*!
 @brief Return the node name.
 @return The element's name
 */
- (NSString *)getNodeName;

/*!
 @brief Get the previous node to be counted.
 @param xctxt The XPath runtime state for this.
 @param pos The current node
 @return the previous node to be counted.
 @throw TransformerException
 */
- (jint)getPreviousNodeWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt
                                              withInt:(jint)pos;

/*!
 @brief Get the target node that will be counted..
 @param xctxt The XPath runtime state for this.
 @param sourceNode non-null reference to the  <a href="http://www.w3.org/TR/xslt#dt-current-node"> current source node </a>
  .
 @return the target node that will be counted
 @throw TransformerException
 */
- (jint)getTargetNodeWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt
                                            withInt:(jint)sourceNode;

/*!
 @brief Get the "value" attribute.
 The value attribute contains an expression. The expression is evaluated
  and the resulting object is converted to a number as if by a call to the
  number function.
 @return Value of "value" attribute.
 */
- (OrgApacheXpathXPath *)getValue;

/*!
 @brief Get an int constant identifying the type of element.
 - seealso: org.apache.xalan.templates.Constants
 @return The token ID for this element
 */
- (jint)getXSLToken;

/*!
 @brief Set the "count" attribute.
 The count attribute is a pattern that specifies what nodes
  should be counted at those levels. If count attribute is not
  specified, then it defaults to the pattern that matches any
  node with the same node type as the current node and, if the
  current node has an expanded-name, with the same expanded-name
  as the current node.
 @param v Value to set for "count" attribute.
 */
- (void)setCountWithOrgApacheXpathXPath:(OrgApacheXpathXPath *)v;

/*!
 @brief Set the "format" attribute.
 The "format" attribute is used to control conversion of a list of
  numbers into a string.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @param v Value to set for "format" attribute.
 */
- (void)setFormatWithOrgApacheXalanTemplatesAVT:(OrgApacheXalanTemplatesAVT *)v;

/*!
 @brief Set the "from" attribute.Specifies where to count from.
 For level="single" or level="multiple":
  Only ancestors that are searched are
  those that are descendants of the nearest ancestor that matches
  the from pattern.
  For level="any:
  Only nodes after the first node before the
  current node that match the from pattern are considered.
 @param v Value to set for "from" attribute.
 */
- (void)setFromWithOrgApacheXpathXPath:(OrgApacheXpathXPath *)v;

/*!
 @brief Set the "grouping-separator" attribute.
 The grouping-separator attribute gives the separator
  used as a grouping (e.g. thousands) separator in decimal
  numbering sequences.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @param v Value to set for "grouping-separator" attribute.
 */
- (void)setGroupingSeparatorWithOrgApacheXalanTemplatesAVT:(OrgApacheXalanTemplatesAVT *)v;

/*!
 @brief Set the "grouping-size" attribute.
 The optional grouping-size specifies the size (normally 3) of the grouping.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @param v Value to set for "grouping-size" attribute.
 */
- (void)setGroupingSizeWithOrgApacheXalanTemplatesAVT:(OrgApacheXalanTemplatesAVT *)v;

/*!
 @brief Set the "lang" attribute.
 When numbering with an alphabetic sequence, the lang attribute
  specifies which language's alphabet is to be used; it has the same
  range of values as xml:lang [XML]; if no lang value is specified,
  the language should be determined from the system environment.
  Implementers should document for which languages they support numbering.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @param v Value to set for "lang" attribute.
 */
- (void)setLangWithOrgApacheXalanTemplatesAVT:(OrgApacheXalanTemplatesAVT *)v;

/*!
 @brief Set the "letter-value" attribute.
 The letter-value attribute disambiguates between numbering sequences
  that use letters.
 - seealso: <a href="http://www.w3.org/TR/xslt#convert">convert in XSLT Specification</a>
 @param v Value to set for "letter-value" attribute.
 */
- (void)setLetterValueWithOrgApacheXalanTemplatesAVT:(OrgApacheXalanTemplatesAVT *)v;

/*!
 @brief Set the "level" attribute.
 The level attribute specifies what levels of the source tree should
  be considered; it has the values single, multiple or any. The default
  is single.
 @param v Value to set for "level" attribute.
 */
- (void)setLevelWithInt:(jint)v;

/*!
 @brief Set the "value" attribute.
 The value attribute contains an expression. The expression is evaluated
  and the resulting object is converted to a number as if by a call to the
  number function.
 @param v Value to set for "value" attribute.
 */
- (void)setValueWithOrgApacheXpathXPath:(OrgApacheXpathXPath *)v;

#pragma mark Protected

/*!
 @brief Convert a long integer into alphabetic counting, in other words
  count using the sequence A B C ...Z AA AB AC.... etc.
 @param val Value to convert -- must be greater than zero.
 @param table a table containing one character for each digit in the radix
 @param aTable Array of alpha characters representing numbers
 @param stringBuf Buffer where to save the string representing alpha count of number.
 - seealso: TransformerImpl#DecimalToRoman
 Note that the radix of the conversion is inferred from the size
  of the table.
 */
- (void)int2alphaCountWithLong:(jlong)val
withOrgApacheXmlUtilsResCharArrayWrapper:(OrgApacheXmlUtilsResCharArrayWrapper *)aTable
withOrgApacheXmlUtilsFastStringBuffer:(OrgApacheXmlUtilsFastStringBuffer *)stringBuf;

/*!
 @brief Convert a long integer into alphabetic counting, in other words
  count using the sequence A B C ...Z.
 @param val Value to convert -- must be greater than zero.
 @param table a table containing one character for each digit in the radix
 @return String representing alpha count of number.
 - seealso: TransformerImpl#DecimalToRoman
 Note that the radix of the conversion is inferred from the size
  of the table.
 */
- (NSString *)int2singlealphaCountWithLong:(jlong)val
  withOrgApacheXmlUtilsResCharArrayWrapper:(OrgApacheXmlUtilsResCharArrayWrapper *)table;

/*!
 @brief Convert a long integer into roman numerals.
 @param val Value to convert.
 @param prefixesAreOK true_ to enable prefix notation (e.g. 4 = "IV"),  false_ to disable prefix notation (e.g. 4 = "IIII").
 @return Roman numeral string.
 - seealso: DecimalToRoman
 - seealso: m_romanConvertTable
 */
- (NSString *)long2romanWithLong:(jlong)val
                     withBoolean:(jboolean)prefixesAreOK;

/*!
 @brief Convert a long integer into traditional alphabetic counting, in other words
  count using the traditional numbering.
 @param val Value to convert -- must be greater than zero.
 @param thisBundle Resource bundle to use
 @return String representing alpha count of number.
 - seealso: XSLProcessor#DecimalToRoman
 Note that the radix of the conversion is inferred from the size
  of the table.
 */
- (NSString *)tradAlphaCountWithLong:(jlong)val
withOrgApacheXmlUtilsResXResourceBundle:(OrgApacheXmlUtilsResXResourceBundle *)thisBundle;

#pragma mark Package-Private

/*!
 @brief Given a 'from' pattern (ala xsl:number), a match pattern
  and a context, find the first ancestor that matches the
  pattern (including the context handed in).
 @param xctxt The XPath runtime state for this.
 @param fromMatchPattern The ancestor must match this pattern.
 @param countMatchPattern The ancestor must also match this pattern.
 @param context The node that "." expresses.
 @param namespaceContext The context in which namespaces in the  queries are supposed to be expanded.
 @return the first ancestor that matches the given pattern
 @throw javax.xml.transform.TransformerException
 */
- (jint)findAncestorWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt
                           withOrgApacheXpathXPath:(OrgApacheXpathXPath *)fromMatchPattern
                           withOrgApacheXpathXPath:(OrgApacheXpathXPath *)countMatchPattern
                                           withInt:(jint)context
             withOrgApacheXalanTemplatesElemNumber:(OrgApacheXalanTemplatesElemNumber *)namespaceContext;

/*!
 @brief Format a vector of numbers into a formatted string.
 @param transformer non-null reference to the the current transform-time state.
 @param list Array of one or more long integer numbers.
 @param contextNode The node that "." expresses.
 @return String that represents list according to
  %conversion-atts; attributes.
  TODO: Optimize formatNumberList so that it caches the last count and
  reuses that info for the next count.
 @throw TransformerException
 */
- (NSString *)formatNumberListWithOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer
                                                             withLongArray:(IOSLongArray *)list
                                                                   withInt:(jint)contextNode;

/*!
 @brief Get the count match pattern, or a default value.
 @param support The XPath runtime state for this.
 @param contextNode The node that "." expresses.
 @return the count match pattern, or a default value.
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXpathXPath *)getCountMatchPatternWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)support
                                                                    withInt:(jint)contextNode;

/*!
 @brief Given an XML source node, get the count according to the
  parameters set up by the xsl:number attributes.
 @param transformer non-null reference to the the current transform-time state.
 @param sourceNode The source node being counted.
 @return The count of nodes
 @throw TransformerException
 */
- (NSString *)getCountStringWithOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer
                                                                 withInt:(jint)sourceNode;

/*!
 @brief Get the locale we should be using.
 @param transformer non-null reference to the the current transform-time state.
 @param contextNode The node that "." expresses.
 @return The locale to use. May be specified by "lang" attribute,
  but if not, use default locale on the system.
 @throw TransformerException
 */
- (JavaUtilLocale *)getLocaleWithOrgApacheXalanTransformerTransformerImpl:(OrgApacheXalanTransformerTransformerImpl *)transformer
                                                                  withInt:(jint)contextNode;

/*!
 @brief Get the ancestors, up to the root, that match the
  pattern.
 @param xctxt The XPath runtime state for this.
 @param node Count this node and it's ancestors.
 @param stopAtFirstFound Flag indicating to stop after the  first node is found (difference between level = single
   or multiple)
 @return The number of ancestors that match the pattern.
 @throw javax.xml.transform.TransformerException
 */
- (OrgApacheXmlUtilsNodeVector *)getMatchingAncestorsWithOrgApacheXpathXPathContext:(OrgApacheXpathXPathContext *)xctxt
                                                                            withInt:(jint)node
                                                                        withBoolean:(jboolean)stopAtFirstFound;

/*!
 @brief Get a string value for zero, which is not really defined by the 1.0 spec, 
  thought I think it might be cleared up by the erreta.
 */
- (NSString *)getZeroString;

@end

J2OBJC_STATIC_INIT(OrgApacheXalanTemplatesElemNumber)

inline jlong OrgApacheXalanTemplatesElemNumber_get_serialVersionUID(void);
#define OrgApacheXalanTemplatesElemNumber_serialVersionUID 8118472298274407610LL
J2OBJC_STATIC_FIELD_CONSTANT(OrgApacheXalanTemplatesElemNumber, serialVersionUID, jlong)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesElemNumber_init(OrgApacheXalanTemplatesElemNumber *self);

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemNumber *new_OrgApacheXalanTemplatesElemNumber_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemNumber *create_OrgApacheXalanTemplatesElemNumber_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesElemNumber)

#endif

#if !defined (OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer_) && (INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber || defined(INCLUDE_OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer))
#define OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer_

@class OrgApacheXalanTemplatesElemNumber;

/*!
 @brief This class returns tokens using non-alphanumberic
  characters as delimiters.
 */
@interface OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer : NSObject

#pragma mark Public

/*!
 @brief Construct a NumberFormatStringTokenizer.
 @param str Format string to be tokenized
 */
- (instancetype __nonnull)initWithOrgApacheXalanTemplatesElemNumber:(OrgApacheXalanTemplatesElemNumber *)outer$
                                                       withNSString:(NSString *)str;

/*!
 @brief Calculates the number of times that this tokenizer's 
 <code>nextToken</code> method can be called before it generates an
  exception.
 @return the number of tokens remaining in the string using the current
           delimiter set.
 - seealso: java.util.StringTokenizer#nextToken()
 */
- (jint)countTokens;

/*!
 @brief Tells if <code>nextToken</code> will throw an exception
  if it is called.
 @return true if <code>nextToken</code> can be called
  without throwing an exception.
 */
- (jboolean)hasMoreTokens;

/*!
 @brief Tells if there is a digit or a letter character ahead.
 @return true if there is a number or character ahead.
 */
- (jboolean)isLetterOrDigitAhead;

/*!
 @brief Tells if there is a digit or a letter character ahead.
 @return true if there is a number or character ahead.
 */
- (jboolean)nextIsSep;

/*!
 @brief Returns the next token from this string tokenizer.
 @return the next token from this string tokenizer.
 @throw NoSuchElementExceptionif there are no more tokens in this
                tokenizer's string.
 */
- (NSString *)nextToken;

/*!
 @brief Reset tokenizer so that nextToken() starts from the beginning.
 */
- (void)reset;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer)

FOUNDATION_EXPORT void OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer_initWithOrgApacheXalanTemplatesElemNumber_withNSString_(OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer *self, OrgApacheXalanTemplatesElemNumber *outer$, NSString *str);

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer *new_OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer_initWithOrgApacheXalanTemplatesElemNumber_withNSString_(OrgApacheXalanTemplatesElemNumber *outer$, NSString *str) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer *create_OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer_initWithOrgApacheXalanTemplatesElemNumber_withNSString_(OrgApacheXalanTemplatesElemNumber *outer$, NSString *str);

J2OBJC_TYPE_LITERAL_HEADER(OrgApacheXalanTemplatesElemNumber_NumberFormatStringTokenizer)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgApacheXalanTemplatesElemNumber")
