//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/AbstractBiMap.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap")
#ifdef RESTRICT_ComGoogleCommonCollectAbstractBiMap
#define INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap 1
#endif
#undef RESTRICT_ComGoogleCommonCollectAbstractBiMap
#ifdef INCLUDE_ComGoogleCommonCollectAbstractBiMap_Inverse
#define INCLUDE_ComGoogleCommonCollectAbstractBiMap 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectAbstractBiMap_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap || defined(INCLUDE_ComGoogleCommonCollectAbstractBiMap))
#define ComGoogleCommonCollectAbstractBiMap_

#define RESTRICT_ComGoogleCommonCollectForwardingMap 1
#define INCLUDE_ComGoogleCommonCollectForwardingMap 1
#include "com/google/common/collect/ForwardingMap.h"

#define RESTRICT_ComGoogleCommonCollectBiMap 1
#define INCLUDE_ComGoogleCommonCollectBiMap 1
#include "com/google/common/collect/BiMap.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@protocol JavaUtilFunctionBiFunction;
@protocol JavaUtilIterator;
@protocol JavaUtilMap;
@protocol JavaUtilSet;

/*!
 @brief A general-purpose bimap implementation using any two backing <code>Map</code> instances.
 <p>Note that this class contains <code>equals()</code> calls that keep it from supporting <code>IdentityHashMap</code>
  backing maps.
 @author Kevin Bourrillion
 @author Mike Bostock
 */
@interface ComGoogleCommonCollectAbstractBiMap : ComGoogleCommonCollectForwardingMap < ComGoogleCommonCollectBiMap, JavaIoSerializable > {
 @public
  ComGoogleCommonCollectAbstractBiMap *inverse_;
}

#pragma mark Public

- (void)clear;

- (jboolean)containsValueWithId:(id __nullable)value;

- (id<JavaUtilSet>)entrySet;

- (id)forcePutWithId:(id)key
              withId:(id)value;

- (id<ComGoogleCommonCollectBiMap>)inverse;

- (id<JavaUtilSet>)keySet;

- (id)putWithId:(id)key
         withId:(id)value;

- (void)putAllWithJavaUtilMap:(id<JavaUtilMap>)map;

- (id)removeWithId:(id __nullable)key;

- (void)replaceAllWithJavaUtilFunctionBiFunction:(id<JavaUtilFunctionBiFunction>)function;

- (id<JavaUtilSet>)values;

#pragma mark Protected

- (id<JavaUtilMap>)delegate;

#pragma mark Package-Private

/*!
 @brief Package-private constructor for creating a map-backed bimap.
 */
- (instancetype __nonnull)initPackagePrivateWithJavaUtilMap:(id<JavaUtilMap>)forward
                                            withJavaUtilMap:(id<JavaUtilMap>)backward;

/*!
 @brief Returns its input, or throws an exception if this is not a valid key.
 */
- (id)checkKeyWithId:(id)key;

/*!
 @brief Returns its input, or throws an exception if this is not a valid value.
 */
- (id)checkValueWithId:(id)value;

- (id<JavaUtilIterator>)entrySetIterator;

- (ComGoogleCommonCollectAbstractBiMap *)makeInverseWithJavaUtilMap:(id<JavaUtilMap>)backward;

/*!
 @brief Specifies the delegate maps going in each direction.Called by the constructor and by
  subclasses during deserialization.
 */
- (void)setDelegatesWithJavaUtilMap:(id<JavaUtilMap>)forward
                    withJavaUtilMap:(id<JavaUtilMap>)backward;

- (void)setInverseWithComGoogleCommonCollectAbstractBiMap:(ComGoogleCommonCollectAbstractBiMap *)inverse;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractBiMap)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractBiMap_initPackagePrivateWithJavaUtilMap_withJavaUtilMap_(ComGoogleCommonCollectAbstractBiMap *self, id<JavaUtilMap> forward, id<JavaUtilMap> backward);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractBiMap)

#endif

#if !defined (ComGoogleCommonCollectAbstractBiMap_BiMapEntry_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap || defined(INCLUDE_ComGoogleCommonCollectAbstractBiMap_BiMapEntry))
#define ComGoogleCommonCollectAbstractBiMap_BiMapEntry_

#define RESTRICT_ComGoogleCommonCollectForwardingMapEntry 1
#define INCLUDE_ComGoogleCommonCollectForwardingMapEntry 1
#include "com/google/common/collect/ForwardingMapEntry.h"

@class ComGoogleCommonCollectAbstractBiMap;
@protocol JavaUtilMap_Entry;

@interface ComGoogleCommonCollectAbstractBiMap_BiMapEntry : ComGoogleCommonCollectForwardingMapEntry

#pragma mark Public

- (id)setValueWithId:(id)value;

#pragma mark Protected

- (id<JavaUtilMap_Entry>)delegate;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectAbstractBiMap:(ComGoogleCommonCollectAbstractBiMap *)outer$
                                                withJavaUtilMap_Entry:(id<JavaUtilMap_Entry>)delegate;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractBiMap_BiMapEntry)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractBiMap_BiMapEntry_initWithComGoogleCommonCollectAbstractBiMap_withJavaUtilMap_Entry_(ComGoogleCommonCollectAbstractBiMap_BiMapEntry *self, ComGoogleCommonCollectAbstractBiMap *outer$, id<JavaUtilMap_Entry> delegate);

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractBiMap_BiMapEntry *new_ComGoogleCommonCollectAbstractBiMap_BiMapEntry_initWithComGoogleCommonCollectAbstractBiMap_withJavaUtilMap_Entry_(ComGoogleCommonCollectAbstractBiMap *outer$, id<JavaUtilMap_Entry> delegate) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractBiMap_BiMapEntry *create_ComGoogleCommonCollectAbstractBiMap_BiMapEntry_initWithComGoogleCommonCollectAbstractBiMap_withJavaUtilMap_Entry_(ComGoogleCommonCollectAbstractBiMap *outer$, id<JavaUtilMap_Entry> delegate);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractBiMap_BiMapEntry)

#endif

#if !defined (ComGoogleCommonCollectAbstractBiMap_Inverse_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap || defined(INCLUDE_ComGoogleCommonCollectAbstractBiMap_Inverse))
#define ComGoogleCommonCollectAbstractBiMap_Inverse_

@class ComGoogleCommonCollectAbstractBiMap;
@protocol JavaUtilMap;

/*!
 @brief The inverse of any other <code>AbstractBiMap</code> subclass.
 */
@interface ComGoogleCommonCollectAbstractBiMap_Inverse : ComGoogleCommonCollectAbstractBiMap

#pragma mark Package-Private

- (instancetype __nonnull)initWithJavaUtilMap:(id<JavaUtilMap>)backward
      withComGoogleCommonCollectAbstractBiMap:(ComGoogleCommonCollectAbstractBiMap *)forward;

- (id)checkKeyWithId:(id)key;

- (id)checkValueWithId:(id)value;

- (id)readResolve;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivateWithJavaUtilMap:(id<JavaUtilMap>)arg0
                                            withJavaUtilMap:(id<JavaUtilMap>)arg1 NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractBiMap_Inverse)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractBiMap_Inverse_initWithJavaUtilMap_withComGoogleCommonCollectAbstractBiMap_(ComGoogleCommonCollectAbstractBiMap_Inverse *self, id<JavaUtilMap> backward, ComGoogleCommonCollectAbstractBiMap *forward);

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractBiMap_Inverse *new_ComGoogleCommonCollectAbstractBiMap_Inverse_initWithJavaUtilMap_withComGoogleCommonCollectAbstractBiMap_(id<JavaUtilMap> backward, ComGoogleCommonCollectAbstractBiMap *forward) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractBiMap_Inverse *create_ComGoogleCommonCollectAbstractBiMap_Inverse_initWithJavaUtilMap_withComGoogleCommonCollectAbstractBiMap_(id<JavaUtilMap> backward, ComGoogleCommonCollectAbstractBiMap *forward);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractBiMap_Inverse)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectAbstractBiMap")
