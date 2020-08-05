//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/io/Closer.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonIoCloser")
#ifdef RESTRICT_ComGoogleCommonIoCloser
#define INCLUDE_ALL_ComGoogleCommonIoCloser 0
#else
#define INCLUDE_ALL_ComGoogleCommonIoCloser 1
#endif
#undef RESTRICT_ComGoogleCommonIoCloser
#ifdef INCLUDE_ComGoogleCommonIoCloser_SuppressingSuppressor
#define INCLUDE_ComGoogleCommonIoCloser_Suppressor 1
#endif
#ifdef INCLUDE_ComGoogleCommonIoCloser_LoggingSuppressor
#define INCLUDE_ComGoogleCommonIoCloser_Suppressor 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonIoCloser_) && (INCLUDE_ALL_ComGoogleCommonIoCloser || defined(INCLUDE_ComGoogleCommonIoCloser))
#define ComGoogleCommonIoCloser_

#define RESTRICT_JavaIoCloseable 1
#define INCLUDE_JavaIoCloseable 1
#include "java/io/Closeable.h"

@class IOSClass;
@class JavaLangRuntimeException;
@class JavaLangThrowable;
@protocol ComGoogleCommonIoCloser_Suppressor;

/*!
 @brief A <code>Closeable</code> that collects <code>Closeable</code> resources and closes them all when it is 
 closed.This is intended to approximately emulate the behavior of Java 7's <a href="http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html">
 try-with-resources</a> statement in JDK6-compatible code.
 Running on Java 7, code using this
  should be approximately equivalent in behavior to the same code written with try-with-resources.
  Running on Java 6, exceptions that cannot be thrown must be logged rather than being added to the
  thrown exception as a suppressed exception. 
 <p>This class is intended to be used in the following pattern: 
 @code
 Closer closer = Closer.create();
  try {
    InputStream in = closer.register(openInputStream());
    OutputStream out = closer.register(openOutputStream());
    // do stuff
  } catch (Throwable e) {
    // ensure that any checked exception types other than IOException that could be thrown are
    // provided here, e.g. throw closer.rethrow(e, CheckedException.class);
    throw closer.rethrow(e);
  } finally {
    closer.close();
  } 
 
@endcode
  
 <p>Note that this try-catch-finally block is not equivalent to a try-catch-finally block using
  try-with-resources. To get the equivalent of that, you must wrap the above code in <i>another</i>
  try block in order to catch any exception that may be thrown (including from the call to <code>close()</code>
 ).
  
 <p>This pattern ensures the following: 
 <ul>
    <li>Each <code>Closeable</code> resource that is successfully registered will be closed later.
    <li>If a <code>Throwable</code> is thrown in the try block, no exceptions that occur when attempting
        to close resources will be thrown from the finally block. The throwable from the try block
        will be thrown.   
 <li>If no exceptions or errors were thrown in the try block, the <i>first</i> exception thrown
        by an attempt to close a resource will be thrown.   
 <li>Any exception caught when attempting to close a resource that is <i>not</i> thrown (because
        another exception is already being thrown) is <i>suppressed</i>.
  </ul>
  
 <p>An exception that is suppressed is not thrown. The method of suppression used depends on the
  version of Java the code is running on: 
 <ul>
    <li><b>Java 7+:</b> Exceptions are suppressed by adding them to the exception that <i>will</i>
        be thrown using <code>Throwable.addSuppressed(Throwable)</code>.
    <li><b>Java 6:</b> Exceptions are suppressed by logging them instead. 
 </ul>
 @author Colin Decker
 @since 14.0
 */
@interface ComGoogleCommonIoCloser : NSObject < JavaIoCloseable > {
 @public
  id<ComGoogleCommonIoCloser_Suppressor> suppressor_;
}

#pragma mark Public

/*!
 @brief Closes all <code>Closeable</code> instances that have been added to this <code>Closer</code>.If an
  exception was thrown in the try block and passed to one of the <code>exceptionThrown</code> methods,
  any exceptions thrown when attempting to close a closeable will be suppressed.
 Otherwise, the 
 <i>first</i> exception to be thrown from an attempt to close a closeable will be thrown and any
  additional exceptions that are thrown after that will be suppressed.
 */
- (void)close;

/*!
 @brief Creates a new <code>Closer</code>.
 */
+ (ComGoogleCommonIoCloser *)create;

/*!
 @brief Registers the given <code>closeable</code> to be closed when this <code>Closer</code> is closed
 .
 @return the given <code>closeable</code>
 */
- (id<JavaIoCloseable>)register__WithJavaIoCloseable:(id<JavaIoCloseable> __nullable)closeable;

/*!
 @brief Stores the given throwable and rethrows it.It will be rethrown as is if it is an <code>IOException</code>
 , <code>RuntimeException</code> or <code>Error</code>.
 Otherwise, it will be rethrown wrapped
  in a <code>RuntimeException</code>. <b>Note:</b> Be sure to declare all of the checked exception
  types your try block can throw when calling an overload of this method so as to avoid losing
  the original exception type. 
 <p>This method always throws, and as such should be called as <code>throw closer.rethrow(e);</code>
  to ensure the compiler knows that it will throw.
 @return this method does not return; it always throws
 @throw IOExceptionwhen the given throwable is an IOException
 */
- (JavaLangRuntimeException *)rethrowWithJavaLangThrowable:(JavaLangThrowable * __nonnull)e;

/*!
 @brief Stores the given throwable and rethrows it.It will be rethrown as is if it is an <code>IOException</code>
 , <code>RuntimeException</code>, <code>Error</code> or a checked exception of the given type.
 Otherwise, it will be rethrown wrapped in a <code>RuntimeException</code>. <b>Note:</b> Be sure to
  declare all of the checked exception types your try block can throw when calling an overload of
  this method so as to avoid losing the original exception type. 
 <p>This method always throws, and as such should be called as <code>throw closer.rethrow(e,
  ...);</code>
  to ensure the compiler knows that it will throw.
 @return this method does not return; it always throws
 @throw IOExceptionwhen the given throwable is an IOException
 @throw Xwhen the given throwable is of the declared type X
 */
- (JavaLangRuntimeException *)rethrowWithJavaLangThrowable:(JavaLangThrowable * __nonnull)e
                                              withIOSClass:(IOSClass * __nonnull)declaredType;

/*!
 @brief Stores the given throwable and rethrows it.It will be rethrown as is if it is an <code>IOException</code>
 , <code>RuntimeException</code>, <code>Error</code> or a checked exception of either of the
  given types.
 Otherwise, it will be rethrown wrapped in a <code>RuntimeException</code>. <b>Note:</b>
  Be sure to declare all of the checked exception types your try block can throw when calling an
  overload of this method so as to avoid losing the original exception type. 
 <p>This method always throws, and as such should be called as <code>throw closer.rethrow(e,
  ...);</code>
  to ensure the compiler knows that it will throw.
 @return this method does not return; it always throws
 @throw IOExceptionwhen the given throwable is an IOException
 @throw X1when the given throwable is of the declared type X1
 @throw X2when the given throwable is of the declared type X2
 */
- (JavaLangRuntimeException *)rethrowWithJavaLangThrowable:(JavaLangThrowable * __nonnull)e
                                              withIOSClass:(IOSClass * __nonnull)declaredType1
                                              withIOSClass:(IOSClass * __nonnull)declaredType2;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonIoCloser_Suppressor:(id<ComGoogleCommonIoCloser_Suppressor> __nonnull)suppressor;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonIoCloser)

J2OBJC_FIELD_SETTER(ComGoogleCommonIoCloser, suppressor_, id<ComGoogleCommonIoCloser_Suppressor>)

FOUNDATION_EXPORT ComGoogleCommonIoCloser *ComGoogleCommonIoCloser_create(void);

FOUNDATION_EXPORT void ComGoogleCommonIoCloser_initWithComGoogleCommonIoCloser_Suppressor_(ComGoogleCommonIoCloser *self, id<ComGoogleCommonIoCloser_Suppressor> suppressor);

FOUNDATION_EXPORT ComGoogleCommonIoCloser *new_ComGoogleCommonIoCloser_initWithComGoogleCommonIoCloser_Suppressor_(id<ComGoogleCommonIoCloser_Suppressor> suppressor) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonIoCloser *create_ComGoogleCommonIoCloser_initWithComGoogleCommonIoCloser_Suppressor_(id<ComGoogleCommonIoCloser_Suppressor> suppressor);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoCloser)

#endif

#if !defined (ComGoogleCommonIoCloser_Suppressor_) && (INCLUDE_ALL_ComGoogleCommonIoCloser || defined(INCLUDE_ComGoogleCommonIoCloser_Suppressor))
#define ComGoogleCommonIoCloser_Suppressor_

@class JavaLangThrowable;
@protocol JavaIoCloseable;

/*!
 @brief Suppression strategy interface.
 */
@protocol ComGoogleCommonIoCloser_Suppressor < JavaObject >

/*!
 @brief Suppresses the given exception (<code>suppressed</code>) which was thrown when attempting to close
  the given closeable.
 <code>thrown</code> is the exception that is actually being thrown from the
  method. Implementations of this method should not throw under any circumstances.
 */
- (void)suppressWithJavaIoCloseable:(id<JavaIoCloseable> __nonnull)closeable
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)thrown
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)suppressed;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonIoCloser_Suppressor)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoCloser_Suppressor)

#endif

