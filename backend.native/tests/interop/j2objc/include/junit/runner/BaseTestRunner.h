//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/junit/build_result/java/junit/runner/BaseTestRunner.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JunitRunnerBaseTestRunner")
#ifdef RESTRICT_JunitRunnerBaseTestRunner
#define INCLUDE_ALL_JunitRunnerBaseTestRunner 0
#else
#define INCLUDE_ALL_JunitRunnerBaseTestRunner 1
#endif
#undef RESTRICT_JunitRunnerBaseTestRunner

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JunitRunnerBaseTestRunner_) && (INCLUDE_ALL_JunitRunnerBaseTestRunner || defined(INCLUDE_JunitRunnerBaseTestRunner))
#define JunitRunnerBaseTestRunner_

#define RESTRICT_JunitFrameworkTestListener 1
#define INCLUDE_JunitFrameworkTestListener 1
#include "junit/framework/TestListener.h"

@class IOSClass;
@class IOSObjectArray;
@class JavaLangThrowable;
@class JavaUtilProperties;
@class JunitFrameworkAssertionFailedError;
@protocol JunitFrameworkTest;

/*!
 @brief Base class for all test runners.
 This class was born live on stage in Sardinia during XP2000.
 */
@interface JunitRunnerBaseTestRunner : NSObject < JunitFrameworkTestListener > {
 @public
  jboolean fLoading_;
}
@property (readonly, copy, class) NSString *SUITE_METHODNAME NS_SWIFT_NAME(SUITE_METHODNAME);
@property (class) jint fgMaxMessageLength NS_SWIFT_NAME(fgMaxMessageLength);
@property (class) jboolean fgFilterStack NS_SWIFT_NAME(fgFilterStack);

+ (NSString *)SUITE_METHODNAME;

+ (jint)fgMaxMessageLength;

+ (void)setFgMaxMessageLength:(jint)value;

+ (jboolean)fgFilterStack;

+ (void)setFgFilterStack:(jboolean)value;

#pragma mark Public

- (instancetype __nonnull)init;

- (void)addErrorWithJunitFrameworkTest:(id<JunitFrameworkTest>)test
                 withJavaLangThrowable:(JavaLangThrowable *)t;

- (void)addFailureWithJunitFrameworkTest:(id<JunitFrameworkTest>)test
  withJunitFrameworkAssertionFailedError:(JunitFrameworkAssertionFailedError *)t;

/*!
 @brief Returns the formatted string of the elapsed time.
 */
- (NSString *)elapsedTimeAsStringWithLong:(jlong)runTime;

- (void)endTestWithJunitFrameworkTest:(id<JunitFrameworkTest>)test;

/*!
 @brief Extract the class name from a String in VA/Java style
 */
- (NSString *)extractClassNameWithNSString:(NSString *)className_;

/*!
 @brief Filters stack frames from internal JUnit classes
 */
+ (NSString *)getFilteredTraceWithNSString:(NSString *)stack;

/*!
 @brief Returns a filtered stack trace
 */
+ (NSString *)getFilteredTraceWithJavaLangThrowable:(JavaLangThrowable *)t;

+ (NSString *)getPreferenceWithNSString:(NSString *)key;

+ (jint)getPreferenceWithNSString:(NSString *)key
                          withInt:(jint)dflt;

/*!
 @brief Returns the Test corresponding to the given suite.This is
  a template method, subclasses override runFailed(), clearStatus().
 */
- (id<JunitFrameworkTest>)getTestWithNSString:(NSString *)suiteClassName;

+ (void)savePreferences;

/*!
 @brief Sets the loading behaviour of the test runner
 */
- (void)setLoadingWithBoolean:(jboolean)enable;

+ (void)setPreferenceWithNSString:(NSString *)key
                     withNSString:(NSString *)value;

- (void)startTestWithJunitFrameworkTest:(id<JunitFrameworkTest>)test;

- (void)testEndedWithNSString:(NSString *)testName;

- (void)testFailedWithInt:(jint)status
   withJunitFrameworkTest:(id<JunitFrameworkTest>)test
    withJavaLangThrowable:(JavaLangThrowable *)t;

- (void)testStartedWithNSString:(NSString *)testName;

