//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/util/function/BinaryOperator.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaUtilFunctionBinaryOperator")
#ifdef RESTRICT_JavaUtilFunctionBinaryOperator
#define INCLUDE_ALL_JavaUtilFunctionBinaryOperator 0
#else
#define INCLUDE_ALL_JavaUtilFunctionBinaryOperator 1
#endif
#undef RESTRICT_JavaUtilFunctionBinaryOperator

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaUtilFunctionBinaryOperator_) && (INCLUDE_ALL_JavaUtilFunctionBinaryOperator || defined(INCLUDE_JavaUtilFunctionBinaryOperator))
#define JavaUtilFunctionBinaryOperator_

#define RESTRICT_JavaUtilFunctionBiFunction 1
#define INCLUDE_JavaUtilFunctionBiFunction 1
#include "java/util/function/BiFunction.h"

@protocol JavaUtilComparator;

/*!
 @brief Represents an operation upon two operands of the same type, producing a result
  of the same type as the operands.This is a specialization of 
 <code>BiFunction</code> for the case where the operands and the result are all of
  the same type.
 <p>This is a <a href="package-summary.html">functional interface</a>
  whose functional method is <code>apply(Object, Object)</code>.
 - seealso: BiFunction
 - seealso: UnaryOperator
 @since 1.8
 */
@protocol JavaUtilFunctionBinaryOperator < JavaUtilFunctionBiFunction, JavaObject >

@end

@interface JavaUtilFunctionBinaryOperator : NSObject

/*!
 @brief Returns a <code>BinaryOperator</code> which returns the lesser of two elements
  according to the specified <code>Comparator</code>.
 @param comparator a <code>Comparator</code>  for comparing the two values
 @return a <code>BinaryOperator</code> which returns the lesser of its operands,
          according to the supplied <code>Comparator</code>
 @throw NullPointerExceptionif the argument is null
 */
+ (id<JavaUtilFunctionBinaryOperator>)minByWithJavaUtilComparator:(id<JavaUtilComparator>)comparator;

/*!
 @brief Returns a <code>BinaryOperator</code> which returns the greater of two elements
  according to the specified <code>Comparator</code>.
 @param comparator a <code>Comparator</code>  for comparing the two values
 @return a <code>BinaryOperator</code> which returns the greater of its operands,
          according to the supplied <code>Comparator</code>
 @throw NullPointerExceptionif the argument is null
 */
+ (id<JavaUtilFunctionBinaryOperator>)maxByWithJavaUtilComparator:(id<JavaUtilComparator>)comparator;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilFunctionBinaryOperator)

FOUNDATION_EXPORT id<JavaUtilFunctionBinaryOperator> JavaUtilFunctionBinaryOperator_minByWithJavaUtilComparator_(id<JavaUtilComparator> comparator);

FOUNDATION_EXPORT id<JavaUtilFunctionBinaryOperator> JavaUtilFunctionBinaryOperator_maxByWithJavaUtilComparator_(id<JavaUtilComparator> comparator);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilFunctionBinaryOperator)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaUtilFunctionBinaryOperator")
