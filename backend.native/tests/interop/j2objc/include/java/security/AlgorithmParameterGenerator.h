//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/security/AlgorithmParameterGenerator.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaSecurityAlgorithmParameterGenerator")
#ifdef RESTRICT_JavaSecurityAlgorithmParameterGenerator
#define INCLUDE_ALL_JavaSecurityAlgorithmParameterGenerator 0
#else
#define INCLUDE_ALL_JavaSecurityAlgorithmParameterGenerator 1
#endif
#undef RESTRICT_JavaSecurityAlgorithmParameterGenerator

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaSecurityAlgorithmParameterGenerator_) && (INCLUDE_ALL_JavaSecurityAlgorithmParameterGenerator || defined(INCLUDE_JavaSecurityAlgorithmParameterGenerator))
#define JavaSecurityAlgorithmParameterGenerator_

@class JavaSecurityAlgorithmParameterGeneratorSpi;
@class JavaSecurityAlgorithmParameters;
@class JavaSecurityProvider;
@class JavaSecuritySecureRandom;
@protocol JavaSecuritySpecAlgorithmParameterSpec;

/*!
 @brief The <code>AlgorithmParameterGenerator</code> class is used to generate a
  set of
  parameters to be used with a certain algorithm.Parameter generators
  are constructed using the <code>getInstance</code> factory methods
  (static methods that return instances of a given class).
 <P>The object that will generate the parameters can be initialized
  in two different ways: in an algorithm-independent manner, or in an
  algorithm-specific manner: 
 <ul>
  <li>The algorithm-independent approach uses the fact that all parameter
  generators share the concept of a "size" and a
  source of randomness. The measure of size is universally shared
  by all algorithm parameters, though it is interpreted differently
  for different algorithms. For example, in the case of parameters for the 
 <i>DSA</i> algorithm, "size" corresponds to the size
  of the prime modulus (in bits).
  When using this approach, algorithm-specific parameter generation
  values - if any - default to some standard values, unless they can be
  derived from the specified size. 
 <li>The other approach initializes a parameter generator object
  using algorithm-specific semantics, which are represented by a set of
  algorithm-specific parameter generation values. To generate
  Diffie-Hellman system parameters, for example, the parameter generation
  values usually
  consist of the size of the prime modulus and the size of the
  random exponent, both specified in number of bits. 
 </ul>
  
 <P>In case the client does not explicitly initialize the
  AlgorithmParameterGenerator
  (via a call to an <code>init</code> method), each provider must supply (and
  document) a default initialization. For example, the Sun provider uses a
  default modulus prime size of 1024 bits for the generation of DSA
  parameters. 
 <p> Android provides the following <code>AlgorithmParameterGenerator</code> algorithms: 
 <table>
    <thead>
      <tr>
        <th>Algorithm</th>
        <th>Supported API Levels</th>
      </tr>
    </thead>
    <tbody>
      <tr class="deprecated">
        <td>AES</td>
        <td>1-8</td>
      </tr>
      <tr class="deprecated">
        <td>DES</td>
        <td>1-8</td>
      </tr>
      <tr class="deprecated">
        <td>DESede</td>
        <td>1-8</td>
      </tr>
      <tr>
        <td>DH</td>
        <td>1+</td>
      </tr>
      <tr>
        <td>DSA</td>
        <td>1+</td>
      </tr>
    </tbody>
  </table>
  These algorithms are described in the <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#AlgorithmParameterGenerator">
  AlgorithmParameterGenerator section</a> of the
  Java Cryptography Architecture Standard Algorithm Name Documentation.
 @author Jan Luehe
 - seealso: AlgorithmParameters
 - seealso: java.security.spec.AlgorithmParameterSpec
 @since 1.2
 */
@interface JavaSecurityAlgorithmParameterGenerator : NSObject

#pragma mark Public

/*!
 @brief Generates the parameters.
 @return the new AlgorithmParameters object.
 */
- (JavaSecurityAlgorithmParameters *)generateParameters;

/*!
 @brief Returns the standard name of the algorithm this parameter
  generator is associated with.
 @return the string name of the algorithm.
 */
- (NSString *)getAlgorithm;

/*!
 @brief Returns an AlgorithmParameterGenerator object for generating
  a set of parameters to be used with the specified algorithm.
 <p> This method traverses the list of registered security Providers,
  starting with the most preferred Provider.
  A new AlgorithmParameterGenerator object encapsulating the
  AlgorithmParameterGeneratorSpi implementation from the first
  Provider that supports the specified algorithm is returned. 
 <p> Note that the list of registered providers may be retrieved via the 
 <code>Security.getProviders()</code> method.
 @param algorithm the name of the algorithm this  parameter generator is associated with.
   See the AlgorithmParameterGenerator section in the 
  <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#AlgorithmParameterGenerator">
   Java Cryptography Architecture Standard Algorithm Name Documentation
  </a>  for information about standard algorithm names.
 @return the new AlgorithmParameterGenerator object.
 @throw NoSuchAlgorithmExceptionif no Provider supports an
           AlgorithmParameterGeneratorSpi implementation for the
           specified algorithm.
 - seealso: Provider
 */
+ (JavaSecurityAlgorithmParameterGenerator *)getInstanceWithNSString:(NSString *)algorithm;

/*!
 @brief Returns an AlgorithmParameterGenerator object for generating
  a set of parameters to be used with the specified algorithm.
 <p> A new AlgorithmParameterGenerator object encapsulating the
  AlgorithmParameterGeneratorSpi implementation from the specified Provider
  object is returned.  Note that the specified Provider object
  does not have to be registered in the provider list.
 @param algorithm the string name of the algorithm this  parameter generator is associated with.
   See the AlgorithmParameterGenerator section in the 
  <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#AlgorithmParameterGenerator">
   Java Cryptography Architecture Standard Algorithm Name Documentation
  </a>  for information about standard algorithm names.
 @param provider the Provider object.
 @return the new AlgorithmParameterGenerator object.
 @throw NoSuchAlgorithmExceptionif an AlgorithmParameterGeneratorSpi
           implementation for the specified algorithm is not available
           from the specified Provider object.
 @throw IllegalArgumentExceptionif the specified provider is null.
 - seealso: Provider
 @since 1.4
 */
+ (JavaSecurityAlgorithmParameterGenerator *)getInstanceWithNSString:(NSString *)algorithm
                                            withJavaSecurityProvider:(JavaSecurityProvider *)provider;

/*!
 @brief Returns an AlgorithmParameterGenerator object for generating
  a set of parameters to be used with the specified algorithm.
 <p> A new AlgorithmParameterGenerator object encapsulating the
  AlgorithmParameterGeneratorSpi implementation from the specified provider
  is returned.  The specified provider must be registered
  in the security provider list. 
 <p> Note that the list of registered providers may be retrieved via the 
 <code>Security.getProviders()</code> method.
 @param algorithm the name of the algorithm this  parameter generator is associated with.
   See the AlgorithmParameterGenerator section in the 
  <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/security/StandardNames.html#AlgorithmParameterGenerator">
   Java Cryptography Architecture Standard Algorithm Name Documentation
  </a>  for information about standard algorithm names.
 @param provider the string name of the Provider.
 @return the new AlgorithmParameterGenerator object.
 @throw NoSuchAlgorithmExceptionif an AlgorithmParameterGeneratorSpi
           implementation for the specified algorithm is not
           available from the specified provider.
 @throw NoSuchProviderExceptionif the specified provider is not
           registered in the security provider list.
 @throw IllegalArgumentExceptionif the provider name is null
           or empty.
 - seealso: Provider
 */
+ (JavaSecurityAlgorithmParameterGenerator *)getInstanceWithNSString:(NSString *)algorithm
                                                        withNSString:(NSString *)provider;

/*!
 @brief Returns the provider of this algorithm parameter generator object.
 @return the provider of this algorithm parameter generator object
 */
- (JavaSecurityProvider *)getProvider;

/*!
 @brief Initializes this parameter generator with a set of algorithm-specific
  parameter generation values.
 To generate the parameters, the <code>SecureRandom</code>
  implementation of the highest-priority installed provider is used as
  the source of randomness.
  (If none of the installed providers supply an implementation of 
 <code>SecureRandom</code>, a system-provided source of randomness is
  used.)
 @param genParamSpec the set of algorithm-specific parameter generation values.
 @throw InvalidAlgorithmParameterExceptionif the given parameter
  generation values are inappropriate for this parameter generator.
 */
