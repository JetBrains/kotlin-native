//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/thirdparty/publicsuffix/PublicSuffixType.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleThirdpartyPublicsuffixPublicSuffixType")
#ifdef RESTRICT_ComGoogleThirdpartyPublicsuffixPublicSuffixType
#define INCLUDE_ALL_ComGoogleThirdpartyPublicsuffixPublicSuffixType 0
#else
#define INCLUDE_ALL_ComGoogleThirdpartyPublicsuffixPublicSuffixType 1
#endif
#undef RESTRICT_ComGoogleThirdpartyPublicsuffixPublicSuffixType

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleThirdpartyPublicsuffixPublicSuffixType_) && (INCLUDE_ALL_ComGoogleThirdpartyPublicsuffixPublicSuffixType || defined(INCLUDE_ComGoogleThirdpartyPublicsuffixPublicSuffixType))
#define ComGoogleThirdpartyPublicsuffixPublicSuffixType_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;

typedef NS_ENUM(NSUInteger, ComGoogleThirdpartyPublicsuffixPublicSuffixType_Enum) {
  ComGoogleThirdpartyPublicsuffixPublicSuffixType_Enum_PRIVATE = 0,
  ComGoogleThirdpartyPublicsuffixPublicSuffixType_Enum_REGISTRY = 1,
};

/*!
 @brief <b>Do not use this class directly.For access to public-suffix information, use <code>com.google.common.net.InternetDomainName</code>
 .
 </b>
  
 <p>Specifies the type of a top-level domain definition.
 @since 23.3
 */
@interface ComGoogleThirdpartyPublicsuffixPublicSuffixType : JavaLangEnum

@property (readonly, class, nonnull) ComGoogleThirdpartyPublicsuffixPublicSuffixType *PRIVATE NS_SWIFT_NAME(PRIVATE);
@property (readonly, class, nonnull) ComGoogleThirdpartyPublicsuffixPublicSuffixType *REGISTRY NS_SWIFT_NAME(REGISTRY);
+ (ComGoogleThirdpartyPublicsuffixPublicSuffixType * __nonnull)PRIVATE;

+ (ComGoogleThirdpartyPublicsuffixPublicSuffixType * __nonnull)REGISTRY;

#pragma mark Public

+ (ComGoogleThirdpartyPublicsuffixPublicSuffixType *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

/*!
 @brief Returns a PublicSuffixType of the right type according to the given code
 */
+ (ComGoogleThirdpartyPublicsuffixPublicSuffixType *)fromCodeWithChar:(jchar)code;

+ (ComGoogleThirdpartyPublicsuffixPublicSuffixType *)fromIsPrivateWithBoolean:(jboolean)isPrivate;

- (jchar)getInnerNodeCode;

- (jchar)getLeafNodeCode;

- (ComGoogleThirdpartyPublicsuffixPublicSuffixType_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(ComGoogleThirdpartyPublicsuffixPublicSuffixType)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_values_[];

/*!
 @brief Public suffix that is provided by a private company, e.g."
 blogspot.com"
 */
inline ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_get_PRIVATE(void);
J2OBJC_ENUM_CONSTANT(ComGoogleThirdpartyPublicsuffixPublicSuffixType, PRIVATE)

/*!
 @brief Public suffix that is backed by an ICANN-style domain name registry
 */
inline ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_get_REGISTRY(void);
J2OBJC_ENUM_CONSTANT(ComGoogleThirdpartyPublicsuffixPublicSuffixType, REGISTRY)

FOUNDATION_EXPORT ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_fromCodeWithChar_(jchar code);

FOUNDATION_EXPORT ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_fromIsPrivateWithBoolean_(jboolean isPrivate);

FOUNDATION_EXPORT IOSObjectArray *ComGoogleThirdpartyPublicsuffixPublicSuffixType_values(void);

FOUNDATION_EXPORT ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComGoogleThirdpartyPublicsuffixPublicSuffixType *ComGoogleThirdpartyPublicsuffixPublicSuffixType_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleThirdpartyPublicsuffixPublicSuffixType)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleThirdpartyPublicsuffixPublicSuffixType")
