//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/collect/BoundType.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonCollectBoundType")
#ifdef RESTRICT_ComGoogleCommonCollectBoundType
#define INCLUDE_ALL_ComGoogleCommonCollectBoundType 0
#else
#define INCLUDE_ALL_ComGoogleCommonCollectBoundType 1
#endif
#undef RESTRICT_ComGoogleCommonCollectBoundType

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonCollectBoundType_) && (INCLUDE_ALL_ComGoogleCommonCollectBoundType || defined(INCLUDE_ComGoogleCommonCollectBoundType))
#define ComGoogleCommonCollectBoundType_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;

typedef NS_ENUM(NSUInteger, ComGoogleCommonCollectBoundType_Enum) {
  ComGoogleCommonCollectBoundType_Enum_OPEN = 0,
  ComGoogleCommonCollectBoundType_Enum_CLOSED = 1,
};

/*!
 @brief Indicates whether an endpoint of some range is contained in the range itself ("closed") or not
  ("open").If a range is unbounded on a side, it is neither open nor closed on that side; the
  bound simply does not exist.
 @since 10.0
 */
@interface ComGoogleCommonCollectBoundType : JavaLangEnum {
 @public
  jboolean inclusive_;
}

@property (readonly, class, nonnull) ComGoogleCommonCollectBoundType *OPEN NS_SWIFT_NAME(OPEN);
@property (readonly, class, nonnull) ComGoogleCommonCollectBoundType *CLOSED NS_SWIFT_NAME(CLOSED);
+ (ComGoogleCommonCollectBoundType * __nonnull)OPEN;

+ (ComGoogleCommonCollectBoundType * __nonnull)CLOSED;

#pragma mark Public

+ (ComGoogleCommonCollectBoundType *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

- (ComGoogleCommonCollectBoundType *)flip;

/*!
 @brief Returns the bound type corresponding to a boolean value for inclusivity.
 */
+ (ComGoogleCommonCollectBoundType *)forBooleanWithBoolean:(jboolean)inclusive;

- (ComGoogleCommonCollectBoundType_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonCollectBoundType)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_values_[];

/*!
 @brief The endpoint value <i>is not</i> considered part of the set ("exclusive").
 */
inline ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_get_OPEN(void);
J2OBJC_ENUM_CONSTANT(ComGoogleCommonCollectBoundType, OPEN)

inline ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_get_CLOSED(void);
J2OBJC_ENUM_CONSTANT(ComGoogleCommonCollectBoundType, CLOSED)

FOUNDATION_EXPORT ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_forBooleanWithBoolean_(jboolean inclusive);

FOUNDATION_EXPORT IOSObjectArray *ComGoogleCommonCollectBoundType_values(void);

FOUNDATION_EXPORT ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComGoogleCommonCollectBoundType *ComGoogleCommonCollectBoundType_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonCollectBoundType)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonCollectBoundType")
