//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/primitives/Doubles.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonPrimitivesDoubles")
#ifdef RESTRICT_ComGoogleCommonPrimitivesDoubles
#define INCLUDE_ALL_ComGoogleCommonPrimitivesDoubles 0
#else
#define INCLUDE_ALL_ComGoogleCommonPrimitivesDoubles 1
#endif
#undef RESTRICT_ComGoogleCommonPrimitivesDoubles

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonPrimitivesDoubles_) && (INCLUDE_ALL_ComGoogleCommonPrimitivesDoubles || defined(INCLUDE_ComGoogleCommonPrimitivesDoubles))
#define ComGoogleCommonPrimitivesDoubles_

@class ComGoogleCommonBaseConverter;
@class IOSDoubleArray;
@class IOSObjectArray;
@class JavaLangDouble;
@class JavaUtilRegexPattern;
@protocol JavaUtilCollection;
@protocol JavaUtilComparator;
@protocol JavaUtilList;

/*!
 @brief Static utility methods pertaining to <code>double</code> primitives, that are not already found in
  either <code>Double</code> or <code>Arrays</code>.
 <p>See the Guava User Guide article on <a href="https://github.com/google/guava/wiki/PrimitivesExplained">
 primitive utilities</a>.
 @author Kevin Bourrillion
 @since 1.0
 */
@interface ComGoogleCommonPrimitivesDoubles : NSObject
@property (readonly, class) jint BYTES NS_SWIFT_NAME(BYTES);
@property (readonly, class, strong) JavaUtilRegexPattern *FLOATING_POINT_PATTERN NS_SWIFT_NAME(FLOATING_POINT_PATTERN);

+ (jint)BYTES;

+ (JavaUtilRegexPattern *)FLOATING_POINT_PATTERN;

#pragma mark Public

/*!
 @brief Returns a fixed-size list backed by the specified array, similar to <code>Arrays.asList(Object[])</code>
 .The list supports <code>List.set(int, Object)</code>, but any attempt to
  set a value to <code>null</code> will result in a <code>NullPointerException</code>.
 <p>The returned list maintains the values, but not the identities, of <code>Double</code> objects
  written to or read from it. For example, whether <code>list.get(0) == list.get(0)</code> is true for
  the returned list is unspecified. 
 <p>The returned list may have unexpected behavior if it contains <code>NaN</code>, or if <code>NaN</code>
  is used as a parameter to any of its methods. 
 <p><b>Note:</b> when possible, you should represent your data as an <code>ImmutableDoubleArray</code>
  instead, which has an <code>asList</code> view.
 @param backingArray the array to back the list
 @return a list view of the array
 */
+ (id<JavaUtilList>)asListWithDoubleArray:(IOSDoubleArray * __nonnull)backingArray;

/*!
 @brief Compares the two specified <code>double</code> values.The sign of the value returned is the same as
  that of <code>((Double) a).compareTo(b)</code>.
 As with that
  method, <code>NaN</code> is treated as greater than all other values, and <code>0.0 > -0.0</code>.
  
 <p><b>Note:</b> this method simply delegates to the JDK method <code>Double.compare</code>. It is
  provided for consistency with the other primitive types, whose compare methods were not added
  to the JDK until JDK 7.
 @param a the first <code>double</code>  to compare
 @param b the second <code>double</code>  to compare
 @return a negative value if <code>a</code> is less than <code>b</code>; a positive value if <code>a</code> is
      greater than <code>b</code>; or zero if they are equal
 */
+ (jint)compareWithDouble:(jdouble)a
               withDouble:(jdouble)b;

/*!
 @brief Returns the values from each provided array combined into a single array.For example, <code>concat(new double[] {a, b}, new double[] {}, new double[] {c}</code>
  returns the array <code>{a, b,
  c}</code>
 .
 @param arrays zero or more <code>double</code>  arrays
 @return a single array containing all the values from the source arrays, in order
 */
+ (IOSDoubleArray *)concatWithDoubleArray2:(IOSObjectArray * __nonnull)arrays;

/*!
 @brief Returns the value nearest to <code>value</code> which is within the closed range <code>[min..max]</code>.
 <p>If <code>value</code> is within the range <code>[min..max]</code>, <code>value</code> is returned
  unchanged. If <code>value</code> is less than <code>min</code>, <code>min</code> is returned, and if <code>value</code>
  is greater than <code>max</code>, <code>max</code> is returned.
 @param value the <code>double</code>  value to constrain
 @param min the lower bound (inclusive) of the range to constrain <code>value</code>  to
 @param max the upper bound (inclusive) of the range to constrain <code>value</code>  to
 @throw IllegalArgumentExceptionif <code>min > max</code>
 @since 21.0
 */
+ (jdouble)constrainToRangeWithDouble:(jdouble)value
                           withDouble:(jdouble)min
                           withDouble:(jdouble)max;

/*!
 @brief Returns <code>true</code> if <code>target</code> is present as an element anywhere in <code>array</code>.Note
  that this always returns <code>false</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>double</code>  values, possibly empty
 @param target a primitive <code>double</code>  value
 @return <code>true</code> if <code>array[i] == target</code> for some value of <code>i</code>
 */
+ (jboolean)containsWithDoubleArray:(IOSDoubleArray * __nonnull)array
                         withDouble:(jdouble)target;

/*!
 @brief Returns an array containing the same values as <code>array</code>, but guaranteed to be of a
  specified minimum length.If <code>array</code> already has a length of at least <code>minLength</code>,
  it is returned directly.
 Otherwise, a new array of size <code>minLength + padding</code> is
  returned, containing the values of <code>array</code>, and zeroes in the remaining places.
 @param array the source array
 @param minLength the minimum length the returned array must guarantee
 @param padding an extra amount to "grow" the array by if growth is necessary
 @throw IllegalArgumentExceptionif <code>minLength</code> or <code>padding</code> is negative
 @return an array containing the values of <code>array</code>, with guaranteed minimum length <code>minLength</code>
 */
+ (IOSDoubleArray *)ensureCapacityWithDoubleArray:(IOSDoubleArray * __nonnull)array
                                          withInt:(jint)minLength
                                          withInt:(jint)padding;

/*!
 @brief Returns a hash code for <code>value</code>; equal to the result of invoking <code>((Double)
  value).hashCode()</code>
 .
 <p><b>Java 8 users:</b> use <code>Double.hashCode(double)</code> instead.
 @param value a primitive <code>double</code>  value
 @return a hash code for the value
 */
+ (jint)hashCodeWithDouble:(jdouble)value;

/*!
 @brief Returns the index of the first appearance of the value <code>target</code> in <code>array</code>.Note
  that this always returns <code>-1</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>double</code>  values, possibly empty
 @param target a primitive <code>double</code>  value
 @return the least index <code>i</code> for which <code>array[i] == target</code>, or <code>-1</code> if no
      such index exists.
 */
+ (jint)indexOfWithDoubleArray:(IOSDoubleArray * __nonnull)array
                    withDouble:(jdouble)target;

/*!
 @brief Returns the start position of the first occurrence of the specified <code>target</code> within 
 <code>array</code>, or <code>-1</code> if there is no such occurrence.
 <p>More formally, returns the lowest index <code>i</code> such that <code>Arrays.copyOfRange(array,
  i, i + target.length)</code>
  contains exactly the same elements as <code>target</code>.
  
 <p>Note that this always returns <code>-1</code> when <code>target</code> contains <code>NaN</code>.
 @param array the array to search for the sequence <code>target</code>
 @param target the array to search for as a sub-sequence of <code>array</code>
 */
+ (jint)indexOfWithDoubleArray:(IOSDoubleArray * __nonnull)array
               withDoubleArray:(IOSDoubleArray * __nonnull)target;

/*!
 @brief Returns <code>true</code> if <code>value</code> represents a real number.This is equivalent to, but not
  necessarily implemented as, <code>!
 (Double.isInfinite(value) || Double.isNaN(value))</code>.
  
 <p><b>Java 8 users:</b> use <code>Double.isFinite(double)</code> instead.
 @since 10.0
 */
+ (jboolean)isFiniteWithDouble:(jdouble)value;

/*!
 @brief Returns a string containing the supplied <code>double</code> values, converted to strings as
  specified by <code>Double.toString(double)</code>, and separated by <code>separator</code>.For example, 
 <code>join("-", 1.0, 2.0, 3.0)</code> returns the string <code>"1.0-2.0-3.0"</code>.
 <p>Note that <code>Double.toString(double)</code> formats <code>double</code> differently in GWT
  sometimes. In the previous example, it returns the string <code>"1-2-3"</code>.
 @param separator the text that should appear between consecutive values in the resulting string      (but not at the start or end)
 @param array an array of <code>double</code>  values, possibly empty
 */
+ (NSString *)joinWithNSString:(NSString * __nonnull)separator
               withDoubleArray:(IOSDoubleArray * __nonnull)array;

/*!
 @brief Returns the index of the last appearance of the value <code>target</code> in <code>array</code>.Note
  that this always returns <code>-1</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>double</code>  values, possibly empty
 @param target a primitive <code>double</code>  value
 @return the greatest index <code>i</code> for which <code>array[i] == target</code>, or <code>-1</code> if no
      such index exists.
 */
+ (jint)lastIndexOfWithDoubleArray:(IOSDoubleArray * __nonnull)array
                        withDouble:(jdouble)target;

/*!
 @brief Returns a comparator that compares two <code>double</code> arrays <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
 lexicographically</a>.That is, it
  compares, using <code>compare(double, double)</code>), the first pair of values that follow any
  common prefix, or when one array is a prefix of the other, treats the shorter array as the
  lesser.
 For example, <code>[] < [1.0] < [1.0, 2.0] < [2.0]</code>.
  
 <p>The returned comparator is inconsistent with <code>Object.equals(Object)</code> (since arrays
  support only identity equality), but it is consistent with <code>Arrays.equals(double[],
 double[])</code>
 .
 @since 2.0
 */
+ (id<JavaUtilComparator>)lexicographicalComparator;

/*!
 @brief Returns the greatest value present in <code>array</code>, using the same rules of comparison as 
 <code>Math.max(double, double)</code>.
 @param array a  <i> nonempty </i>  array of <code>double</code>  values
 @return the value present in <code>array</code> that is greater than or equal to every other value
      in the array
 @throw IllegalArgumentExceptionif <code>array</code> is empty
 */
+ (jdouble)maxWithDoubleArray:(IOSDoubleArray * __nonnull)array;

/*!
 @brief Returns the least value present in <code>array</code>, using the same rules of comparison as <code>Math.min(double, double)</code>
 .
 @param array a  <i> nonempty </i>  array of <code>double</code>  values
 @return the value present in <code>array</code> that is less than or equal to every other value in
      the array
 @throw IllegalArgumentExceptionif <code>array</code> is empty
 */
+ (jdouble)minWithDoubleArray:(IOSDoubleArray * __nonnull)array;

/*!
 @brief Reverses the elements of <code>array</code>.This is equivalent to <code>Collections.reverse(Doubles.asList(array))</code>
 , but is likely to be more efficient.
 @since 23.1
 */
+ (void)reverseWithDoubleArray:(IOSDoubleArray * __nonnull)array;

/*!
 @brief Reverses the elements of <code>array</code> between <code>fromIndex</code> inclusive and <code>toIndex</code>
  exclusive.This is equivalent to <code>Collections.reverse(Doubles.asList(array).subList(fromIndex, toIndex))</code>
 , but is likely to be
  more efficient.
 @throw IndexOutOfBoundsExceptionif <code>fromIndex < 0</code>, <code>toIndex > array.length</code>, or
      <code>toIndex > fromIndex</code>
 @since 23.1
 */
+ (void)reverseWithDoubleArray:(IOSDoubleArray * __nonnull)array
                       withInt:(jint)fromIndex
                       withInt:(jint)toIndex;

/*!
 @brief Sorts the elements of <code>array</code> in descending order.
 <p>Note that this method uses the total order imposed by <code>Double.compare</code>, which treats
  all NaN values as equal and 0.0 as greater than -0.0.
 @since 23.1
 */
+ (void)sortDescendingWithDoubleArray:(IOSDoubleArray * __nonnull)array;

/*!
 @brief Sorts the elements of <code>array</code> between <code>fromIndex</code> inclusive and <code>toIndex</code>
  exclusive in descending order.
 <p>Note that this method uses the total order imposed by <code>Double.compare</code>, which treats
  all NaN values as equal and 0.0 as greater than -0.0.
 @since 23.1
 */
+ (void)sortDescendingWithDoubleArray:(IOSDoubleArray * __nonnull)array
                              withInt:(jint)fromIndex
                              withInt:(jint)toIndex;

/*!
 @brief Returns a serializable converter object that converts between strings and doubles using <code>Double.valueOf</code>
  and <code>Double.toString()</code>.
 @since 16.0
 */
+ (ComGoogleCommonBaseConverter *)stringConverter;

/*!
 @brief Returns an array containing each value of <code>collection</code>, converted to a <code>double</code>
  value in the manner of <code>Number.doubleValue</code>.
 <p>Elements are copied from the argument collection as if by <code>collection.toArray()</code>.
  Calling this method is as thread-safe as calling that method.
 @param collection a collection of <code>Number</code>  instances
 @return an array containing the same values as <code>collection</code>, in the same order, converted
      to primitives
 @throw NullPointerExceptionif <code>collection</code> or any of its elements is null
 @since 1.0 (parameter was <code>Collection<Double></code> before 12.0)
 */
+ (IOSDoubleArray *)toArrayWithJavaUtilCollection:(id<JavaUtilCollection> __nonnull)collection;

/*!
 @brief Parses the specified string as a double-precision floating point value.The ASCII character 
 <code>'-'</code> (<code>'&#92;u002D'</code>) is recognized as the minus sign.
 <p>Unlike <code>Double.parseDouble(String)</code>, this method returns <code>null</code> instead of
  throwing an exception if parsing fails. Valid inputs are exactly those accepted by <code>Double.valueOf(String)</code>
 , except that leading and trailing whitespace is not permitted. 
 <p>This implementation is likely to be faster than <code>Double.parseDouble</code> if many failures
  are expected.
 @param string the string representation of a <code>double</code>  value
 @return the floating point value represented by <code>string</code>, or <code>null</code> if <code>string</code>
  has a length of zero or cannot be parsed as a <code>double</code> value
 @since 14.0
 */
+ (JavaLangDouble * __nullable)tryParseWithNSString:(NSString * __nonnull)string;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonPrimitivesDoubles)

/*!
 @brief The number of bytes required to represent a primitive <code>double</code> value.
 <p><b>Java 8 users:</b> use <code>Double.BYTES</code> instead.
 @since 10.0
 */
inline jint ComGoogleCommonPrimitivesDoubles_get_BYTES(void);
#define ComGoogleCommonPrimitivesDoubles_BYTES 8
J2OBJC_STATIC_FIELD_CONSTANT(ComGoogleCommonPrimitivesDoubles, BYTES, jint)

/*!
 @brief This is adapted from the regex suggested by <code>Double.valueOf(String)</code> for prevalidating
  inputs.All valid inputs must pass this regex, but it's semantically fine if not all inputs
  that pass this regex are valid -- only a performance hit is incurred, not a semantics bug.
 */
inline JavaUtilRegexPattern *ComGoogleCommonPrimitivesDoubles_get_FLOATING_POINT_PATTERN(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilRegexPattern *ComGoogleCommonPrimitivesDoubles_FLOATING_POINT_PATTERN;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonPrimitivesDoubles, FLOATING_POINT_PATTERN, JavaUtilRegexPattern *)

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesDoubles_hashCodeWithDouble_(jdouble value);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesDoubles_compareWithDouble_withDouble_(jdouble a, jdouble b);

FOUNDATION_EXPORT jboolean ComGoogleCommonPrimitivesDoubles_isFiniteWithDouble_(jdouble value);

FOUNDATION_EXPORT jboolean ComGoogleCommonPrimitivesDoubles_containsWithDoubleArray_withDouble_(IOSDoubleArray *array, jdouble target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesDoubles_indexOfWithDoubleArray_withDouble_(IOSDoubleArray *array, jdouble target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesDoubles_indexOfWithDoubleArray_withDoubleArray_(IOSDoubleArray *array, IOSDoubleArray *target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesDoubles_lastIndexOfWithDoubleArray_withDouble_(IOSDoubleArray *array, jdouble target);

FOUNDATION_EXPORT jdouble ComGoogleCommonPrimitivesDoubles_minWithDoubleArray_(IOSDoubleArray *array);

FOUNDATION_EXPORT jdouble ComGoogleCommonPrimitivesDoubles_maxWithDoubleArray_(IOSDoubleArray *array);

FOUNDATION_EXPORT jdouble ComGoogleCommonPrimitivesDoubles_constrainToRangeWithDouble_withDouble_withDouble_(jdouble value, jdouble min, jdouble max);

FOUNDATION_EXPORT IOSDoubleArray *ComGoogleCommonPrimitivesDoubles_concatWithDoubleArray2_(IOSObjectArray *arrays);

FOUNDATION_EXPORT ComGoogleCommonBaseConverter *ComGoogleCommonPrimitivesDoubles_stringConverter(void);

FOUNDATION_EXPORT IOSDoubleArray *ComGoogleCommonPrimitivesDoubles_ensureCapacityWithDoubleArray_withInt_withInt_(IOSDoubleArray *array, jint minLength, jint padding);

FOUNDATION_EXPORT NSString *ComGoogleCommonPrimitivesDoubles_joinWithNSString_withDoubleArray_(NSString *separator, IOSDoubleArray *array);

FOUNDATION_EXPORT id<JavaUtilComparator> ComGoogleCommonPrimitivesDoubles_lexicographicalComparator(void);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesDoubles_sortDescendingWithDoubleArray_(IOSDoubleArray *array);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesDoubles_sortDescendingWithDoubleArray_withInt_withInt_(IOSDoubleArray *array, jint fromIndex, jint toIndex);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesDoubles_reverseWithDoubleArray_(IOSDoubleArray *array);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesDoubles_reverseWithDoubleArray_withInt_withInt_(IOSDoubleArray *array, jint fromIndex, jint toIndex);

FOUNDATION_EXPORT IOSDoubleArray *ComGoogleCommonPrimitivesDoubles_toArrayWithJavaUtilCollection_(id<JavaUtilCollection> collection);

FOUNDATION_EXPORT id<JavaUtilList> ComGoogleCommonPrimitivesDoubles_asListWithDoubleArray_(IOSDoubleArray *backingArray);

FOUNDATION_EXPORT JavaLangDouble *ComGoogleCommonPrimitivesDoubles_tryParseWithNSString_(NSString *string);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonPrimitivesDoubles)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonPrimitivesDoubles")
