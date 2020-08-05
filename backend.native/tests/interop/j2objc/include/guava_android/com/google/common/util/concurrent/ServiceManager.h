//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/util/concurrent/ServiceManager.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager")
#ifdef RESTRICT_ComGoogleCommonUtilConcurrentServiceManager
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager 0
#else
#define INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager 1
#endif
#undef RESTRICT_ComGoogleCommonUtilConcurrentServiceManager

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonUtilConcurrentServiceManager_) && (INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager || defined(INCLUDE_ComGoogleCommonUtilConcurrentServiceManager))
#define ComGoogleCommonUtilConcurrentServiceManager_

@class ComGoogleCommonCollectImmutableMap;
@class ComGoogleCommonCollectImmutableMultimap;
@class ComGoogleCommonUtilConcurrentServiceManager_Listener;
@class JavaUtilConcurrentTimeUnit;
@protocol JavaLangIterable;
@protocol JavaUtilConcurrentExecutor;

/*!
 @brief A manager for monitoring and controlling a set of services.This class
  provides methods for starting, stopping and 
 inspecting a collection of services.
 Additionally, users can monitor state transitions with the listener
  mechanism. 
 <p>While it is recommended that service lifecycles be managed via this class, state transitions
  initiated via other mechanisms do not impact the correctness of its methods. For example, if the
  services are started by some mechanism besides <code>startAsync</code>, the listeners will be invoked
  when appropriate and <code>awaitHealthy</code> will still work as expected. 
 <p>Here is a simple example of how to use a <code>ServiceManager</code> to start a server. 
 @code
 class Server {
    public static void main(String[] args) {
      Set<Service> services = ...;
      ServiceManager manager = new ServiceManager(services);
      manager.addListener(new Listener() {
          public void stopped() {}
          public void healthy() {
            // Services have been initialized and are healthy, start accepting requests...
          }
          public void failure(Service service) {
            // Something failed, at this point we could log it, notify a load balancer, or take
            // some other action.  For now we will just exit.
            System.exit(1);
          }
        },
        MoreExecutors.directExecutor());
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          // Give the services 5 seconds to stop to ensure that we are responsive to shutdown
          // requests.
          try {
            manager.stopAsync().awaitStopped(5, TimeUnit.SECONDS);
          } catch (TimeoutException timeout) {
            // stopping timed out
          }        }
      });
      manager.startAsync();  // start all the services asynchronously
    }  }  
 
@endcode
  
 <p>This class uses the ServiceManager's methods to start all of its services, to respond to
  service failure and to ensure that when the JVM is shutting down all the services are stopped.
 @author Luke Sandberg
 @since 14.0
 */
@interface ComGoogleCommonUtilConcurrentServiceManager : NSObject

#pragma mark Public

/*!
 @brief Constructs a new instance for managing the given services.
 @param services The services to manage
 @throw IllegalArgumentExceptionif not all services are new or if there
      are any duplicate services.
 */
- (instancetype __nonnull)initWithJavaLangIterable:(id<JavaLangIterable> __nonnull)services;

/*!
 @brief Registers a <code>Listener</code> to be run when this <code>ServiceManager</code> changes state.The
  listener will not have previous state changes replayed, so it is suggested that listeners are
  added before any of the managed services are started.
 <p><code>addListener</code> guarantees execution ordering across calls to a given listener but not
  across calls to multiple listeners. Specifically, a given listener will have its callbacks
  invoked in the same order as the underlying service enters those states. Additionally, at most
  one of the listener's callbacks will execute at once. However, multiple listeners' callbacks
  may execute concurrently, and listeners may execute in an order different from the one in which
  they were registered. 
 <p>RuntimeExceptions thrown by a listener will be caught and logged.
 @param listener the listener to run when the manager changes state
 */
- (void)addListenerWithComGoogleCommonUtilConcurrentServiceManager_Listener:(ComGoogleCommonUtilConcurrentServiceManager_Listener * __nonnull)listener;

/*!
 @brief Registers a <code>Listener</code> to be executed on the given
  executor.The listener will not have previous state changes replayed, so it is suggested that
  listeners are added before any of the managed services are started
 .
 <p><code>addListener</code> guarantees execution ordering across calls to a given listener but not
  across calls to multiple listeners. Specifically, a given listener will have its callbacks
  invoked in the same order as the underlying service enters those states. Additionally, at most
  one of the listener's callbacks will execute at once. However, multiple listeners' callbacks
  may execute concurrently, and listeners may execute in an order different from the one in which
  they were registered. 
 <p>RuntimeExceptions thrown by a listener will be caught and logged. Any exception thrown
  during <code>Executor.execute</code> (e.g., a <code>RejectedExecutionException</code>) will be caught and
  logged. 
 <p>For fast, lightweight listeners that would be safe to execute in any thread, consider
  calling <code>addListener(Listener)</code>.
 @param listener the listener to run when the manager changes state
 @param executor the executor in which the listeners callback methods will be run.
 */
- (void)addListenerWithComGoogleCommonUtilConcurrentServiceManager_Listener:(ComGoogleCommonUtilConcurrentServiceManager_Listener * __nonnull)listener
                                             withJavaUtilConcurrentExecutor:(id<JavaUtilConcurrentExecutor> __nonnull)executor;

/*!
 @brief Waits for the <code>ServiceManager</code> to become healthy.The manager
  will become healthy after all the component services have reached the running
  state.
 @throw IllegalStateExceptionif the service manager reaches a state from which it cannot
      become healthy.
 */
- (void)awaitHealthy;

/*!
 @brief Waits for the <code>ServiceManager</code> to become healthy for no more
  than the given time.The manager will become healthy after all the component services have
  reached the running state.
 @param timeout the maximum time to wait
 @param unit the time unit of the timeout argument
 @throw TimeoutExceptionif not all of the services have finished starting within the deadline
 @throw IllegalStateExceptionif the service manager reaches a state from which it cannot
      become healthy.
 */
- (void)awaitHealthyWithLong:(jlong)timeout
withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit * __nonnull)unit;

/*!
 @brief Waits for the all the services to reach a terminal state.After this method returns all
  services will either be terminated or failed
 .
 */
- (void)awaitStopped;

/*!
 @brief Waits for the all the services to reach a terminal state for no more than the given time.After
  this method returns all services will either be terminated
  or failed.
 @param timeout the maximum time to wait
 @param unit the time unit of the timeout argument
 @throw TimeoutExceptionif not all of the services have stopped within the deadline
 */
- (void)awaitStoppedWithLong:(jlong)timeout
withJavaUtilConcurrentTimeUnit:(JavaUtilConcurrentTimeUnit * __nonnull)unit;

/*!
 @brief Returns true if all services are currently in the running state.
 <p>Users who want more detailed information should use the <code>servicesByState</code> method to
  get detailed information about which services are not running.
 */
- (jboolean)isHealthy;

/*!
 @brief Provides a snapshot of the current state of all the services under management.
 <p>N.B. This snapshot is guaranteed to be consistent, i.e. the set of states returned will
  correspond to a point in time view of the services.
 */
- (ComGoogleCommonCollectImmutableMultimap *)servicesByState;

/*!
 @brief Initiates service startup on all the services being managed.It
  is only valid to call this method if all of the services are new.
 @return this
 @throw IllegalStateExceptionif any of the Services are not <code>new</code> when the
      method is called.
 */
- (ComGoogleCommonUtilConcurrentServiceManager *)startAsync;

/*!
 @brief Returns the service load times.This value will only return startup times for services that
  have finished starting.
 @return Map of services and their corresponding startup time in millis, the map entries will be
      ordered by startup time.
 */
- (ComGoogleCommonCollectImmutableMap *)startupTimes;

/*!
 @brief Initiates service shutdown if necessary on all the services
  being managed.
 @return this
 */
- (ComGoogleCommonUtilConcurrentServiceManager *)stopAsync;

- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonUtilConcurrentServiceManager)

FOUNDATION_EXPORT void ComGoogleCommonUtilConcurrentServiceManager_initWithJavaLangIterable_(ComGoogleCommonUtilConcurrentServiceManager *self, id<JavaLangIterable> services);

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentServiceManager *new_ComGoogleCommonUtilConcurrentServiceManager_initWithJavaLangIterable_(id<JavaLangIterable> services) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonUtilConcurrentServiceManager *create_ComGoogleCommonUtilConcurrentServiceManager_initWithJavaLangIterable_(id<JavaLangIterable> services);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonUtilConcurrentServiceManager)

#endif

#if !defined (ComGoogleCommonUtilConcurrentServiceManager_Listener_) && (INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager || defined(INCLUDE_ComGoogleCommonUtilConcurrentServiceManager_Listener))
#define ComGoogleCommonUtilConcurrentServiceManager_Listener_

@protocol ComGoogleCommonUtilConcurrentService;

/*!
 @brief A listener for the aggregate state changes of the services that are under management.Users
  that need to listen to more fine-grained events (such as when each particular service
  starts, or terminates), should attach service
  listeners
  to each individual service.
 @author Luke Sandberg
 @since 15.0 (present as an interface in 14.0)
 */
@interface ComGoogleCommonUtilConcurrentServiceManager_Listener : NSObject

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Called when a component service has failed.
 @param service The service that failed.
 */
- (void)failureWithComGoogleCommonUtilConcurrentService:(id<ComGoogleCommonUtilConcurrentService> __nonnull)service;

/*!
 @brief Called when the service initially becomes healthy.
 <p>This will be called at most once after all the services have entered the running
  state. If any services fail during start up or fail
 /terminate before all other services have
  started running then this method will not be called.
 */
- (void)healthy;

/*!
 @brief Called when the all of the component services have reached a terminal state, either 
 terminated or failed.
 */
- (void)stopped;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonUtilConcurrentServiceManager_Listener)

FOUNDATION_EXPORT void ComGoogleCommonUtilConcurrentServiceManager_Listener_init(ComGoogleCommonUtilConcurrentServiceManager_Listener *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonUtilConcurrentServiceManager_Listener)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonUtilConcurrentServiceManager")
