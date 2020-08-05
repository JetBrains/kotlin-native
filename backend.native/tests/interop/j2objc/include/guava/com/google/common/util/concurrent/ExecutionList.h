//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/util/concurrent/ExecutionList.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentExecutionList")
#ifdef RESTRICT_ComGoogleCommonUtilConcurrentExecutionList
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentExecutionList 0
#else
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentExecutionList 1
#endif
#undef RESTRICT_ComGoogleCommonUtilConcurrentExecutionList

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonUtilConcurrentExecutionList_) && (INCLUDE_ALL_ComGoogleCommonUtilConcurrentExecutionList || defined(INCLUDE_ComGoogleCommonUtilConcurrentExecutionList))
#define ComGoogleCommonUtilConcurrentExecutionList_

@protocol JavaLangRunnable;
@protocol JavaUtilConcurrentExecutor;

/*!
 @brief A support class for <code>ListenableFuture</code> implementations to manage their listeners.An
  instance contains a list of listeners, each with an associated <code>Executor</code>, and guarantees
  that every <code>Runnable</code> that is added will be executed after <code>execute()</code>
  is called.
 Any <code>Runnable</code> added after the call to <code>execute</code> is still
  guaranteed to execute. There is no guarantee, however, that listeners will be executed in the
  order that they are added. 
 <p>Exceptions thrown by a listener will be propagated up to the executor. Any exception thrown
  during <code>Executor.execute</code> (e.g., a <code>RejectedExecutionException</code> or an exception
  thrown by direct execution) will be caught and logged.
 @author Nishant Thakkar
 @author Sven Mawson
 @since 1.0
 */
@interface ComGoogleCommonUtilConcurrentExecutionList : NSObject

#pragma mark Public

/*!
 @brief Creates a new, empty <code>ExecutionList</code>.
 */
- (instancetype __nonnull)init;

/*!
 @brief Adds the <code>Runnable</code> and accompanying <code>Executor</code> to the list of listeners to
  execute.If execution has already begun, the listener is executed immediately.
 <p>When selecting an executor, note that <code>directExecutor</code> is dangerous in some cases. See
  the discussion in the <code>ListenableFuture.addListener</code>
  documentation.
 */
- (void)addWithJavaLangRunnable:(id<JavaLangRunnable> __nonnull)runnable
 withJavaUtilConcurrentExecutor:(id<JavaUtilConcurrentExecutor> __nonnull)executor;

/*!
 @brief Runs this execution list, executing all existing pairs in the order they were added.However,
  note that listeners added after this point may be executed before those previously added, and
  note that the execution order of all listeners is ultimately chosen by the implementations of
  the supplied executors.
 <p>This method is idempotent. Calling it several times in parallel is semantically equivalent
  to calling it exactly once.
 @since 10.0 (present in 1.0 as <code>run</code>)
 */
- (void)execute;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonUtilConcurrentExecutionList)

FOUNDATION_EXPORT void ComGoogleCommonUtilConcurrentExecutionList_init(ComGoogleCommonUtilConcurrentExecutionList *self);

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentExecutionList *new_ComGoogleCommonUtilConcurrentExecutionList_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentExecutionList *create_ComGoogleCommonUtilConcurrentExecutionList_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonUtilConcurrentExecutionList)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentExecutionList")
