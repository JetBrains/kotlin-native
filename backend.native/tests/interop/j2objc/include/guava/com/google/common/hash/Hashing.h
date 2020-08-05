//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/build_result/java/com/google/common/hash/Hashing.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonHashHashing")
#ifdef RESTRICT_ComGoogleCommonHashHashing
#define INCLUDE_ALL_ComGoogleCommonHashHashing 0
#else
#define INCLUDE_ALL_ComGoogleCommonHashHashing 1
#endif
#undef RESTRICT_ComGoogleCommonHashHashing

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonHashHashing_) && (INCLUDE_ALL_ComGoogleCommonHashHashing || defined(INCLUDE_ComGoogleCommonHashHashing))
#define ComGoogleCommonHashHashing_

@class ComGoogleCommonHashHashCode;
@class IOSByteArray;
@class IOSObjectArray;
@protocol ComGoogleCommonHashHashFunction;
@protocol JavaLangIterable;
@protocol JavaSecurityKey;

/*!
 @brief Static methods to obtain <code>HashFunction</code> instances, and other static hashing-related
  utilities.
 <p>A comparison of the various hash functions can be found <a href="http://goo.gl/jS7HH">
 here</a>.
 @author Kevin Bourrillion
 @author Dimitris Andreou
 @author Kurt Alfred Kluever
 @since 11.0
 */
@interface ComGoogleCommonHashHashing : NSObject
@property (readonly, class) jint GOOD_FAST_HASH_SEED NS_SWIFT_NAME(GOOD_FAST_HASH_SEED);

+ (jint)GOOD_FAST_HASH_SEED;

#pragma mark Public

/*!
 @brief Returns a hash function implementing the Adler-32 checksum algorithm (32 hash bits).
 <p>To get the <code>long</code> value equivalent to <code>Checksum.getValue()</code> for a <code>HashCode</code>
  produced by this function, use <code>HashCode.padToLong()</code>.
  
 <p>This function is best understood as a <a href="https://en.wikipedia.org/wiki/Checksum">
 checksum</a> rather than a true <a href="https://en.wikipedia.org/wiki/Hash_function">
 hash function</a>.
 @since 14.0
 */
+ (id<ComGoogleCommonHashHashFunction>)adler32;

/*!
 @brief Returns a hash code, having the same bit length as each of the input hash codes, that combines
  the information of these hash codes in an ordered fashion.That is, whenever two equal hash
  codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
  was computed from the <i>same</i> input hash codes in the <i>same</i> order.
 @throw IllegalArgumentExceptionif <code>hashCodes</code> is empty, or the hash codes do not all
      have the same bit length
 */
+ (ComGoogleCommonHashHashCode *)combineOrderedWithJavaLangIterable:(id<JavaLangIterable> __nonnull)hashCodes;

/*!
 @brief Returns a hash code, having the same bit length as each of the input hash codes, that combines
  the information of these hash codes in an unordered fashion.That is, whenever two equal hash
  codes are produced by two calls to this method, it is <i>as likely as possible</i> that each
  was computed from the <i>same</i> input hash codes in <i>some</i> order.
 @throw IllegalArgumentExceptionif <code>hashCodes</code> is empty, or the hash codes do not all
      have the same bit length
 */
+ (ComGoogleCommonHashHashCode *)combineUnorderedWithJavaLangIterable:(id<JavaLangIterable> __nonnull)hashCodes;

/*!
 @brief Returns a hash function which computes its hash code by concatenating the hash codes of the
  underlying hash functions together.This can be useful if you need to generate hash codes of a
  specific length.
 <p>For example, if you need 1024-bit hash codes, you could join two <code>Hashing.sha512</code> hash
  functions together: <code>Hashing.concatenating(Hashing.sha512(), Hashing.sha512())</code>.
 @since 19.0
 */
+ (id<ComGoogleCommonHashHashFunction>)concatenatingWithComGoogleCommonHashHashFunction:(id<ComGoogleCommonHashHashFunction> __nonnull)first
                                                    withComGoogleCommonHashHashFunction:(id<ComGoogleCommonHashHashFunction> __nonnull)second
                                               withComGoogleCommonHashHashFunctionArray:(IOSObjectArray * __nonnull)rest;

/*!
 @brief Returns a hash function which computes its hash code by concatenating the hash codes of the
  underlying hash functions together.This can be useful if you need to generate hash codes of a
  specific length.
 <p>For example, if you need 1024-bit hash codes, you could join two <code>Hashing.sha512</code> hash
  functions together: <code>Hashing.concatenating(Hashing.sha512(), Hashing.sha512())</code>.
 @since 19.0
 */
+ (id<ComGoogleCommonHashHashFunction>)concatenatingWithJavaLangIterable:(id<JavaLangIterable> __nonnull)hashFunctions;

/*!
 @brief Assigns to <code>hashCode</code> a "bucket" in the range <code>[0, buckets)</code>, in a uniform manner
  that minimizes the need for remapping as <code>buckets</code> grows.That is, <code>consistentHash(h, n)</code>
  equals: 
 <ul>
    <li><code>n - 1</code>, with approximate probability <code>1/n</code>
    <li><code>consistentHash(h, n - 1)</code>, otherwise (probability <code>1 - 1/n</code>)
  </ul>
  
 <p>This method is suitable for the common use case of dividing work among buckets that meet the
  following conditions: 
 <ul>
    <li>You want to assign the same fraction of inputs to each bucket.
 <li>When you reduce the number of buckets, you can accept that the most recently added
        buckets will be removed first. More concretely, if you are dividing traffic among tasks,
        you can decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and       
 <code>consistentHash</code> will handle it. If, however, you are dividing traffic among
        servers <code>alpha</code>, <code>bravo</code>, and <code>charlie</code> and you occasionally need to
        take each of the servers offline, <code>consistentHash</code> will be a poor fit: It provides
        no way for you to specify which of the three buckets is disappearing. Thus, if your
        buckets change from <code>[alpha, bravo, charlie]</code> to <code>[bravo, charlie]</code>, it will
        assign all the old <code>alpha</code> traffic to <code>bravo</code> and all the old <code>bravo</code>
        traffic to <code>charlie</code>, rather than letting <code>bravo</code> keep its traffic. 
 </ul>
  
 <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
  consistent hashing</a> for more information.
 */
+ (jint)consistentHashWithComGoogleCommonHashHashCode:(ComGoogleCommonHashHashCode * __nonnull)hashCode
                                              withInt:(jint)buckets;

/*!
 @brief Assigns to <code>input</code> a "bucket" in the range <code>[0, buckets)</code>, in a uniform manner that
  minimizes the need for remapping as <code>buckets</code> grows.That is, <code>consistentHash(h,
  n)</code>
  equals: 
 <ul>
    <li><code>n - 1</code>, with approximate probability <code>1/n</code>
    <li><code>consistentHash(h, n - 1)</code>, otherwise (probability <code>1 - 1/n</code>)
  </ul>
  
 <p>This method is suitable for the common use case of dividing work among buckets that meet the
  following conditions: 
 <ul>
    <li>You want to assign the same fraction of inputs to each bucket.
 <li>When you reduce the number of buckets, you can accept that the most recently added
        buckets will be removed first. More concretely, if you are dividing traffic among tasks,
        you can decrease the number of tasks from 15 and 10, killing off the final 5 tasks, and       
 <code>consistentHash</code> will handle it. If, however, you are dividing traffic among
        servers <code>alpha</code>, <code>bravo</code>, and <code>charlie</code> and you occasionally need to
        take each of the servers offline, <code>consistentHash</code> will be a poor fit: It provides
        no way for you to specify which of the three buckets is disappearing. Thus, if your
        buckets change from <code>[alpha, bravo, charlie]</code> to <code>[bravo, charlie]</code>, it will
        assign all the old <code>alpha</code> traffic to <code>bravo</code> and all the old <code>bravo</code>
        traffic to <code>charlie</code>, rather than letting <code>bravo</code> keep its traffic. 
 </ul>
  
 <p>See the <a href="http://en.wikipedia.org/wiki/Consistent_hashing">Wikipedia article on
  consistent hashing</a> for more information.
 */
+ (jint)consistentHashWithLong:(jlong)input
                       withInt:(jint)buckets;

/*!
 @brief Returns a hash function implementing the CRC-32 checksum algorithm (32 hash bits).
 <p>To get the <code>long</code> value equivalent to <code>Checksum.getValue()</code> for a <code>HashCode</code>
  produced by this function, use <code>HashCode.padToLong()</code>.
  
 <p>This function is best understood as a <a href="https://en.wikipedia.org/wiki/Checksum">
 checksum</a> rather than a true <a href="https://en.wikipedia.org/wiki/Hash_function">
 hash function</a>.
 @since 14.0
 */
+ (id<ComGoogleCommonHashHashFunction>)crc32;

/*!
 @brief Returns a hash function implementing the CRC32C checksum algorithm (32 hash bits) as described
  by RFC 3720, Section 12.1.
 <p>This function is best understood as a <a href="https://en.wikipedia.org/wiki/Checksum">
 checksum</a> rather than a true <a href="https://en.wikipedia.org/wiki/Hash_function">
 hash function</a>.
 @since 18.0
 */
+ (id<ComGoogleCommonHashHashFunction>)crc32c;

/*!
 @brief Returns a hash function implementing FarmHash's Fingerprint64, an open-source algorithm.
 <p>This is designed for generating persistent fingerprints of strings. It isn't
  cryptographically secure, but it produces a high-quality hash with fewer collisions than some
  alternatives we've used in the past. 
 <p>FarmHash fingerprints are encoded by <code>HashCode.asBytes</code> in little-endian order. This
  means <code>HashCode.asLong</code> is guaranteed to return the same value that
  farmhash::Fingerprint64() would for the same input (when compared using <code>com.google.common.primitives.UnsignedLongs</code>
 's encoding of 64-bit unsigned numbers). 
 <p>This function is best understood as a <a href="https://en.wikipedia.org/wiki/Fingerprint_(computing)">
 fingerprint</a> rather than a true 
 <a href="https://en.wikipedia.org/wiki/Hash_function">hash function</a>.
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)farmHashFingerprint64;

/*!
 @brief Returns a general-purpose, <b>temporary-use</b>, non-cryptographic hash function.The algorithm
  the returned function implements is unspecified and subject to change without notice.
 <p><b>Warning:</b> a new random seed for these functions is chosen each time the <code>Hashing</code>
  class is loaded. <b>Do not use this method</b> if hash codes may escape the current
  process in any way, for example being sent over RPC, or saved to disk. For a general-purpose,
  non-cryptographic hash function that will never change behavior, we suggest <code>murmur3_128</code>
 .
  
 <p>Repeated calls to this method on the same loaded <code>Hashing</code> class, using the same value
  for <code>minimumBits</code>, will return identically-behaving <code>HashFunction</code> instances.
 @param minimumBits a positive integer (can be arbitrarily large)
 @return a hash function, described above, that produces hash codes of length <code>minimumBits</code>
  or greater
 */
+ (id<ComGoogleCommonHashHashFunction>)goodFastHashWithInt:(jint)minimumBits;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  MD5 (128 hash bits) hash function and a <code>SecretKeySpec</code> created from the given byte array
  and the MD5 algorithm.
 @param key the key material of the secret key
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacMd5WithByteArray:(IOSByteArray * __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  MD5 (128 hash bits) hash function and the given secret key.
 @param key the secret key
 @throw IllegalArgumentExceptionif the given key is inappropriate for initializing this MAC
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacMd5WithJavaSecurityKey:(id<JavaSecurityKey> __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-1 (160 hash bits) hash function and a <code>SecretKeySpec</code> created from the given byte
  array and the SHA-1 algorithm.
 @param key the key material of the secret key
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha1WithByteArray:(IOSByteArray * __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-1 (160 hash bits) hash function and the given secret key.
 @param key the secret key
 @throw IllegalArgumentExceptionif the given key is inappropriate for initializing this MAC
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha1WithJavaSecurityKey:(id<JavaSecurityKey> __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-256 (256 hash bits) hash function and a <code>SecretKeySpec</code> created from the given byte
  array and the SHA-256 algorithm.
 @param key the key material of the secret key
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha256WithByteArray:(IOSByteArray * __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-256 (256 hash bits) hash function and the given secret key.
 @param key the secret key
 @throw IllegalArgumentExceptionif the given key is inappropriate for initializing this MAC
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha256WithJavaSecurityKey:(id<JavaSecurityKey> __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-512 (512 hash bits) hash function and a <code>SecretKeySpec</code> created from the given byte
  array and the SHA-512 algorithm.
 @param key the key material of the secret key
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha512WithByteArray:(IOSByteArray * __nonnull)key;

/*!
 @brief Returns a hash function implementing the Message Authentication Code (MAC) algorithm, using the
  SHA-512 (512 hash bits) hash function and the given secret key.
 @param key the secret key
 @throw IllegalArgumentExceptionif the given key is inappropriate for initializing this MAC
 @since 20.0
 */
+ (id<ComGoogleCommonHashHashFunction>)hmacSha512WithJavaSecurityKey:(id<JavaSecurityKey> __nonnull)key;

/*!
 @brief Returns a hash function implementing the MD5 hash algorithm (128 hash bits).
 */
+ (id<ComGoogleCommonHashHashFunction>)md5 __attribute__((deprecated));

/*!
 @brief Returns a hash function implementing the <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">
 128-bit murmur3
  algorithm, x64 variant</a> (little-endian variant), using a seed value of zero.
 <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
 */
+ (id<ComGoogleCommonHashHashFunction>)murmur3_128;

/*!
 @brief Returns a hash function implementing the <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">
 128-bit murmur3
  algorithm, x64 variant</a> (little-endian variant), using the given seed value.
 <p>The exact C++ equivalent is the MurmurHash3_x64_128 function (Murmur3F).
 */
+ (id<ComGoogleCommonHashHashFunction>)murmur3_128WithInt:(jint)seed;

/*!
 @brief Returns a hash function implementing the <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">
 32-bit murmur3
  algorithm, x86 variant</a> (little-endian variant), using a seed value of zero.
 <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
 */
+ (id<ComGoogleCommonHashHashFunction>)murmur3_32;

/*!
 @brief Returns a hash function implementing the <a href="https://github.com/aappleby/smhasher/blob/master/src/MurmurHash3.cpp">
 32-bit murmur3
  algorithm, x86 variant</a> (little-endian variant), using the given seed value.
 <p>The exact C++ equivalent is the MurmurHash3_x86_32 function (Murmur3A).
 */
+ (id<ComGoogleCommonHashHashFunction>)murmur3_32WithInt:(jint)seed;

/*!
 @brief Returns a hash function implementing the SHA-1 algorithm (160 hash bits).
 */
+ (id<ComGoogleCommonHashHashFunction>)sha1 __attribute__((deprecated));

/*!
 @brief Returns a hash function implementing the SHA-256 algorithm (256 hash bits).
 */
+ (id<ComGoogleCommonHashHashFunction>)sha256;

/*!
 @brief Returns a hash function implementing the SHA-384 algorithm (384 hash bits).
 @since 19.0
 */
+ (id<ComGoogleCommonHashHashFunction>)sha384;

/*!
 @brief Returns a hash function implementing the SHA-512 algorithm (512 hash bits).
 */
+ (id<ComGoogleCommonHashHashFunction>)sha512;

/*!
 @brief Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
  SipHash-2-4 algorithm</a> using a seed value of <code>k = 00 01 02 ...
 </code>.
 @since 15.0
 */
+ (id<ComGoogleCommonHashHashFunction>)sipHash24;

/*!
 @brief Returns a hash function implementing the <a href="https://131002.net/siphash/">64-bit
  SipHash-2-4 algorithm</a> using the given seed.
 @since 15.0
 */
+ (id<ComGoogleCommonHashHashFunction>)sipHash24WithLong:(jlong)k0
                                                withLong:(jlong)k1;

#pragma mark Package-Private

/*!
 @brief Checks that the passed argument is positive, and ceils it to a multiple of 32.
 */
+ (jint)checkPositiveAndMakeMultipleOf32WithInt:(jint)bits;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonHashHashing)

/*!
 @brief Used to randomize <code>goodFastHash</code> instances, so that programs which persist anything
  dependent on the hash codes they produce will fail sooner.
 */
inline jint ComGoogleCommonHashHashing_get_GOOD_FAST_HASH_SEED(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT jint ComGoogleCommonHashHashing_GOOD_FAST_HASH_SEED;
J2OBJC_STATIC_FIELD_PRIMITIVE_FINAL(ComGoogleCommonHashHashing, GOOD_FAST_HASH_SEED, jint)

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_goodFastHashWithInt_(jint minimumBits);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_murmur3_32WithInt_(jint seed);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_murmur3_32(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_murmur3_128WithInt_(jint seed);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_murmur3_128(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sipHash24(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sipHash24WithLong_withLong_(jlong k0, jlong k1);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_md5(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sha1(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sha256(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sha384(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_sha512(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacMd5WithJavaSecurityKey_(id<JavaSecurityKey> key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacMd5WithByteArray_(IOSByteArray *key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha1WithJavaSecurityKey_(id<JavaSecurityKey> key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha1WithByteArray_(IOSByteArray *key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha256WithJavaSecurityKey_(id<JavaSecurityKey> key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha256WithByteArray_(IOSByteArray *key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha512WithJavaSecurityKey_(id<JavaSecurityKey> key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_hmacSha512WithByteArray_(IOSByteArray *key);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_crc32c(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_crc32(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_adler32(void);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_farmHashFingerprint64(void);

FOUNDATION_EXPORT jint ComGoogleCommonHashHashing_consistentHashWithComGoogleCommonHashHashCode_withInt_(ComGoogleCommonHashHashCode *hashCode, jint buckets);

FOUNDATION_EXPORT jint ComGoogleCommonHashHashing_consistentHashWithLong_withInt_(jlong input, jint buckets);

FOUNDATION_EXPORT ComGoogleCommonHashHashCode *ComGoogleCommonHashHashing_combineOrderedWithJavaLangIterable_(id<JavaLangIterable> hashCodes);

FOUNDATION_EXPORT ComGoogleCommonHashHashCode *ComGoogleCommonHashHashing_combineUnorderedWithJavaLangIterable_(id<JavaLangIterable> hashCodes);

FOUNDATION_EXPORT jint ComGoogleCommonHashHashing_checkPositiveAndMakeMultipleOf32WithInt_(jint bits);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_concatenatingWithComGoogleCommonHashHashFunction_withComGoogleCommonHashHashFunction_withComGoogleCommonHashHashFunctionArray_(id<ComGoogleCommonHashHashFunction> first, id<ComGoogleCommonHashHashFunction> second, IOSObjectArray *rest);

FOUNDATION_EXPORT id<ComGoogleCommonHashHashFunction> ComGoogleCommonHashHashing_concatenatingWithJavaLangIterable_(id<JavaLangIterable> hashFunctions);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonHashHashing)

#endif

#if !defined (ComGoogleCommonHashHashing_ChecksumType_) && (INCLUDE_ALL_ComGoogleCommonHashHashing || defined(INCLUDE_ComGoogleCommonHashHashing_ChecksumType))
#define ComGoogleCommonHashHashing_ChecksumType_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

#define RESTRICT_ComGoogleCommonHashImmutableSupplier 1
#define INCLUDE_ComGoogleCommonHashImmutableSupplier 1
#include "com/google/common/hash/ImmutableSupplier.h"

@class IOSObjectArray;
@protocol ComGoogleCommonHashHashFunction;
@protocol JavaUtilZipChecksum;

typedef NS_ENUM(NSUInteger, ComGoogleCommonHashHashing_ChecksumType_Enum) {
  ComGoogleCommonHashHashing_ChecksumType_Enum_CRC_32 = 0,
  ComGoogleCommonHashHashing_ChecksumType_Enum_ADLER_32 = 1,
};

@interface ComGoogleCommonHashHashing_ChecksumType : JavaLangEnum < ComGoogleCommonHashImmutableSupplier > {
 @public
  id<ComGoogleCommonHashHashFunction> hashFunction_;
}

@property (readonly, class, nonnull) ComGoogleCommonHashHashing_ChecksumType *CRC_32 NS_SWIFT_NAME(CRC_32);
@property (readonly, class, nonnull) ComGoogleCommonHashHashing_ChecksumType *ADLER_32 NS_SWIFT_NAME(ADLER_32);
+ (ComGoogleCommonHashHashing_ChecksumType * __nonnull)CRC_32;

+ (ComGoogleCommonHashHashing_ChecksumType * __nonnull)ADLER_32;

#pragma mark Public

- (id<JavaUtilZipChecksum>)get;

+ (ComGoogleCommonHashHashing_ChecksumType *)valueOfWithNSString:(NSString * __nonnull)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

- (ComGoogleCommonHashHashing_ChecksumType_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonHashHashing_ChecksumType)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT ComGoogleCommonHashHashing_ChecksumType *ComGoogleCommonHashHashing_ChecksumType_values_[];

inline ComGoogleCommonHashHashing_ChecksumType *ComGoogleCommonHashHashing_ChecksumType_get_CRC_32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleCommonHashHashing_ChecksumType, CRC_32)

inline ComGoogleCommonHashHashing_ChecksumType *ComGoogleCommonHashHashing_ChecksumType_get_ADLER_32(void);
J2OBJC_ENUM_CONSTANT(ComGoogleCommonHashHashing_ChecksumType, ADLER_32)

J2OBJC_FIELD_SETTER(ComGoogleCommonHashHashing_ChecksumType, hashFunction_, id<ComGoogleCommonHashHashFunction>)

FOUNDATION_EXPORT IOSObjectArray *ComGoogleCommonHashHashing_ChecksumType_values(void);

FOUNDATION_EXPORT ComGoogleCommonHashHashing_ChecksumType *ComGoogleCommonHashHashing_ChecksumType_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT ComGoogleCommonHashHashing_ChecksumType *ComGoogleCommonHashHashing_ChecksumType_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonHashHashing_ChecksumType)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonHashHashing")
