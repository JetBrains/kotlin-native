//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/math/DoubleMath.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonMathDoubleMath")
#ifdef RESTRICT_ComGoogleCommonMathDoubleMath
#define INCLUDE_ALL_ComGoogleCommonMathDoubleMath 0
#else
#define INCLUDE_ALL_ComGoogleCommonMathDoubleMath 1
#endif
#undef RESTRICT_ComGoogleCommonMathDoubleMath

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonMathDoubleMath_) && (INCLUDE_ALL_ComGoogleCommonMathDoubleMath || defined(INCLUDE_ComGoogleCommonMathDoubleMath))
#define ComGoogleCommonMathDoubleMath_

@class IOSDoubleArray;
@class IOSIntArray;
@class IOSLongArray;
@class JavaMathBigInteger;
@class JavaMathRoundingMode;
@protocol JavaLangIterable;
@protocol JavaUtilIterator;

/*!
 @brief A class for arithmetic on doubles that is not covered by <code>java.lang.Math</code>.
 @author Louis Wasserman
 @since 11.0
 */
@interface ComGoogleCommonMathDoubleMath : NSObject
@property (readonly, class) jint MAX_FACTORIAL NS_SWIFT_NAME(MAX_FACTORIAL);
@property (readonly, class, strong) IOSDoubleArray *everySixteenthFactorial NS_SWIFT_NAME(everySixteenthFactorial);

+ (jint)MAX_FACTORIAL;

+ (IOSDoubleArray *)everySixteenthFactorial;

#pragma mark Public

/*!
 @brief Returns <code>n!
 </code>, that is, the product of the first <code>n</code> positive integers, <code>1</code> if 
 <code>n == 0</code>, or <code>n!</code>, or <code>Double.POSITIVE_INFINITY</code> if <code>n! >
  Double.MAX_VALUE</code>
 .
 <p>The result is within 1 ulp of the true value.
 @throw IllegalArgumentExceptionif <code>n < 0</code>
 */
+ (jdouble)factorialWithInt:(jint)n;

/*!
 @brief Compares <code>a</code> and <code>b</code> "fuzzily," with a tolerance for nearly-equal values.
 <p>This method is equivalent to <code>fuzzyEquals(a, b, tolerance) ? 0 : Double.compare(a,
  b)</code>
 . In particular, like <code>Double.compare(double, double)</code>, it treats all NaN values as
  equal and greater than all other values (including <code>Double.POSITIVE_INFINITY</code>).
  
 <p>This is <em>not</em> a total ordering and is <em>not</em> suitable for use in <code>Comparable.compareTo</code>
  implementations. In particular, it is not transitive.
 @throw IllegalArgumentExceptionif <code>tolerance</code> is <code>< 0</code> or NaN
 @since 13.0
 */
+ (jint)fuzzyCompareWithDouble:(jdouble)a
                    withDouble:(jdouble)b
                    withDouble:(jdouble)tolerance;

/*!
 @brief Returns <code>true</code> if <code>a</code> and <code>b</code> are within <code>tolerance</code> of each other.
 <p>Technically speaking, this is equivalent to <code>Math.abs(a - b) <= tolerance ||
  Double.valueOf(a).equals(Double.valueOf(b))</code>
 .
  
 <p>Notable special cases include: 
 <ul>
    <li>All NaNs are fuzzily equal.
    <li>If <code>a == b</code>, then <code>a</code> and <code>b</code> are always fuzzily equal.
    <li>Positive and negative zero are always fuzzily equal.
    <li>If <code>tolerance</code> is zero, and neither <code>a</code> nor <code>b</code> is NaN, then <code>a</code>
        and <code>b</code> are fuzzily equal if and only if <code>a == b</code>.
    <li>With <code>Double.POSITIVE_INFINITY</code> tolerance, all non-NaN values are fuzzily equal.
    <li>With finite tolerance, <code>Double.POSITIVE_INFINITY</code> and <code>Double.NEGATIVE_INFINITY</code>
  are fuzzily equal only to themselves. 
 </ul>
  
 <p>This is reflexive and symmetric, but <em>not</em> transitive, so it is <em>not</em> an
  equivalence relation and <em>not</em> suitable for use in <code>Object.equals</code>
  implementations.
 @throw IllegalArgumentExceptionif <code>tolerance</code> is <code>< 0</code> or NaN
 @since 13.0
 */
+ (jboolean)fuzzyEqualsWithDouble:(jdouble)a
                       withDouble:(jdouble)b
                       withDouble:(jdouble)tolerance;

