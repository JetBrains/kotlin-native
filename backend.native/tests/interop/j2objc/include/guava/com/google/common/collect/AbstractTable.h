//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/AbstractTable.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectAbstractTable")
#ifdef RESTRICT_ComGoogleCommonCollectAbstractTable
#define INCLUDE_ALL_ComGoogleCommonCollectAbstractTable 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectAbstractTable 1
#endif
#undef RESTRICT_ComGoogleCommonCollectAbstractTable

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectAbstractTable_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractTable || defined(INCLUDE_ComGoogleCommonCollectAbstractTable))
#define ComGoogleCommonCollectAbstractTable_

#define RESTRICT_ComGoogleCommonCollectTable 1
#define INCLUDE_ComGoogleCommonCollectTable 1
#include "com/google/common/collect/Table.h"

@protocol JavaUtilCollection;
@protocol JavaUtilIterator;
@protocol JavaUtilSet;
@protocol JavaUtilSpliterator;

/*!
 @brief Skeletal, implementation-agnostic implementation of the <code>Table</code> interface.
 @author Louis Wasserman
 */
@interface ComGoogleCommonCollectAbstractTable : NSObject < ComGoogleCommonCollectTable >

#pragma mark Public

- (id<JavaUtilSet>)cellSet;

- (void)clear;

- (id<JavaUtilSet>)columnKeySet;

- (jboolean)containsWithId:(id __nullable)rowKey
                    withId:(id __nullable)columnKey;

- (jboolean)containsColumnWithId:(id __nullable)columnKey;

- (jboolean)containsRowWithId:(id __nullable)rowKey;

- (jboolean)containsValueWithId:(id __nullable)value;

- (jboolean)isEqual:(id __nullable)obj;

- (id)getWithId:(id __nullable)rowKey
         withId:(id __nullable)columnKey;

- (NSUInteger)hash;

- (jboolean)isEmpty;

- (id)putWithId:(id)rowKey
         withId:(id)columnKey
         withId:(id)value;

- (void)putAllWithComGoogleCommonCollectTable:(id<ComGoogleCommonCollectTable>)table;

- (id)removeWithId:(id __nullable)rowKey
            withId:(id __nullable)columnKey;

- (id<JavaUtilSet>)rowKeySet;

/*!
 @brief Returns the string representation <code>rowMap().toString()</code>.
 */
- (NSString *)description;

- (id<JavaUtilCollection>)values;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivate;

- (id<JavaUtilIterator>)cellIterator;

- (id<JavaUtilSpliterator>)cellSpliterator;

- (id<JavaUtilSet>)createCellSet;

- (id<JavaUtilCollection>)createValues;

- (id<JavaUtilIterator>)valuesIterator;

- (id<JavaUtilSpliterator>)valuesSpliterator;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractTable)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractTable_initPackagePrivate(ComGoogleCommonCollectAbstractTable *self);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractTable)

#endif

#if !defined (ComGoogleCommonCollectAbstractTable_CellSet_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractTable || defined(INCLUDE_ComGoogleCommonCollectAbstractTable_CellSet))
#define ComGoogleCommonCollectAbstractTable_CellSet_

#define RESTRICT_JavaUtilAbstractSet 1
#define INCLUDE_JavaUtilAbstractSet 1
#include "java/util/AbstractSet.h"

@class ComGoogleCommonCollectAbstractTable;
@protocol JavaUtilIterator;
@protocol JavaUtilSpliterator;

@interface ComGoogleCommonCollectAbstractTable_CellSet : JavaUtilAbstractSet

#pragma mark Public

- (void)clear;

- (jboolean)containsWithId:(id)o;

- (id<JavaUtilIterator>)iterator;

- (jboolean)removeWithId:(id __nullable)o;

- (jint)size;

- (id<JavaUtilSpliterator>)spliterator;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectAbstractTable:(ComGoogleCommonCollectAbstractTable *)outer$;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractTable_CellSet)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractTable_CellSet_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable_CellSet *self, ComGoogleCommonCollectAbstractTable *outer$);

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractTable_CellSet *new_ComGoogleCommonCollectAbstractTable_CellSet_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable *outer$) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractTable_CellSet *create_ComGoogleCommonCollectAbstractTable_CellSet_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable *outer$);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractTable_CellSet)

#endif

#if !defined (ComGoogleCommonCollectAbstractTable_Values_) && (INCLUDE_ALL_ComGoogleCommonCollectAbstractTable || defined(INCLUDE_ComGoogleCommonCollectAbstractTable_Values))
#define ComGoogleCommonCollectAbstractTable_Values_

#define RESTRICT_JavaUtilAbstractCollection 1
#define INCLUDE_JavaUtilAbstractCollection 1
#include "java/util/AbstractCollection.h"

@class ComGoogleCommonCollectAbstractTable;
@protocol JavaUtilIterator;
@protocol JavaUtilSpliterator;

@interface ComGoogleCommonCollectAbstractTable_Values : JavaUtilAbstractCollection

#pragma mark Public

- (void)clear;

- (jboolean)containsWithId:(id)o;

- (id<JavaUtilIterator>)iterator;

- (jint)size;

- (id<JavaUtilSpliterator>)spliterator;

#pragma mark Package-Private

- (instancetype __nonnull)initWithComGoogleCommonCollectAbstractTable:(ComGoogleCommonCollectAbstractTable *)outer$;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonCollectAbstractTable_Values)

FOUNDATION_EXPORT void ComGoogleCommonCollectAbstractTable_Values_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable_Values *self, ComGoogleCommonCollectAbstractTable *outer$);

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractTable_Values *new_ComGoogleCommonCollectAbstractTable_Values_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable *outer$) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonCollectAbstractTable_Values *create_ComGoogleCommonCollectAbstractTable_Values_initWithComGoogleCommonCollectAbstractTable_(ComGoogleCommonCollectAbstractTable *outer$);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectAbstractTable_Values)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectAbstractTable")
