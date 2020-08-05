//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: Classes/com/google/j2objc/util/logging/IOSLogHandler.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler")
#ifdef RESTRICT_ComGoogleJ2objcUtilLoggingIOSLogHandler
#define INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler 0
#else
#define INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler 1
#endif
#undef RESTRICT_ComGoogleJ2objcUtilLoggingIOSLogHandler

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleJ2objcUtilLoggingIOSLogHandler_) && (INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler || defined(INCLUDE_ComGoogleJ2objcUtilLoggingIOSLogHandler))
#define ComGoogleJ2objcUtilLoggingIOSLogHandler_

#define RESTRICT_JavaUtilLoggingHandler 1
#define INCLUDE_JavaUtilLoggingHandler 1
#include "java/util/logging/Handler.h"

@class JavaUtilLoggingLogRecord;

/*!
 @brief Handler implementation that calls iOS asl_log(), or os_log() if supported by the OS.
 @author Tom Ball
 */
@interface ComGoogleJ2objcUtilLoggingIOSLogHandler : JavaUtilLoggingHandler
@property (readonly, copy, class) NSString *IOS_LOG_MANAGER_DEFAULTS NS_SWIFT_NAME(IOS_LOG_MANAGER_DEFAULTS);

+ (NSString *)IOS_LOG_MANAGER_DEFAULTS;

#pragma mark Public

- (instancetype __nonnull)init;

- (void)close;

- (void)flush;

- (void)publishWithJavaUtilLoggingLogRecord:(JavaUtilLoggingLogRecord *)record;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleJ2objcUtilLoggingIOSLogHandler)

inline NSString *ComGoogleJ2objcUtilLoggingIOSLogHandler_get_IOS_LOG_MANAGER_DEFAULTS(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT NSString *ComGoogleJ2objcUtilLoggingIOSLogHandler_IOS_LOG_MANAGER_DEFAULTS;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleJ2objcUtilLoggingIOSLogHandler, IOS_LOG_MANAGER_DEFAULTS, NSString *)

FOUNDATION_EXPORT void ComGoogleJ2objcUtilLoggingIOSLogHandler_init(ComGoogleJ2objcUtilLoggingIOSLogHandler *self);

FOUNDATION_EXPORT ComGoogleJ2objcUtilLoggingIOSLogHandler *new_ComGoogleJ2objcUtilLoggingIOSLogHandler_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleJ2objcUtilLoggingIOSLogHandler *create_ComGoogleJ2objcUtilLoggingIOSLogHandler_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleJ2objcUtilLoggingIOSLogHandler)

#endif

#if !defined (ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter_) && (INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler || defined(INCLUDE_ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter))
#define ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter_

#define RESTRICT_JavaUtilLoggingFormatter 1
#define INCLUDE_JavaUtilLoggingFormatter 1
#include "java/util/logging/Formatter.h"

@class JavaUtilLoggingLogRecord;

@interface ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter : JavaUtilLoggingFormatter

#pragma mark Public

/*!
 @brief Very simple formatter, since asl_log adds its own text.
 */
- (NSString *)formatWithJavaUtilLoggingLogRecord:(JavaUtilLoggingLogRecord *)record;

#pragma mark Package-Private

- (instancetype __nonnull)init;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter)

FOUNDATION_EXPORT void ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter_init(ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter *self);

FOUNDATION_EXPORT ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter *new_ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter *create_ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleJ2objcUtilLoggingIOSLogHandler_IOSLogFormatter)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleJ2objcUtilLoggingIOSLogHandler")
