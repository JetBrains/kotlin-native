//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/primitives/Floats.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonPrimitivesFloats")
#ifdef RESTRICT_ComGoogleCommonPrimitivesFloats
#define INCLUDE_ALL_ComGoogleCommonPrimitivesFloats 0
#else
#define INCLUDE_ALL_ComGoogleCommonPrimitivesFloats 1
#endif
#undef RESTRICT_ComGoogleCommonPrimitivesFloats

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonPrimitivesFloats_) && (INCLUDE_ALL_ComGoogleCommonPrimitivesFloats || defined(INCLUDE_ComGoogleCommonPrimitivesFloats))
#define ComGoogleCommonPrimitivesFloats_

@class ComGoogleCommonBaseConverter;
@class IOSFloatArray;
@class IOSObjectArray;
@class JavaLangFloat;
@protocol JavaUtilCollection;
@protocol JavaUtilComparator;
@protocol JavaUtilList;

/*!
 @brief Static utility methods pertaining to <code>float</code> primitives, that are not already found in
  either <code>Float</code> or <code>Arrays</code>.
 <p>See the Guava User Guide article on <a href="https://github.com/google/guava/wiki/PrimitivesExplained">
 primitive utilities</a>.
 @author Kevin Bourrillion
 @since 1.0
 */
@interface ComGoogleCommonPrimitivesFloats : NSObject
@property (readonly, class) jint BYTES NS_SWIFT_NAME(BYTES);

+ (jint)BYTES;

#pragma mark Public

/*!
 @brief Returns a fixed-size list backed by the specified array, similar to <code>Arrays.asList(Object[])</code>
 .The list supports <code>List.set(int, Object)</code>, but any attempt to
  set a value to <code>null</code> will result in a <code>NullPointerException</code>.
 <p>The returned list maintains the values, but not the identities, of <code>Float</code> objects
  written to or read from it. For example, whether <code>list.get(0) == list.get(0)</code> is true for
  the returned list is unspecified. 
 <p>The returned list may have unexpected behavior if it contains <code>NaN</code>, or if <code>NaN</code>
  is used as a parameter to any of its methods.
 @param backingArray the array to back the list
 @return a list view of the array
 */
+ (id<JavaUtilList>)asListWithFloatArray:(IOSFloatArray * __nonnull)backingArray;

/*!
 @brief Compares the two specified <code>float</code> values using <code>Float.compare(float, float)</code>.You
  may prefer to invoke that method directly; this method exists only for consistency with the
  other utilities in this package.
 <p><b>Note:</b> this method simply delegates to the JDK method <code>Float.compare</code>. It is
  provided for consistency with the other primitive types, whose compare methods were not added
  to the JDK until JDK 7.
 @param a the first <code>float</code>  to compare
 @param b the second <code>float</code>  to compare
 @return the result of invoking <code>Float.compare(float, float)</code>
 */
+ (jint)compareWithFloat:(jfloat)a
               withFloat:(jfloat)b;

/*!
 @brief Returns the values from each provided array combined into a single array.For example, <code>concat(new float[] {a, b}, new float[] {}, new float[] {c}</code>
  returns the array <code>{a, b,
  c}</code>
 .
 @param arrays zero or more <code>float</code>  arrays
 @return a single array containing all the values from the source arrays, in order
 */
+ (IOSFloatArray *)concatWithFloatArray2:(IOSObjectArray * __nonnull)arrays;

/*!
 @brief Returns the value nearest to <code>value</code> which is within the closed range <code>[min..max]</code>.
 <p>If <code>value</code> is within the range <code>[min..max]</code>, <code>value</code> is returned
  unchanged. If <code>value</code> is less than <code>min</code>, <code>min</code> is returned, and if <code>value</code>
  is greater than <code>max</code>, <code>max</code> is returned.
 @param value the <code>float</code>  value to constrain
 @param min the lower bound (inclusive) of the range to constrain <code>value</code>  to
 @param max the upper bound (inclusive) of the range to constrain <code>value</code>  to
 @throw IllegalArgumentExceptionif <code>min > max</code>
 @since 21.0
 */
+ (jfloat)constrainToRangeWithFloat:(jfloat)value
                          withFloat:(jfloat)min
                          withFloat:(jfloat)max;

/*!
 @brief Returns <code>true</code> if <code>target</code> is present as an element anywhere in <code>array</code>.Note
  that this always returns <code>false</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>float</code>  values, possibly empty
 @param target a primitive <code>float</code>  value
 @return <code>true</code> if <code>array[i] == target</code> for some value of <code>i</code>
 */
+ (jboolean)containsWithFloatArray:(IOSFloatArray * __nonnull)array
                         withFloat:(jfloat)target;

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
+ (IOSFloatArray *)ensureCapacityWithFloatArray:(IOSFloatArray * __nonnull)array
                                        withInt:(jint)minLength
                                        withInt:(jint)padding;

/*!
 @brief Returns a hash code for <code>value</code>; equal to the result of invoking <code>((Float)
  value).hashCode()</code>
 .
 <p><b>Java 8 users:</b> use <code>Float.hashCode(float)</code> instead.
 @param value a primitive <code>float</code>  value
 @return a hash code for the value
 */
+ (jint)hashCodeWithFloat:(jfloat)value;

/*!
 @brief Returns the index of the first appearance of the value <code>target</code> in <code>array</code>.Note
  that this always returns <code>-1</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>float</code>  values, possibly empty
 @param target a primitive <code>float</code>  value
 @return the least index <code>i</code> for which <code>array[i] == target</code>, or <code>-1</code> if no
      such index exists.
 */
+ (jint)indexOfWithFloatArray:(IOSFloatArray * __nonnull)array
                    withFloat:(jfloat)target;

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
+ (jint)indexOfWithFloatArray:(IOSFloatArray * __nonnull)array
               withFloatArray:(IOSFloatArray * __nonnull)target;

/*!
 @brief Returns <code>true</code> if <code>value</code> represents a real number.This is equivalent to, but not
  necessarily implemented as, <code>!
 (Float.isInfinite(value) || Float.isNaN(value))</code>.
  
 <p><b>Java 8 users:</b> use <code>Float.isFinite(float)</code> instead.
 @since 10.0
 */
+ (jboolean)isFiniteWithFloat:(jfloat)value;

/*!
 @brief Returns a string containing the supplied <code>float</code> values, converted to strings as
  specified by <code>Float.toString(float)</code>, and separated by <code>separator</code>.For example, 
 <code>join("-", 1.0f, 2.0f, 3.0f)</code> returns the string <code>"1.0-2.0-3.0"</code>.
 <p>Note that <code>Float.toString(float)</code> formats <code>float</code> differently in GWT. In the
  previous example, it returns the string <code>"1-2-3"</code>.
 @param separator the text that should appear between consecutive values in the resulting string      (but not at the start or end)
 @param array an array of <code>float</code>  values, possibly empty
 */
+ (NSString *)joinWithNSString:(NSString * __nonnull)separator
                withFloatArray:(IOSFloatArray * __nonnull)array;

/*!
 @brief Returns the index of the last appearance of the value <code>target</code> in <code>array</code>.Note
  that this always returns <code>-1</code> when <code>target</code> is <code>NaN</code>.
 @param array an array of <code>float</code>  values, possibly empty
 @param target a primitive <code>float</code>  value
 @return the greatest index <code>i</code> for which <code>array[i] == target</code>, or <code>-1</code> if no
      such index exists.
 */
+ (jint)lastIndexOfWithFloatArray:(IOSFloatArray * __nonnull)array
                        withFloat:(jfloat)target;

/*!
 @brief Returns a comparator that compares two <code>float</code> arrays <a href="http://en.wikipedia.org/wiki/Lexicographical_order">
 lexicographically</a>.That is, it
  compares, using <code>compare(float, float)</code>), the first pair of values that follow any
  common prefix, or when one array is a prefix of the other, treats the shorter array as the
  lesser.
 For example, <code>[] < [1.0f] < [1.0f, 2.0f] < [2.0f]</code>.
  
 <p>The returned comparator is inconsistent with <code>Object.equals(Object)</code> (since arrays
  support only identity equality), but it is consistent with <code>Arrays.equals(float[],
 float[])</code>
 .
 @since 2.0
 */
+ (id<JavaUtilComparator>)lexicographicalComparator;

/*!
 @brief Returns the greatest value present in <code>array</code>, using the same rules of comparison as 
 <code>Math.max(float, float)</code>.
 @param array a  <i> nonempty </i>  array of <code>float</code>  values
 @return the value present in <code>array</code> that is greater than or equal to every other value
      in the array
 @throw IllegalArgumentExceptionif <code>array</code> is empty
 */
+ (jfloat)maxWithFloatArray:(IOSFloatArray * __nonnull)array;

/*!
 @brief Returns the least value present in <code>array</code>, using the same rules of comparison as <code>Math.min(float, float)</code>
 .
 @param array a  <i> nonempty </i>  array of <code>float</code>  values
 @return the value present in <code>array</code> that is less than or equal to every other value in
      the array
 @throw IllegalArgumentExceptionif <code>array</code> is empty
 */
+ (jfloat)minWithFloatArray:(IOSFloatArray * __nonnull)array;

/*!
 @brief Reverses the elements of <code>array</code>.This is equivalent to <code>Collections.reverse(Floats.asList(array))</code>
 , but is likely to be more efficient.
 @since 23.1
 */
+ (void)reverseWithFloatArray:(IOSFloatArray * __nonnull)array;

/*!
 @brief Reverses the elements of <code>array</code> between <code>fromIndex</code> inclusive and <code>toIndex</code>
  exclusive.This is equivalent to <code>Collections.reverse(Floats.asList(array).subList(fromIndex, toIndex))</code>
 , but is likely to be
  more efficient.
 @throw IndexOutOfBoundsExceptionif <code>fromIndex < 0</code>, <code>toIndex > array.length</code>, or
      <code>toIndex > fromIndex</code>
 @since 23.1
 */
+ (void)reverseWithFloatArray:(IOSFloatArray * __nonnull)array
                      withInt:(jint)fromIndex
                      withInt:(jint)toIndex;

/*!
 @brief Sorts the elements of <code>array</code> in descending order.
 <p>Note that this method uses the total order imposed by <code>Float.compare</code>, which treats
  all NaN values as equal and 0.0 as greater than -0.0.
 @since 23.1
 */
+ (void)sortDescendingWithFloatArray:(IOSFloatArray * __nonnull)array;

/*!
 @brief Sorts the elements of <code>array</code> between <code>fromIndex</code> inclusive and <code>toIndex</code>
  exclusive in descending order.
 <p>Note that this method uses the total order imposed by <code>Float.compare</code>, which treats
  all NaN values as equal and 0.0 as greater than -0.0.
 @since 23.1
 */
+ (void)sortDescendingWithFloatArray:(IOSFloatArray * __nonnull)array
                             withInt:(jint)fromIndex
                             withInt:(jint)toIndex;

/*!
 @brief Returns a serializable converter object that converts between strings and floats using <code>Float.valueOf</code>
  and <code>Float.toString()</code>.
 @since 16.0
 */
+ (ComGoogleCommonBaseConverter *)stringConverter;

/*!
 @brief Returns an array containing each value of <code>collection</code>, converted to a <code>float</code>
  value in the manner of <code>Number.floatValue</code>.
 <p>Elements are copied from the argument collection as if by <code>collection.toArray()</code>.
  Calling this method is as thread-safe as calling that method.
 @param collection a collection of <code>Number</code>  instances
 @return an array containing the same values as <code>collection</code>, in the same order, converted
      to primitives
 @throw NullPointerExceptionif <code>collection</code> or any of its elements is null
 @since 1.0 (parameter was <code>Collection<Float></code> before 12.0)
 */
+ (IOSFloatArray *)toArrayWithJavaUtilCollection:(id<JavaUtilCollection> __nonnull)collection;

/*!
 @brief Parses the specified string as a single-precision floating point value.The ASCII character 
 <code>'-'</code> (<code>'&#92;u002D'</code>) is recognized as the minus sign.
 <p>Unlike <code>Float.parseFloat(String)</code>, this method returns <code>null</code> instead of
  throwing an exception if parsing fails. Valid inputs are exactly those accepted by <code>Float.valueOf(String)</code>
 , except that leading and trailing whitespace is not permitted. 
 <p>This implementation is likely to be faster than <code>Float.parseFloat</code> if many failures
  are expected.
 @param string the string representation of a <code>float</code>  value
 @return the floating point value represented by <code>string</code>, or <code>null</code> if <code>string</code>
  has a length of zero or cannot be parsed as a <code>float</code> value
 @since 14.0
 */
+ (JavaLangFloat * __nullable)tryParseWithNSString:(NSString * __nonnull)string;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonPrimitivesFloats)

/*!
 @brief The number of bytes required to represent a primitive <code>float</code> value.
 <p><b>Java 8 users:</b> use <code>Float.BYTES</code> instead.
 @since 10.0
 */
inline jint ComGoogleCommonPrimitivesFloats_get_BYTES(void);
#define ComGoogleCommonPrimitivesFloats_BYTES 4
J2OBJC_STATIC_FIELD_CONSTANT(ComGoogleCommonPrimitivesFloats, BYTES, jint)

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesFloats_hashCodeWithFloat_(jfloat value);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesFloats_compareWithFloat_withFloat_(jfloat a, jfloat b);

FOUNDATION_EXPORT jboolean ComGoogleCommonPrimitivesFloats_isFiniteWithFloat_(jfloat value);

FOUNDATION_EXPORT jboolean ComGoogleCommonPrimitivesFloats_containsWithFloatArray_withFloat_(IOSFloatArray *array, jfloat target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesFloats_indexOfWithFloatArray_withFloat_(IOSFloatArray *array, jfloat target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesFloats_indexOfWithFloatArray_withFloatArray_(IOSFloatArray *array, IOSFloatArray *target);

FOUNDATION_EXPORT jint ComGoogleCommonPrimitivesFloats_lastIndexOfWithFloatArray_withFloat_(IOSFloatArray *array, jfloat target);

FOUNDATION_EXPORT jfloat ComGoogleCommonPrimitivesFloats_minWithFloatArray_(IOSFloatArray *array);

FOUNDATION_EXPORT jfloat ComGoogleCommonPrimitivesFloats_maxWithFloatArray_(IOSFloatArray *array);

FOUNDATION_EXPORT jfloat ComGoogleCommonPrimitivesFloats_constrainToRangeWithFloat_withFloat_withFloat_(jfloat value, jfloat min, jfloat max);

FOUNDATION_EXPORT IOSFloatArray *ComGoogleCommonPrimitivesFloats_concatWithFloatArray2_(IOSObjectArray *arrays);

FOUNDATION_EXPORT ComGoogleCommonBaseConverter *ComGoogleCommonPrimitivesFloats_stringConverter(void);

FOUNDATION_EXPORT IOSFloatArray *ComGoogleCommonPrimitivesFloats_ensureCapacityWithFloatArray_withInt_withInt_(IOSFloatArray *array, jint minLength, jint padding);

FOUNDATION_EXPORT NSString *ComGoogleCommonPrimitivesFloats_joinWithNSString_withFloatArray_(NSString *separator, IOSFloatArray *array);

FOUNDATION_EXPORT id<JavaUtilComparator> ComGoogleCommonPrimitivesFloats_lexicographicalComparator(void);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesFloats_sortDescendingWithFloatArray_(IOSFloatArray *array);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesFloats_sortDescendingWithFloatArray_withInt_withInt_(IOSFloatArray *array, jint fromIndex, jint toIndex);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesFloats_reverseWithFloatArray_(IOSFloatArray *array);

FOUNDATION_EXPORT void ComGoogleCommonPrimitivesFloats_reverseWithFloatArray_withInt_withInt_(IOSFloatArray *array, jint fromIndex, jint toIndex);

FOUNDATION_EXPORT IOSFloatArray *ComGoogleCommonPrimitivesFloats_toArrayWithJavaUtilCollection_(id<JavaUtilCollection> collection);

FOUNDATION_EXPORT id<JavaUtilList> ComGoogleCommonPrimitivesFloats_asListWithFloatArray_(IOSFloatArray *backingArray);

FOUNDATION_EXPORT JavaLangFloat *ComGoogleCommonPrimitivesFloats_tryParseWithNSString_(NSString *string);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonPrimitivesFloats)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonPrimitivesFloats")
