//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/lang/IllegalStateException.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaLangIllegalStateException")
#ifdef RESTRICT_JavaLangIllegalStateException
#define INCLUDE_ALL_JavaLangIllegalStateException 0
#else
#define INCLUDE_ALL_JavaLangIllegalStateException 1
#endif
#undef RESTRICT_JavaLangIllegalStateException

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaLangIllegalStateException_) && (INCLUDE_ALL_JavaLangIllegalStateException || defined(INCLUDE_JavaLangIllegalStateException))
#define JavaLangIllegalStateException_

#define RESTRICT_JavaLangRuntimeException 1
#define INCLUDE_JavaLangRuntimeException 1
#include "java/lang/RuntimeException.h"

@class JavaLangThrowable;

/*!
 @brief Signals that a method has been invoked at an illegal or
  inappropriate time.In other words, the Java environment or
  Java application is not in an appropriate state for the requested
  operation.
 @author Jonni Kanerva
 @since JDK1.1
 */
@interface JavaLangIllegalStateException : JavaLangRuntimeException
@property (readonly, class) jlong serialVersionUID NS_SWIFT_NAME(serialVersionUID);

+ (jlong)serialVersionUID;

#pragma mark Public

/*!
 @brief Constructs an IllegalStateException with no detail message.
 A detail message is a String that describes this particular exception.
 */
- (instancetype __nonnull)init;

/*!
 @brief Constructs an IllegalStateException with the specified detail
  message.A detail message is a String that describes this particular
  exception.
 @param s the String that contains a detailed message
 */
- (instancetype __nonnull)initWithNSString:(NSString *)s;

/*!
 @brief Constructs a new exception with the specified detail message and
  cause.
 <p>Note that the detail message associated with <code>cause</code> is 
 <i>not</i> automatically incorporated in this exception's detail
  message.
 @param message the detail message (which is saved for later retrieval          by the 
 <code>Throwable.getMessage()</code>  method).
 @param cause the cause (which is saved for later retrieval by the          
 <code>Throwable.getCause()</code>  method).  (A  <tt> null </tt>  value          is permitted, and indicates that the cause is nonexistent or
           unknown.)
 @since 1.5
 */
- (instancetype __nonnull)initWithNSString:(NSString *)message
                     withJavaLangThrowable:(JavaLangThrowable *)cause;

/*!
 @brief Constructs a new exception with the specified cause and a detail
  message of <tt>(cause==null ?
 null : cause.toString())</tt> (which
  typically contains the class and detail message of <tt>cause</tt>).
 This constructor is useful for exceptions that are little more than
  wrappers for other throwables (for example, <code>java.security.PrivilegedActionException</code>
 ).
 @param cause the cause (which is saved for later retrieval by the          
 <code>Throwable.getCause()</code>  method).  (A  <tt> null </tt>  value is          permitted, and indicates that the cause is nonexistent or
           unknown.)
 @since 1.5
 */
- (instancetype __nonnull)initWithJavaLangThrowable:(JavaLangThrowable *)cause;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initWithNSString:(NSString *)arg0
                     withJavaLangThrowable:(JavaLangThrowable *)arg1
                               withBoolean:(jboolean)arg2
                               withBoolean:(jboolean)arg3 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaLangIllegalStateException)

inline jlong JavaLangIllegalStateException_get_serialVersionUID(void);
#define JavaLangIllegalStateException_serialVersionUID -1848914673093119416LL
J2OBJC_STATIC_FIELD_CONSTANT(JavaLangIllegalStateException, serialVersionUID, jlong)

FOUNDATION_EXPORT void JavaLangIllegalStateException_init(JavaLangIllegalStateException *self);

FOUNDATION_EXPORT JavaLangIllegalStateException *new_JavaLangIllegalStateException_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaLangIllegalStateException *create_JavaLangIllegalStateException_init(void);

FOUNDATION_EXPORT void JavaLangIllegalStateException_initWithNSString_(JavaLangIllegalStateException *self, NSString *s);

FOUNDATION_EXPORT JavaLangIllegalStateException *new_JavaLangIllegalStateException_initWithNSString_(NSString *s) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaLangIllegalStateException *create_JavaLangIllegalStateException_initWithNSString_(NSString *s);

FOUNDATION_EXPORT void JavaLangIllegalStateException_initWithNSString_withJavaLangThrowable_(JavaLangIllegalStateException *self, NSString *message, JavaLangThrowable *cause);

FOUNDATION_EXPORT JavaLangIllegalStateException *new_JavaLangIllegalStateException_initWithNSString_withJavaLangThrowable_(NSString *message, JavaLangThrowable *cause) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaLangIllegalStateException *create_JavaLangIllegalStateException_initWithNSString_withJavaLangThrowable_(NSString *message, JavaLangThrowable *cause);

FOUNDATION_EXPORT void JavaLangIllegalStateException_initWithJavaLangThrowable_(JavaLangIllegalStateException *self, JavaLangThrowable *cause);

FOUNDATION_EXPORT JavaLangIllegalStateException *new_JavaLangIllegalStateException_initWithJavaLangThrowable_(JavaLangThrowable *cause) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaLangIllegalStateException *create_JavaLangIllegalStateException_initWithJavaLangThrowable_(JavaLangThrowable *cause);

J2OBJC_TYPE_LITERAL_HEADER(JavaLangIllegalStateException)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaLangIllegalStateException")
