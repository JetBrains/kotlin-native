//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/junit/build_result/java/junit/textui/TestRunner.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JunitTextuiTestRunner")
#ifdef RESTRICT_JunitTextuiTestRunner
#define INCLUDE_ALL_JunitTextuiTestRunner 0
#else
#define INCLUDE_ALL_JunitTextuiTestRunner 1
#endif
#undef RESTRICT_JunitTextuiTestRunner

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JunitTextuiTestRunner_) && (INCLUDE_ALL_JunitTextuiTestRunner || defined(INCLUDE_JunitTextuiTestRunner))
#define JunitTextuiTestRunner_

#define RESTRICT_JunitRunnerBaseTestRunner 1
#define INCLUDE_JunitRunnerBaseTestRunner 1
#include "junit/runner/BaseTestRunner.h"

@class IOSClass;
@class IOSObjectArray;
@class JavaIoPrintStream;
@class JavaLangThrowable;
@class JunitFrameworkTestResult;
@class JunitTextuiResultPrinter;
@protocol JunitFrameworkTest;

/*!
 @brief A command line based tool to run tests.
 @code

  java junit.textui.TestRunner [-wait] TestCaseClass 
  
@endcode
  
 <p>TestRunner expects the name of a TestCase class as argument.
  If this class defines a static <code>suite</code> method it
  will be invoked and the returned test is run. Otherwise all
  the methods starting with "test" having no arguments are run.</p>
  
 <p> When the wait command line argument is given TestRunner
  waits until the users types RETURN.</p>
  
 <p>TestRunner prints a trace as the tests are executed followed by a
  summary at the end.</p>
 */
@interface JunitTextuiTestRunner : JunitRunnerBaseTestRunner
@property (readonly, class) jint SUCCESS_EXIT NS_SWIFT_NAME(SUCCESS_EXIT);
@property (readonly, class) jint FAILURE_EXIT NS_SWIFT_NAME(FAILURE_EXIT);
@property (readonly, class) jint EXCEPTION_EXIT NS_SWIFT_NAME(EXCEPTION_EXIT);

+ (jint)SUCCESS_EXIT;

+ (jint)FAILURE_EXIT;

+ (jint)EXCEPTION_EXIT;

#pragma mark Public

/*!
 @brief Constructs a TestRunner.
 */
- (instancetype __nonnull)init;

/*!
 @brief Constructs a TestRunner using the given stream for all the output
 */
- (instancetype __nonnull)initWithJavaIoPrintStream:(JavaIoPrintStream *)writer;

/*!
 @brief Constructs a TestRunner using the given ResultPrinter all the output
 */
- (instancetype __nonnull)initWithJunitTextuiResultPrinter:(JunitTextuiResultPrinter *)printer;

- (JunitFrameworkTestResult *)doRunWithJunitFrameworkTest:(id<JunitFrameworkTest>)test;

- (JunitFrameworkTestResult *)doRunWithJunitFrameworkTest:(id<JunitFrameworkTest>)suite
                                              withBoolean:(jboolean)wait;

+ (void)mainWithNSStringArray:(IOSObjectArray *)args;

/*!
 @brief Runs a suite extracted from a TestCase subclass.
 */
+ (void)runWithIOSClass:(IOSClass *)testClass;

/*!
 @brief Runs a single test and collects its results.
 This method can be used to start a test run
  from your program. 
 @code

  public static void main (String[] args) {
     test.textui.TestRunner.run(suite());
  } 
  
@endcode
 */
+ (JunitFrameworkTestResult *)runWithJunitFrameworkTest:(id<JunitFrameworkTest>)test;

/*!
 @brief Runs a single test and waits until the user
  types RETURN.
 */
+ (void)runAndWaitWithJunitFrameworkTest:(id<JunitFrameworkTest>)suite;

- (void)setPrinterWithJunitTextuiResultPrinter:(JunitTextuiResultPrinter *)printer;

/*!
 @brief Starts a test run.Analyzes the command line arguments and runs the given
  test suite.
 */
- (JunitFrameworkTestResult *)startWithNSStringArray:(IOSObjectArray *)args;

- (void)testEndedWithNSString:(NSString *)testName;

- (void)testFailedWithInt:(jint)status
   withJunitFrameworkTest:(id<JunitFrameworkTest>)test
    withJavaLangThrowable:(JavaLangThrowable *)t;

- (void)testStartedWithNSString:(NSString *)testName;

#pragma mark Protected

/*!
 @brief Creates the TestResult to be used for the test run.
 */
- (JunitFrameworkTestResult *)createTestResult;

- (void)pauseWithBoolean:(jboolean)wait;

- (void)runFailedWithNSString:(NSString *)message;

- (JunitFrameworkTestResult *)runSingleMethodWithNSString:(NSString *)testCase
                                             withNSString:(NSString *)method
                                              withBoolean:(jboolean)wait;

@end

J2OBJC_EMPTY_STATIC_INIT(JunitTextuiTestRunner)

inline jint JunitTextuiTestRunner_get_SUCCESS_EXIT(void);
#define JunitTextuiTestRunner_SUCCESS_EXIT 0
J2OBJC_STATIC_FIELD_CONSTANT(JunitTextuiTestRunner, SUCCESS_EXIT, jint)

inline jint JunitTextuiTestRunner_get_FAILURE_EXIT(void);
#define JunitTextuiTestRunner_FAILURE_EXIT 1
J2OBJC_STATIC_FIELD_CONSTANT(JunitTextuiTestRunner, FAILURE_EXIT, jint)

inline jint JunitTextuiTestRunner_get_EXCEPTION_EXIT(void);
#define JunitTextuiTestRunner_EXCEPTION_EXIT 2
J2OBJC_STATIC_FIELD_CONSTANT(JunitTextuiTestRunner, EXCEPTION_EXIT, jint)

FOUNDATION_EXPORT void JunitTextuiTestRunner_init(JunitTextuiTestRunner *self);

FOUNDATION_EXPORT JunitTextuiTestRunner *new_JunitTextuiTestRunner_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JunitTextuiTestRunner *create_JunitTextuiTestRunner_init(void);

FOUNDATION_EXPORT void JunitTextuiTestRunner_initWithJavaIoPrintStream_(JunitTextuiTestRunner *self, JavaIoPrintStream *writer);

FOUNDATION_EXPORT JunitTextuiTestRunner *new_JunitTextuiTestRunner_initWithJavaIoPrintStream_(JavaIoPrintStream *writer) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JunitTextuiTestRunner *create_JunitTextuiTestRunner_initWithJavaIoPrintStream_(JavaIoPrintStream *writer);

FOUNDATION_EXPORT void JunitTextuiTestRunner_initWithJunitTextuiResultPrinter_(JunitTextuiTestRunner *self, JunitTextuiResultPrinter *printer);

FOUNDATION_EXPORT JunitTextuiTestRunner *new_JunitTextuiTestRunner_initWithJunitTextuiResultPrinter_(JunitTextuiResultPrinter *printer) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JunitTextuiTestRunner *create_JunitTextuiTestRunner_initWithJunitTextuiResultPrinter_(JunitTextuiResultPrinter *printer);

FOUNDATION_EXPORT void JunitTextuiTestRunner_runWithIOSClass_(IOSClass *testClass);

FOUNDATION_EXPORT JunitFrameworkTestResult *JunitTextuiTestRunner_runWithJunitFrameworkTest_(id<JunitFrameworkTest> test);

FOUNDATION_EXPORT void JunitTextuiTestRunner_runAndWaitWithJunitFrameworkTest_(id<JunitFrameworkTest> suite);

FOUNDATION_EXPORT void JunitTextuiTestRunner_mainWithNSStringArray_(IOSObjectArray *args);

J2OBJC_TYPE_LITERAL_HEADER(JunitTextuiTestRunner)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JunitTextuiTestRunner")