#if !defined (ComGoogleCommonIoCloser_LoggingSuppressor_) && (INCLUDE_ALL_ComGoogleCommonIoCloser || defined(INCLUDE_ComGoogleCommonIoCloser_LoggingSuppressor))
#define ComGoogleCommonIoCloser_LoggingSuppressor_

@class JavaLangThrowable;
@protocol JavaIoCloseable;

/*!
 @brief Suppresses exceptions by logging them.
 */
@interface ComGoogleCommonIoCloser_LoggingSuppressor : NSObject < ComGoogleCommonIoCloser_Suppressor >
@property (readonly, class, strong) ComGoogleCommonIoCloser_LoggingSuppressor *INSTANCE NS_SWIFT_NAME(INSTANCE);

+ (ComGoogleCommonIoCloser_LoggingSuppressor *)INSTANCE;

#pragma mark Public

- (void)suppressWithJavaIoCloseable:(id<JavaIoCloseable> __nonnull)closeable
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)thrown
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)suppressed;

#pragma mark Package-Private

- (instancetype __nonnull)init;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonIoCloser_LoggingSuppressor)

inline ComGoogleCommonIoCloser_LoggingSuppressor *ComGoogleCommonIoCloser_LoggingSuppressor_get_INSTANCE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonIoCloser_LoggingSuppressor *ComGoogleCommonIoCloser_LoggingSuppressor_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonIoCloser_LoggingSuppressor, INSTANCE, ComGoogleCommonIoCloser_LoggingSuppressor *)

FOUNDATION_EXPORT void ComGoogleCommonIoCloser_LoggingSuppressor_init(ComGoogleCommonIoCloser_LoggingSuppressor *self);

FOUNDATION_EXPORT ComGoogleCommonIoCloser_LoggingSuppressor *new_ComGoogleCommonIoCloser_LoggingSuppressor_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonIoCloser_LoggingSuppressor *create_ComGoogleCommonIoCloser_LoggingSuppressor_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoCloser_LoggingSuppressor)

#endif

#if !defined (ComGoogleCommonIoCloser_SuppressingSuppressor_) && (INCLUDE_ALL_ComGoogleCommonIoCloser || defined(INCLUDE_ComGoogleCommonIoCloser_SuppressingSuppressor))
#define ComGoogleCommonIoCloser_SuppressingSuppressor_

@class JavaLangReflectMethod;
@class JavaLangThrowable;
@protocol JavaIoCloseable;

/*!
 @brief Suppresses exceptions by adding them to the exception that will be thrown using JDK7's
  addSuppressed(Throwable) mechanism.
 */
@interface ComGoogleCommonIoCloser_SuppressingSuppressor : NSObject < ComGoogleCommonIoCloser_Suppressor >
@property (readonly, class, strong) ComGoogleCommonIoCloser_SuppressingSuppressor *INSTANCE NS_SWIFT_NAME(INSTANCE);
@property (readonly, nonatomic, getter=getAddSuppressed, class, strong) JavaLangReflectMethod *addSuppressed NS_SWIFT_NAME(addSuppressed);

+ (ComGoogleCommonIoCloser_SuppressingSuppressor *)INSTANCE;

#pragma mark Public

- (void)suppressWithJavaIoCloseable:(id<JavaIoCloseable> __nonnull)closeable
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)thrown
              withJavaLangThrowable:(JavaLangThrowable * __nonnull)suppressed;

#pragma mark Package-Private

- (instancetype __nonnull)init;

+ (jboolean)isAvailable;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonIoCloser_SuppressingSuppressor)

inline ComGoogleCommonIoCloser_SuppressingSuppressor *ComGoogleCommonIoCloser_SuppressingSuppressor_get_INSTANCE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonIoCloser_SuppressingSuppressor *ComGoogleCommonIoCloser_SuppressingSuppressor_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonIoCloser_SuppressingSuppressor, INSTANCE, ComGoogleCommonIoCloser_SuppressingSuppressor *)

inline JavaLangReflectMethod *ComGoogleCommonIoCloser_SuppressingSuppressor_get_addSuppressed(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaLangReflectMethod *ComGoogleCommonIoCloser_SuppressingSuppressor_addSuppressed;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonIoCloser_SuppressingSuppressor, addSuppressed, JavaLangReflectMethod *)

FOUNDATION_EXPORT void ComGoogleCommonIoCloser_SuppressingSuppressor_init(ComGoogleCommonIoCloser_SuppressingSuppressor *self);

FOUNDATION_EXPORT ComGoogleCommonIoCloser_SuppressingSuppressor *new_ComGoogleCommonIoCloser_SuppressingSuppressor_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonIoCloser_SuppressingSuppressor *create_ComGoogleCommonIoCloser_SuppressingSuppressor_init(void);

FOUNDATION_EXPORT jboolean ComGoogleCommonIoCloser_SuppressingSuppressor_isAvailable(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonIoCloser_SuppressingSuppressor)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonIoCloser")
