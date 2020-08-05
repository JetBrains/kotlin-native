//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/junit/build_result/java/org/junit/rules/ErrorCollector.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgJunitRulesErrorCollector")
#ifdef RESTRICT_OrgJunitRulesErrorCollector
#define INCLUDE_ALL_OrgJunitRulesErrorCollector 0
#else
#define INCLUDE_ALL_OrgJunitRulesErrorCollector 1
#endif
#undef RESTRICT_OrgJunitRulesErrorCollector

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgJunitRulesErrorCollector_) && (INCLUDE_ALL_OrgJunitRulesErrorCollector || defined(INCLUDE_OrgJunitRulesErrorCollector))
#define OrgJunitRulesErrorCollector_

#define RESTRICT_OrgJunitRulesVerifier 1
#define INCLUDE_OrgJunitRulesVerifier 1
#include "org/junit/rules/Verifier.h"

@class JavaLangThrowable;
@protocol JavaUtilConcurrentCallable;
@protocol OrgHamcrestMatcher;

/*!
 @brief The ErrorCollector rule allows execution of a test to continue after the
  first problem is found (for example, to collect _all_ the incorrect rows in a
  table, and report them all at once):
 @code

  public static class UsesErrorCollectorTwice {
  		&#064;Rule
  	public ErrorCollector collector= new ErrorCollector(); 
  &#064;Test
  public void example() {
       collector.addError(new Throwable(&quot;first thing went wrong&quot;));
       collector.addError(new Throwable(&quot;second thing went wrong&quot;));
       collector.checkThat(getResult(), not(containsString(&quot;ERROR!
 &quot;)));
       // all lines will run, and then a combined failure logged at the end.
      }    }    
  
@endcode
 @since 4.7
 */
@interface OrgJunitRulesErrorCollector : OrgJunitRulesVerifier

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Adds a Throwable to the table.Execution continues, but the test will fail at the end.
 */
- (void)addErrorWithJavaLangThrowable:(JavaLangThrowable *)error;

/*!
 @brief Adds to the table the exception, if any, thrown from <code>callable</code>.
 Execution continues, but the test will fail at the end if 
 <code>callable</code> threw an exception.
 */
- (id)checkSucceedsWithJavaUtilConcurrentCallable:(id<JavaUtilConcurrentCallable>)callable;

/*!
 @brief Adds a failure with the given <code>reason</code>
  to the table if <code>matcher</code> does not match <code>value</code>.
 Execution continues, but the test will fail at the end if the match fails.
 */
- (void)checkThatWithNSString:(NSString *)reason
                       withId:(id)value
       withOrgHamcrestMatcher:(id<OrgHamcrestMatcher>)matcher;

/*!
 @brief Adds a failure to the table if <code>matcher</code> does not match <code>value</code>.
 Execution continues, but the test will fail at the end if the match fails.
 */
- (void)checkThatWithId:(id)value
 withOrgHamcrestMatcher:(id<OrgHamcrestMatcher>)matcher;

#pragma mark Protected

- (void)verify;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgJunitRulesErrorCollector)

FOUNDATION_EXPORT void OrgJunitRulesErrorCollector_init(OrgJunitRulesErrorCollector *self);

FOUNDATION_EXPORT OrgJunitRulesErrorCollector *new_OrgJunitRulesErrorCollector_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgJunitRulesErrorCollector *create_OrgJunitRulesErrorCollector_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgJunitRulesErrorCollector)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgJunitRulesErrorCollector")
