//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/ImmutableRangeMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap")
#ifdef RESTRICT_ComGoogleCommonCollectImmutableRangeMap
#define INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectImmutableRangeMap

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectImmutableRangeMap_) && (INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap || defined(INCLUDE_ComGoogleCommonCollectImmutableRangeMap))
#define ComGoogleCommonCollectImmutableRangeMap_

#define RESTRICT_ComGoogleCommonCollectRangeMap 1
#define INCLUDE_ComGoogleCommonCollectRangeMap 1
#include "com/google/common/collect/RangeMap.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@class ComGoogleCommonCollectImmutableList;
@class ComGoogleCommonCollectImmutableMap;
@class ComGoogleCommonCollectImmutableRangeMap_Builder;
@class ComGoogleCommonCollectRange;
@protocol JavaLangComparable;
@protocol JavaUtilFunctionFunction;
@protocol JavaUtilMap_Entry;
@protocol JavaUtilStreamCollector;

/*!
 @brief A <code>RangeMap</code> whose contents will never change, with many other important properties
  detailed at <code>ImmutableCollection</code>.
 @author Louis Wasserman
 @since 14.0
 */
@interface ComGoogleCommonCollectImmutableRangeMap : NSObject < ComGoogleCommonCollectRangeMap, JavaIoSerializable >

#pragma mark Public

- (ComGoogleCommonCollectImmutableMap *)asDescendingMapOfRanges;

- (ComGoogleCommonCollectImmutableMap *)asMapOfRanges;

/*!
 @brief Returns a new builder for an immutable range map.
 */
+ (ComGoogleCommonCollectImmutableRangeMap_Builder *)builder;

/*!
 @brief Guaranteed to throw an exception and leave the <code>RangeMap</code> unmodified.
 @throw UnsupportedOperationExceptionalways
 */
- (void)clear __attribute__((deprecated));

+ (ComGoogleCommonCollectImmutableRangeMap *)copyOfWithComGoogleCommonCollectRangeMap:(id<ComGoogleCommonCollectRangeMap>)rangeMap OBJC_METHOD_FAMILY_NONE;

- (jboolean)isEqual:(id __nullable)o;

- (id)getWithJavaLangComparable:(id<JavaLangComparable>)key;

- (id<JavaUtilMap_Entry> __nullable)getEntryWithJavaLangComparable:(id<JavaLangComparable>)key;

- (NSUInteger)hash;

/*!
 @brief Returns an empty immutable range map.
 */
+ (ComGoogleCommonCollectImmutableRangeMap *)of;

/*!
 @brief Returns an immutable range map mapping a single range to a single value.
 */
+ (ComGoogleCommonCollectImmutableRangeMap *)ofWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range
                                                                        withId:(id)value;

/*!
 @brief Guaranteed to throw an exception and leave the <code>RangeMap</code> unmodified.
 @throw UnsupportedOperationExceptionalways
 */
- (void)putWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range
                                    withId:(id)value __attribute__((deprecated));

/*!
 @brief Guaranteed to throw an exception and leave the <code>RangeMap</code> unmodified.
 @throw UnsupportedOperationExceptionalways
 */
- (void)putAllWithComGoogleCommonCollectRangeMap:(id<ComGoogleCommonCollectRangeMap>)rangeMap __attribute__((deprecated));

/*!
 @brief Guaranteed to throw an exception and leave the <code>RangeMap</code> unmodified.
 @throw UnsupportedOperationExceptionalways
 */
- (void)putCoalescingWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range
                                              withId:(id)value __attribute__((deprecated));

/*!
 @brief Guaranteed to throw an exception and leave the <code>RangeMap</code> unmodified.
 @throw UnsupportedOperationExceptionalways
 */
- (void)removeWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range __attribute__((deprecated));

- (ComGoogleCommonCollectRange *)span;

- (ComGoogleCommonCollectImmutableRangeMap *)subRangeMapWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range;

/*!
 @brief Returns a <code>Collector</code> that accumulates the input elements into a new <code>ImmutableRangeMap</code>
 .As in <code>Builder</code>, overlapping ranges are not permitted.
 @since 23.1
 */
+ (id<JavaUtilStreamCollector>)toImmutableRangeMapWithJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)keyFunction
                                                  withJavaUtilFunctionFunction:(id<JavaUtilFunctionFunction>)valueFunction;

- (NSString *)description;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectImmutableList:(ComGoogleCommonCollectImmutableList *)ranges
                              withComGoogleCommonCollectImmutableList:(ComGoogleCommonCollectImmutableList *)values;

- (id)writeReplace;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonCollectImmutableRangeMap)

FOUNDATION_EXPORT id<JavaUtilStreamCollector> ComGoogleCommonCollectImmutableRangeMap_toImmutableRangeMapWithJavaUtilFunctionFunction_withJavaUtilFunctionFunction_(id<JavaUtilFunctionFunction> keyFunction, id<JavaUtilFunctionFunction> valueFunction);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap *ComGoogleCommonCollectImmutableRangeMap_of(void);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap *ComGoogleCommonCollectImmutableRangeMap_ofWithComGoogleCommonCollectRange_withId_(ComGoogleCommonCollectRange *range, id value);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap *ComGoogleCommonCollectImmutableRangeMap_copyOfWithComGoogleCommonCollectRangeMap_(id<ComGoogleCommonCollectRangeMap> rangeMap);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap_Builder *ComGoogleCommonCollectImmutableRangeMap_builder(void);

FOUNDATION_EXPORT void ComGoogleCommonCollectImmutableRangeMap_initWithComGoogleCommonCollectImmutableList_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectImmutableRangeMap *self, ComGoogleCommonCollectImmutableList *ranges, ComGoogleCommonCollectImmutableList *values);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap *new_ComGoogleCommonCollectImmutableRangeMap_initWithComGoogleCommonCollectImmutableList_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectImmutableList *ranges, ComGoogleCommonCollectImmutableList *values) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap *create_ComGoogleCommonCollectImmutableRangeMap_initWithComGoogleCommonCollectImmutableList_withComGoogleCommonCollectImmutableList_(ComGoogleCommonCollectImmutableList *ranges, ComGoogleCommonCollectImmutableList *values);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectImmutableRangeMap)

#endif

#if !defined (ComGoogleCommonCollectImmutableRangeMap_Builder_) && (INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap || defined(INCLUDE_ComGoogleCommonCollectImmutableRangeMap_Builder))
#define ComGoogleCommonCollectImmutableRangeMap_Builder_

@class ComGoogleCommonCollectImmutableRangeMap;
@class ComGoogleCommonCollectRange;
@protocol ComGoogleCommonCollectRangeMap;

/*!
 @brief A builder for immutable range maps.Overlapping ranges are prohibited.
 @since 14.0
 */
@interface ComGoogleCommonCollectImmutableRangeMap_Builder : NSObject

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @brief Returns an <code>ImmutableRangeMap</code> containing the associations previously added to this
  builder.
 @throw IllegalArgumentExceptionif any two ranges inserted into this builder overlap
 */
- (ComGoogleCommonCollectImmutableRangeMap *)build;

/*!
 @brief Associates the specified range with the specified value.
 @throw IllegalArgumentExceptionif <code>range</code> is empty
 */
- (ComGoogleCommonCollectImmutableRangeMap_Builder *)putWithComGoogleCommonCollectRange:(ComGoogleCommonCollectRange *)range
                                                                                 withId:(id)value;

/*!
 @brief Copies all associations from the specified range map into this builder.
 */
- (ComGoogleCommonCollectImmutableRangeMap_Builder *)putAllWithComGoogleCommonCollectRangeMap:(id<ComGoogleCommonCollectRangeMap>)rangeMap;

#pragma mark Package-Private

- (ComGoogleCommonCollectImmutableRangeMap_Builder *)combineWithComGoogleCommonCollectImmutableRangeMap_Builder:(ComGoogleCommonCollectImmutableRangeMap_Builder *)builder;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectImmutableRangeMap_Builder)

FOUNDATION_EXPORT void ComGoogleCommonCollectImmutableRangeMap_Builder_init(ComGoogleCommonCollectImmutableRangeMap_Builder *self);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap_Builder *new_ComGoogleCommonCollectImmutableRangeMap_Builder_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableRangeMap_Builder *create_ComGoogleCommonCollectImmutableRangeMap_Builder_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectImmutableRangeMap_Builder)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectImmutableRangeMap")