/*!
 @brief Returns <code>true</code> if <code>x</code> represents a mathematical integer.
 <p>This is equivalent to, but not necessarily implemented as, the expression <code>!Double.isNaN(x) && !Double.isInfinite(x) && x == Math.rint(x)</code>
 .
 */
+ (jboolean)isMathematicalIntegerWithDouble:(jdouble)x;

/*!
 @brief Returns <code>true</code> if <code>x</code> is exactly equal to <code>2^k</code> for some finite integer 
 <code>k</code>.
 */
+ (jboolean)isPowerOfTwoWithDouble:(jdouble)x;

/*!
 @brief Returns the base 2 logarithm of a double value.
 <p>Special cases: 
 <ul>
    <li>If <code>x</code> is NaN or less than zero, the result is NaN.
    <li>If <code>x</code> is positive infinity, the result is positive infinity.
    <li>If <code>x</code> is positive or negative zero, the result is negative infinity. 
 </ul>
  
 <p>The computed result is within 1 ulp of the exact result. 
 <p>If the result of this method will be immediately rounded to an <code>int</code>, <code>log2(double, RoundingMode)</code>
  is faster.
 */
+ (jdouble)log2WithDouble:(jdouble)x;

/*!
 @brief Returns the base 2 logarithm of a double value, rounded with the specified rounding mode to an 
 <code>int</code>.
 <p>Regardless of the rounding mode, this is faster than <code>(int) log2(x)</code>.
 @throw IllegalArgumentExceptionif <code>x <= 0.0</code>, <code>x</code> is NaN, or <code>x</code> is
      infinite
 */
+ (jint)log2WithDouble:(jdouble)x
withJavaMathRoundingMode:(JavaMathRoundingMode * __nonnull)mode;

/*!
 @brief Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of 
 <code>values</code>.
 <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
  the arithmetic mean of the population.
 @param values a nonempty series of values
 @throw IllegalArgumentExceptionif <code>values</code> is empty or contains any non-finite value
 */
+ (jdouble)meanWithDoubleArray:(IOSDoubleArray * __nonnull)values __attribute__((deprecated));

/*!
 @brief Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of 
 <code>values</code>.
 <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
  the arithmetic mean of the population.
 @param values a nonempty series of values
 @throw IllegalArgumentExceptionif <code>values</code> is empty
 */
+ (jdouble)meanWithIntArray:(IOSIntArray * __nonnull)values __attribute__((deprecated));

/*!
 @brief Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of 
 <code>values</code>.
 <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
  the arithmetic mean of the population.
 @param values a nonempty series of values, which will be converted to <code>double</code>  values
       (this may cause loss of precision)
 @throw IllegalArgumentExceptionif <code>values</code> is empty or contains any non-finite value
 */
+ (jdouble)meanWithJavaLangIterable:(id<JavaLangIterable> __nonnull)values __attribute__((deprecated));

/*!
 @brief Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of 
 <code>values</code>.
 <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
  the arithmetic mean of the population.
 @param values a nonempty series of values, which will be converted to <code>double</code>  values
       (this may cause loss of precision)
 @throw IllegalArgumentExceptionif <code>values</code> is empty or contains any non-finite value
 */
+ (jdouble)meanWithJavaUtilIterator:(id<JavaUtilIterator> __nonnull)values __attribute__((deprecated));

/*!
 @brief Returns the <a href="http://en.wikipedia.org/wiki/Arithmetic_mean">arithmetic mean</a> of 
 <code>values</code>.
 <p>If these values are a sample drawn from a population, this is also an unbiased estimator of
  the arithmetic mean of the population.
 @param values a nonempty series of values, which will be converted to <code>double</code>  values
       (this may cause loss of precision for longs of magnitude over 2^53 (slightly over 9e15))
 @throw IllegalArgumentExceptionif <code>values</code> is empty
 */
+ (jdouble)meanWithLongArray:(IOSLongArray * __nonnull)values __attribute__((deprecated));

/*!
 @brief Returns the <code>BigInteger</code> value that is equal to <code>x</code> rounded with the specified
  rounding mode, if possible.
 @throw ArithmeticExceptionif
      <ul>
        <li><code>x</code> is infinite or NaN
        <li><code>x</code> is not a mathematical integer and <code>mode</code> is <code>RoundingMode.UNNECESSARY</code>
      </ul>
 */
+ (JavaMathBigInteger *)roundToBigIntegerWithDouble:(jdouble)x
                           withJavaMathRoundingMode:(JavaMathRoundingMode * __nonnull)mode;

/*!
 @brief Returns the <code>int</code> value that is equal to <code>x</code> rounded with the specified rounding
  mode, if possible.
 @throw ArithmeticExceptionif
      <ul>
        <li><code>x</code> is infinite or NaN
        <li><code>x</code>, after being rounded to a mathematical integer using the specified rounding
            mode, is either less than <code>Integer.MIN_VALUE</code> or greater than <code>Integer.MAX_VALUE</code>
        <li><code>x</code> is not a mathematical integer and <code>mode</code> is <code>RoundingMode.UNNECESSARY</code>
      </ul>
 */
+ (jint)roundToIntWithDouble:(jdouble)x
    withJavaMathRoundingMode:(JavaMathRoundingMode * __nonnull)mode;

/*!
 @brief Returns the <code>long</code> value that is equal to <code>x</code> rounded with the specified rounding
  mode, if possible.
 @throw ArithmeticExceptionif
      <ul>
        <li><code>x</code> is infinite or NaN
        <li><code>x</code>, after being rounded to a mathematical integer using the specified rounding
            mode, is either less than <code>Long.MIN_VALUE</code> or greater than <code>Long.MAX_VALUE</code>
        <li><code>x</code> is not a mathematical integer and <code>mode</code> is <code>RoundingMode.UNNECESSARY</code>
      </ul>
 */
+ (jlong)roundToLongWithDouble:(jdouble)x
      withJavaMathRoundingMode:(JavaMathRoundingMode * __nonnull)mode;

#pragma mark Package-Private

+ (jdouble)roundIntermediateWithDouble:(jdouble)x
              withJavaMathRoundingMode:(JavaMathRoundingMode * __nonnull)mode;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonMathDoubleMath)

inline jint ComGoogleCommonMathDoubleMath_get_MAX_FACTORIAL(void);
#define ComGoogleCommonMathDoubleMath_MAX_FACTORIAL 170
J2OBJC_STATIC_FIELD_CONSTANT(ComGoogleCommonMathDoubleMath, MAX_FACTORIAL, jint)

inline IOSDoubleArray *ComGoogleCommonMathDoubleMath_get_everySixteenthFactorial(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT IOSDoubleArray *ComGoogleCommonMathDoubleMath_everySixteenthFactorial;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonMathDoubleMath, everySixteenthFactorial, IOSDoubleArray *)

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_roundIntermediateWithDouble_withJavaMathRoundingMode_(jdouble x, JavaMathRoundingMode *mode);

FOUNDATION_EXPORT jint ComGoogleCommonMathDoubleMath_roundToIntWithDouble_withJavaMathRoundingMode_(jdouble x, JavaMathRoundingMode *mode);

FOUNDATION_EXPORT jlong ComGoogleCommonMathDoubleMath_roundToLongWithDouble_withJavaMathRoundingMode_(jdouble x, JavaMathRoundingMode *mode);

FOUNDATION_EXPORT JavaMathBigInteger *ComGoogleCommonMathDoubleMath_roundToBigIntegerWithDouble_withJavaMathRoundingMode_(jdouble x, JavaMathRoundingMode *mode);

FOUNDATION_EXPORT jboolean ComGoogleCommonMathDoubleMath_isPowerOfTwoWithDouble_(jdouble x);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_log2WithDouble_(jdouble x);

FOUNDATION_EXPORT jint ComGoogleCommonMathDoubleMath_log2WithDouble_withJavaMathRoundingMode_(jdouble x, JavaMathRoundingMode *mode);

FOUNDATION_EXPORT jboolean ComGoogleCommonMathDoubleMath_isMathematicalIntegerWithDouble_(jdouble x);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_factorialWithInt_(jint n);

FOUNDATION_EXPORT jboolean ComGoogleCommonMathDoubleMath_fuzzyEqualsWithDouble_withDouble_withDouble_(jdouble a, jdouble b, jdouble tolerance);

FOUNDATION_EXPORT jint ComGoogleCommonMathDoubleMath_fuzzyCompareWithDouble_withDouble_withDouble_(jdouble a, jdouble b, jdouble tolerance);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_meanWithDoubleArray_(IOSDoubleArray *values);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_meanWithIntArray_(IOSIntArray *values);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_meanWithLongArray_(IOSLongArray *values);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_meanWithJavaLangIterable_(id<JavaLangIterable> values);

FOUNDATION_EXPORT jdouble ComGoogleCommonMathDoubleMath_meanWithJavaUtilIterator_(id<JavaUtilIterator> values);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonMathDoubleMath)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonMathDoubleMath")