/*!
 @brief Truncates a String to the maximum length.
 */
+ (NSString *)truncateWithNSString:(NSString *)s;

#pragma mark Protected

/*!
 @brief Clears the status message.
 */
- (void)clearStatus;

+ (JavaUtilProperties *)getPreferences;

/*!
 @brief Returns the loaded Class for a suite name.
 */
- (IOSClass *)loadSuiteClassWithNSString:(NSString *)suiteClassName;

/*!
 @brief Processes the command line arguments and
  returns the name of the suite class to run or null
 */
- (NSString *)processArgumentsWithNSStringArray:(IOSObjectArray *)args;

/*!
 @brief Override to define how to handle a failed loading of
  a test suite.
 */
- (void)runFailedWithNSString:(NSString *)message;

+ (void)setPreferencesWithJavaUtilProperties:(JavaUtilProperties *)preferences;

+ (jboolean)showStackRaw;

- (jboolean)useReloadingTestSuiteLoader;

#pragma mark Package-Private

+ (jboolean)filterLineWithNSString:(NSString *)line;

@end

J2OBJC_STATIC_INIT(JunitRunnerBaseTestRunner)

inline NSString *JunitRunnerBaseTestRunner_get_SUITE_METHODNAME(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT NSString *JunitRunnerBaseTestRunner_SUITE_METHODNAME;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JunitRunnerBaseTestRunner, SUITE_METHODNAME, NSString *)

inline jint JunitRunnerBaseTestRunner_get_fgMaxMessageLength(void);
inline jint JunitRunnerBaseTestRunner_set_fgMaxMessageLength(jint value);
inline jint *JunitRunnerBaseTestRunner_getRef_fgMaxMessageLength(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT jint JunitRunnerBaseTestRunner_fgMaxMessageLength;
J2OBJC_STATIC_FIELD_PRIMITIVE(JunitRunnerBaseTestRunner, fgMaxMessageLength, jint)

inline jboolean JunitRunnerBaseTestRunner_get_fgFilterStack(void);
inline jboolean JunitRunnerBaseTestRunner_set_fgFilterStack(jboolean value);
inline jboolean *JunitRunnerBaseTestRunner_getRef_fgFilterStack(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT jboolean JunitRunnerBaseTestRunner_fgFilterStack;
J2OBJC_STATIC_FIELD_PRIMITIVE(JunitRunnerBaseTestRunner, fgFilterStack, jboolean)

FOUNDATION_EXPORT void JunitRunnerBaseTestRunner_init(JunitRunnerBaseTestRunner *self);

FOUNDATION_EXPORT void JunitRunnerBaseTestRunner_setPreferencesWithJavaUtilProperties_(JavaUtilProperties *preferences);

FOUNDATION_EXPORT JavaUtilProperties *JunitRunnerBaseTestRunner_getPreferences(void);

FOUNDATION_EXPORT void JunitRunnerBaseTestRunner_savePreferences(void);

FOUNDATION_EXPORT void JunitRunnerBaseTestRunner_setPreferenceWithNSString_withNSString_(NSString *key, NSString *value);

FOUNDATION_EXPORT NSString *JunitRunnerBaseTestRunner_truncateWithNSString_(NSString *s);

FOUNDATION_EXPORT NSString *JunitRunnerBaseTestRunner_getPreferenceWithNSString_(NSString *key);

FOUNDATION_EXPORT jint JunitRunnerBaseTestRunner_getPreferenceWithNSString_withInt_(NSString *key, jint dflt);

FOUNDATION_EXPORT NSString *JunitRunnerBaseTestRunner_getFilteredTraceWithJavaLangThrowable_(JavaLangThrowable *t);

FOUNDATION_EXPORT NSString *JunitRunnerBaseTestRunner_getFilteredTraceWithNSString_(NSString *stack);

FOUNDATION_EXPORT jboolean JunitRunnerBaseTestRunner_showStackRaw(void);

FOUNDATION_EXPORT jboolean JunitRunnerBaseTestRunner_filterLineWithNSString_(NSString *line);

J2OBJC_TYPE_LITERAL_HEADER(JunitRunnerBaseTestRunner)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JunitRunnerBaseTestRunner")
