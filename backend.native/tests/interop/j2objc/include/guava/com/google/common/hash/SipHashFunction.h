//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/hash/SipHashFunction.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonHashSipHashFunction")
#ifdef RESTRICT_ComGoogleCommonHashSipHashFunction
#define INCLUDE_ALL_ComGoogleCommonHashSipHashFunction 0
#else
#define INCLUDE_ALL_ComGoogleCommonHashSipHashFunction 1
#endif
#undef RESTRICT_ComGoogleCommonHashSipHashFunction

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonHashSipHashFunction_) && (INCLUDE_ALL_ComGoogleCommonHashSipHashFunction || defined(INCLUDE_ComGoogleCommonHashSipHashFunction))
#define ComGoogleCommonHashSipHashFunction_

#define RESTRICT_ComGoogleCommonHashAbstractHashFunction 1
#define INCLUDE_ComGoogleCommonHashAbstractHashFunction 1
#include "com/google/common/hash/AbstractHashFunction.h"

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@protocol ComGoogleCommonHashHashFunction;
@protocol ComGoogleCommonHashHasher;

/*!
 @brief <code>HashFunction</code> implementation of SipHash-c-d.
 @author Kurt Alfred Kluever
 @author Jean-Philippe Aumasson
 @author Daniel J. Bernstein
 */
@interface ComGoogleCommonHashSipHashFunction : ComGoogleCommonHashAbstractHashFunction < JavaIoSerializable >
@property (readonly, class, strong) id<ComGoogleCommonHashHashFunction> SIP_HASH_24 NS_SWIFT_NAME(SIP_HASH_24);

+ (id<ComGoogleCommonHashHashFunction>)SIP_HASH_24;

#pragma mark Public

- (jint)bits;

- (jboolean)isEqual:(id __nullable)object;

- (NSUInteger)hash;

- (id<ComGoogleCommonHashHasher>)newHasher OBJC_METHOD_FAMILY_NONE;

- (NSString *)description;

#pragma mark Package-Private

/*!
 @param c the number of compression rounds (must be positive)
 @param d the number of finalization rounds (must be positive)
 @param k0 the first half of the key
 @param k1 the second half of the key
 */
- (instancetype __nonnull)initPackagePrivateWithInt:(jint)c
                                            withInt:(jint)d
                                           withLong:(jlong)k0
                                           withLong:(jlong)k1;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)initPackagePrivate NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonHashSipHashFunction)

inline id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashSipHashFunction_get_SIP_HASH_24(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashSipHashFunction_SIP_HASH_24;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonHashSipHashFunction, SIP_HASH_24, id<ComGoogleCommonHashHashFunction>)

FOUNDATION_EXPORT void ComGoogleCommonHashSipHashFunction_initPackagePrivateWithInt_withInt_withLong_withLong_(ComGoogleCommonHashSipHashFunction *self, jint c, jint d, jlong k0, jlong k1);

FOUNDATION_EXPORT ComGoogleCommonHashSipHashFunction *new_ComGoogleCommonHashSipHashFunction_initPackagePrivateWithInt_withInt_withLong_withLong_(jint c, jint d, jlong k0, jlong k1) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonHashSipHashFunction *create_ComGoogleCommonHashSipHashFunction_initPackagePrivateWithInt_withInt_withLong_withLong_(jint c, jint d, jlong k0, jlong k1);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonHashSipHashFunction)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonHashSipHashFunction")
