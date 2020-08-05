//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/cache/CacheStats.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCacheCacheStats")
#ifdef RESTRICT_ComGoogleCommonCacheCacheStats
#define INCLUDE_ALL_ComGoogleCommonCacheCacheStats 0
#else
#define INCLUDE_ALL_ComGoogleCommonCacheCacheStats 1
#endif
#undef RESTRICT_ComGoogleCommonCacheCacheStats

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCacheCacheStats_) && (INCLUDE_ALL_ComGoogleCommonCacheCacheStats || defined(INCLUDE_ComGoogleCommonCacheCacheStats))
#define ComGoogleCommonCacheCacheStats_

/*!
 @brief Statistics about the performance of a <code>Cache</code>.Instances of this class are immutable.
 <p>Cache statistics are incremented according to the following rules: 
 <ul>
    <li>When a cache lookup encounters an existing cache entry <code>hitCount</code> is incremented.
    <li>When a cache lookup first encounters a missing cache entry, a new entry is loaded.
        <ul>
          <li>After successfully loading an entry <code>missCount</code> and <code>loadSuccessCount</code>
              are incremented, and the total loading time, in nanoseconds, is added to <code>totalLoadTime</code>
 .
          <li>When an exception is thrown while loading an entry, <code>missCount</code> and <code>loadExceptionCount</code>
  are incremented, and the total loading time, in nanoseconds, is
              added to <code>totalLoadTime</code>.
          <li>Cache lookups that encounter a missing cache entry that is still loading will wait
              for loading to complete (whether successful or not) and then increment <code>missCount</code>
 .
        </ul>
    <li>When an entry is evicted from the cache, <code>evictionCount</code> is incremented.
    <li>No stats are modified when a cache entry is invalidated or manually removed.
    <li>No stats are modified by operations invoked on the asMap view of
        the cache. 
 </ul>
  
 <p>A lookup is specifically defined as an invocation of one of the methods <code>LoadingCache.get(Object)</code>
 , <code>LoadingCache.getUnchecked(Object)</code>, <code>Cache.get(Object,
 Callable)</code>
 , or <code>LoadingCache.getAll(Iterable)</code>.
 @author Charles Fry
 @since 10.0
 */
@interface ComGoogleCommonCacheCacheStats : NSObject

#pragma mark Public

/*!
 @brief Constructs a new <code>CacheStats</code> instance.
 <p>Five parameters of the same type in a row is a bad thing, but this class is not constructed
  by end users and is too fine-grained for a builder.
 */
- (instancetype __nonnull)initWithLong:(jlong)hitCount
                              withLong:(jlong)missCount
                              withLong:(jlong)loadSuccessCount
                              withLong:(jlong)loadExceptionCount
                              withLong:(jlong)totalLoadTime
                              withLong:(jlong)evictionCount;

/*!
 @brief Returns the average time spent loading new values.This is defined as <code>totalLoadTime /
  (loadSuccessCount + loadExceptionCount)</code>
 .
 */
- (jdouble)averageLoadPenalty;

- (jboolean)isEqual:(id __nullable)object;

/*!
 @brief Returns the number of times an entry has been evicted.This count does not include manual 
 invalidations.
 */
- (jlong)evictionCount;

- (NSUInteger)hash;

/*!
 @brief Returns the number of times <code>Cache</code> lookup methods have returned a cached value.
 */
- (jlong)hitCount;

/*!
 @brief Returns the ratio of cache requests which were hits.This is defined as <code>hitCount /
  requestCount</code>
 , or <code>1.0</code> when <code>requestCount == 0</code>.
 Note that <code>hitRate +
  missRate =~ 1.0</code>
 .
 */
- (jdouble)hitRate;

/*!
 @brief Returns the total number of times that <code>Cache</code> lookup methods attempted to load new
  values.This includes both successful load operations, as well as those that threw exceptions.
 This is defined as <code>loadSuccessCount + loadExceptionCount</code>.
 */
- (jlong)loadCount;

/*!
 @brief Returns the number of times <code>Cache</code> lookup methods threw an exception while loading a new
  value.This is usually incremented in conjunction with <code>missCount</code>, though <code>missCount</code>
  is also incremented when cache loading completes successfully (see <code>loadSuccessCount</code>
 ).
 Multiple concurrent misses for the same key will result in a single load
  operation. This may be incremented not in conjunction with <code>missCount</code> if the load occurs
  as a result of a refresh or if the cache loader returned more items than was requested. <code>missCount</code>
  may also be incremented not in conjunction with this (nor <code>loadSuccessCount</code>)
  on calls to <code>getIfPresent</code>.
 */
- (jlong)loadExceptionCount;

/*!
 @brief Returns the ratio of cache loading attempts which threw exceptions.This is defined as <code>loadExceptionCount / (loadSuccessCount + loadExceptionCount)</code>
 , or <code>0.0</code> when <code>loadSuccessCount + loadExceptionCount == 0</code>
 .
 */
- (jdouble)loadExceptionRate;

/*!
 @brief Returns the number of times <code>Cache</code> lookup methods have successfully loaded a new value.
 This is usually incremented in conjunction with <code>missCount</code>, though <code>missCount</code> is
  also incremented when an exception is encountered during cache loading (see <code>loadExceptionCount</code>
 ). Multiple concurrent misses for the same key will result in a single load
  operation. This may be incremented not in conjunction with <code>missCount</code> if the load occurs
  as a result of a refresh or if the cache loader returned more items than was requested. <code>missCount</code>
  may also be incremented not in conjunction with this (nor <code>loadExceptionCount</code>
 ) on calls to <code>getIfPresent</code>.
 */
- (jlong)loadSuccessCount;

/*!
 @brief Returns a new <code>CacheStats</code> representing the difference between this <code>CacheStats</code>
  and <code>other</code>.Negative values, which aren't supported by <code>CacheStats</code> will be
  rounded up to zero.
 */
- (ComGoogleCommonCacheCacheStats *)minusWithComGoogleCommonCacheCacheStats:(ComGoogleCommonCacheCacheStats *)other;

/*!
 @brief Returns the number of times <code>Cache</code> lookup methods have returned an uncached (newly
  loaded) value, or null.Multiple concurrent calls to <code>Cache</code> lookup methods on an absent
  value can result in multiple misses, all returning the results of a single cache load
  operation.
 */
- (jlong)missCount;

/*!
 @brief Returns the ratio of cache requests which were misses.This is defined as <code>missCount /
  requestCount</code>
 , or <code>0.0</code> when <code>requestCount == 0</code>.
 Note that <code>hitRate +
  missRate =~ 1.0</code>
 . Cache misses include all requests which weren't cache hits, including
  requests which resulted in either successful or failed loading attempts, and requests which
  waited for other threads to finish loading. It is thus the case that <code>missCount &gt;=
  loadSuccessCount + loadExceptionCount</code>
 . Multiple concurrent misses for the same key will result
  in a single load operation.
 */
- (jdouble)missRate;

/*!
 @brief Returns a new <code>CacheStats</code> representing the sum of this <code>CacheStats</code> and <code>other</code>
 .
 @since 11.0
 */
- (ComGoogleCommonCacheCacheStats *)plusWithComGoogleCommonCacheCacheStats:(ComGoogleCommonCacheCacheStats *)other;

/*!
 @brief Returns the number of times <code>Cache</code> lookup methods have returned either a cached or
  uncached value.This is defined as <code>hitCount + missCount</code>.
 */
- (jlong)requestCount;

- (NSString *)description;

/*!
 @brief Returns the total number of nanoseconds the cache has spent loading new values.This can be
  used to calculate the miss penalty.
 This value is increased every time <code>loadSuccessCount</code>
  or <code>loadExceptionCount</code> is incremented.
 */
- (jlong)totalLoadTime;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCacheCacheStats)

FOUNDATION_EXPORT void ComGoogleCommonCacheCacheStats_initWithLong_withLong_withLong_withLong_withLong_withLong_(ComGoogleCommonCacheCacheStats *self, jlong hitCount, jlong missCount, jlong loadSuccessCount, jlong loadExceptionCount, jlong totalLoadTime, jlong evictionCount);

FOUNDATION_EXPORT ComGoogleCommonCacheCacheStats *new_ComGoogleCommonCacheCacheStats_initWithLong_withLong_withLong_withLong_withLong_withLong_(jlong hitCount, jlong missCount, jlong loadSuccessCount, jlong loadExceptionCount, jlong totalLoadTime, jlong evictionCount) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCacheCacheStats *create_ComGoogleCommonCacheCacheStats_initWithLong_withLong_withLong_withLong_withLong_withLong_(jlong hitCount, jlong missCount, jlong loadSuccessCount, jlong loadExceptionCount, jlong totalLoadTime, jlong evictionCount);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCacheCacheStats)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCacheCacheStats")