- (void)init__WithJavaSecuritySpecAlgorithmParameterSpec:(id<JavaSecuritySpecAlgorithmParameterSpec>)genParamSpec OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Initializes this parameter generator with a set of algorithm-specific
  parameter generation values.
 @param genParamSpec the set of algorithm-specific parameter generation values.
 @param random the source of randomness.
 @throw InvalidAlgorithmParameterExceptionif the given parameter
  generation values are inappropriate for this parameter generator.
 */
- (void)init__WithJavaSecuritySpecAlgorithmParameterSpec:(id<JavaSecuritySpecAlgorithmParameterSpec>)genParamSpec
                            withJavaSecuritySecureRandom:(JavaSecuritySecureRandom *)random OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Initializes this parameter generator for a certain size.
 To create the parameters, the <code>SecureRandom</code>
  implementation of the highest-priority installed provider is used as
  the source of randomness.
  (If none of the installed providers supply an implementation of 
 <code>SecureRandom</code>, a system-provided source of randomness is
  used.)
 @param size the size (number of bits).
 */
- (void)init__WithInt:(jint)size OBJC_METHOD_FAMILY_NONE;

/*!
 @brief Initializes this parameter generator for a certain size and source
  of randomness.
 @param size the size (number of bits).
 @param random the source of randomness.
 */
- (void)init__WithInt:(jint)size
withJavaSecuritySecureRandom:(JavaSecuritySecureRandom *)random OBJC_METHOD_FAMILY_NONE;

#pragma mark Protected

/*!
 @brief Creates an AlgorithmParameterGenerator object.
 @param paramGenSpi the delegate
 @param provider the provider
 @param algorithm the algorithm
 */
- (instancetype __nonnull)initWithJavaSecurityAlgorithmParameterGeneratorSpi:(JavaSecurityAlgorithmParameterGeneratorSpi *)paramGenSpi
                                                    withJavaSecurityProvider:(JavaSecurityProvider *)provider
                                                                withNSString:(NSString *)algorithm;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaSecurityAlgorithmParameterGenerator)

FOUNDATION_EXPORT void JavaSecurityAlgorithmParameterGenerator_initWithJavaSecurityAlgorithmParameterGeneratorSpi_withJavaSecurityProvider_withNSString_(JavaSecurityAlgorithmParameterGenerator *self, JavaSecurityAlgorithmParameterGeneratorSpi *paramGenSpi, JavaSecurityProvider *provider, NSString *algorithm);

FOUNDATION_EXPORT JavaSecurityAlgorithmParameterGenerator *new_JavaSecurityAlgorithmParameterGenerator_initWithJavaSecurityAlgorithmParameterGeneratorSpi_withJavaSecurityProvider_withNSString_(JavaSecurityAlgorithmParameterGeneratorSpi *paramGenSpi, JavaSecurityProvider *provider, NSString *algorithm) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaSecurityAlgorithmParameterGenerator *create_JavaSecurityAlgorithmParameterGenerator_initWithJavaSecurityAlgorithmParameterGeneratorSpi_withJavaSecurityProvider_withNSString_(JavaSecurityAlgorithmParameterGeneratorSpi *paramGenSpi, JavaSecurityProvider *provider, NSString *algorithm);

FOUNDATION_EXPORT JavaSecurityAlgorithmParameterGenerator *JavaSecurityAlgorithmParameterGenerator_getInstanceWithNSString_(NSString *algorithm);

FOUNDATION_EXPORT JavaSecurityAlgorithmParameterGenerator *JavaSecurityAlgorithmParameterGenerator_getInstanceWithNSString_withNSString_(NSString *algorithm, NSString *provider);

FOUNDATION_EXPORT JavaSecurityAlgorithmParameterGenerator *JavaSecurityAlgorithmParameterGenerator_getInstanceWithNSString_withJavaSecurityProvider_(NSString *algorithm, JavaSecurityProvider *provider);

J2OBJC_TYPE_LITERAL_HEADER(JavaSecurityAlgorithmParameterGenerator)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaSecurityAlgorithmParameterGenerator")
