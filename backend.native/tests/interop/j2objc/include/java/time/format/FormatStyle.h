//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/time/format/FormatStyle.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaTimeFormatFormatStyle")
#ifdef RESTRICT_JavaTimeFormatFormatStyle
#define INCLUDE_ALL_JavaTimeFormatFormatStyle 0
#else
#define INCLUDE_ALL_JavaTimeFormatFormatStyle 1
#endif
#undef RESTRICT_JavaTimeFormatFormatStyle

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaTimeFormatFormatStyle_) && (INCLUDE_ALL_JavaTimeFormatFormatStyle || defined(INCLUDE_JavaTimeFormatFormatStyle))
#define JavaTimeFormatFormatStyle_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;

typedef NS_ENUM(NSUInteger, JavaTimeFormatFormatStyle_Enum) {
  JavaTimeFormatFormatStyle_Enum_FULL = 0,
  JavaTimeFormatFormatStyle_Enum_LONG = 1,
  JavaTimeFormatFormatStyle_Enum_MEDIUM = 2,
  JavaTimeFormatFormatStyle_Enum_SHORT = 3,
};

/*!
 @brief Enumeration of the style of a localized date, time or date-time formatter.
 <p>
  These styles are used when obtaining a date-time style from configuration.
  See <code>DateTimeFormatter</code> and <code>DateTimeFormatterBuilder</code> for usage.
 @since 1.8
 */
@interface JavaTimeFormatFormatStyle : JavaLangEnum

@property (readonly, class, nonnull) JavaTimeFormatFormatStyle *FULL NS_SWIFT_NAME(FULL);
@property (readonly, class, nonnull) JavaTimeFormatFormatStyle *LONG NS_SWIFT_NAME(LONG);
@property (readonly, class, nonnull) JavaTimeFormatFormatStyle *MEDIUM NS_SWIFT_NAME(MEDIUM);
@property (readonly, class, nonnull) JavaTimeFormatFormatStyle *SHORT NS_SWIFT_NAME(SHORT);
+ (JavaTimeFormatFormatStyle * __nonnull)FULL;

+ (JavaTimeFormatFormatStyle * __nonnull)LONG;

+ (JavaTimeFormatFormatStyle * __nonnull)MEDIUM;

+ (JavaTimeFormatFormatStyle * __nonnull)SHORT;

#pragma mark Public

+ (JavaTimeFormatFormatStyle *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

- (JavaTimeFormatFormatStyle_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(JavaTimeFormatFormatStyle)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_values_[];

/*!
 @brief Full text style, with the most detail.
 For example, the format might be 'Tuesday, April 12, 1952 AD' or '3:30:42pm PST'.
 */
inline JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_get_FULL(void);
J2OBJC_ENUM_CONSTANT(JavaTimeFormatFormatStyle, FULL)

/*!
 @brief Long text style, with lots of detail.
 For example, the format might be 'January 12, 1952'.
 */
inline JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_get_LONG(void);
J2OBJC_ENUM_CONSTANT(JavaTimeFormatFormatStyle, LONG)

/*!
 @brief Medium text style, with some detail.
 For example, the format might be 'Jan 12, 1952'.
 */
inline JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_get_MEDIUM(void);
J2OBJC_ENUM_CONSTANT(JavaTimeFormatFormatStyle, MEDIUM)

/*!
 @brief Short text style, typically numeric.
 For example, the format might be '12.13.52' or '3:30pm'.
 */
inline JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_get_SHORT(void);
J2OBJC_ENUM_CONSTANT(JavaTimeFormatFormatStyle, SHORT)

FOUNDATION_EXPORT IOSObjectArray *JavaTimeFormatFormatStyle_values(void);

FOUNDATION_EXPORT JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT JavaTimeFormatFormatStyle *JavaTimeFormatFormatStyle_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(JavaTimeFormatFormatStyle)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaTimeFormatFormatStyle")
