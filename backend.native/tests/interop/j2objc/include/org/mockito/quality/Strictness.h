//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/quality/Strictness.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoQualityStrictness")
#ifdef RESTRICT_OrgMockitoQualityStrictness
#define INCLUDE_ALL_OrgMockitoQualityStrictness 0
#else
#define INCLUDE_ALL_OrgMockitoQualityStrictness 1
#endif
#undef RESTRICT_OrgMockitoQualityStrictness

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoQualityStrictness_) && (INCLUDE_ALL_OrgMockitoQualityStrictness || defined(INCLUDE_OrgMockitoQualityStrictness))
#define OrgMockitoQualityStrictness_

#define RESTRICT_JavaLangEnum 1
#define INCLUDE_JavaLangEnum 1
#include "java/lang/Enum.h"

@class IOSObjectArray;

typedef NS_ENUM(NSUInteger, OrgMockitoQualityStrictness_Enum) {
  OrgMockitoQualityStrictness_Enum_LENIENT = 0,
  OrgMockitoQualityStrictness_Enum_WARN = 1,
  OrgMockitoQualityStrictness_Enum_STRICT_STUBS = 2,
};

/*!
 @brief Configures the "strictness" of Mockito, affecting the behavior of stubbings and verification.
 "Strict stubbing" is a new feature in Mockito 2 that drives cleaner tests and better productivity.
  The easiest way to leverage it is via Mockito's JUnit support (<code>MockitoJUnit</code>) or Mockito Session (<code>MockitoSession</code>).
  <p>
  How strictness influences the behavior of the test? 
 <ol>
      <li><code>Strictness.STRICT_STUBS</code> - ensures clean tests, reduces test code duplication, improves debuggability.
        Best combination of flexibility and productivity. Highly recommended.
        Planned as default for Mockito v3.
        Enable it via <code>MockitoRule</code>, <code>MockitoJUnitRunner</code> or <code>MockitoSession</code>.
        See <code>STRICT_STUBS</code> for the details.</li>
      <li><code>Strictness.LENIENT</code> - no added behavior.
        The default of Mockito 1.x.
        Recommended only if you cannot use <code>STRICT_STUBS</code></li>
      <li><code>Strictness.WARN</code> - cleaner tests but only if you read the console output.
        Reports console warnings about unused stubs
        and stubbing argument mismatch (see <code>org.mockito.quality.MockitoHint</code>).
        The default behavior of Mockito 2.x when <code>JUnitRule</code> or <code>MockitoJUnitRunner</code> are used.
        Recommended if you cannot use <code>STRICT_STUBS</code>.
        Introduced originally with Mockito 2 because console warnings was the only compatible way of adding such feature.</li>
  </ol>
 @since 2.3.0
 */
@interface OrgMockitoQualityStrictness : JavaLangEnum

@property (readonly, class, nonnull) OrgMockitoQualityStrictness *LENIENT NS_SWIFT_NAME(LENIENT);
@property (readonly, class, nonnull) OrgMockitoQualityStrictness *WARN NS_SWIFT_NAME(WARN);
@property (readonly, class, nonnull) OrgMockitoQualityStrictness *STRICT_STUBS NS_SWIFT_NAME(STRICT_STUBS);
+ (OrgMockitoQualityStrictness * __nonnull)LENIENT;

+ (OrgMockitoQualityStrictness * __nonnull)WARN;

+ (OrgMockitoQualityStrictness * __nonnull)STRICT_STUBS;

#pragma mark Public

+ (OrgMockitoQualityStrictness *)valueOfWithNSString:(NSString *)name;

+ (IOSObjectArray *)values;

#pragma mark Package-Private

- (OrgMockitoQualityStrictness_Enum)toNSEnum;

@end

J2OBJC_STATIC_INIT(OrgMockitoQualityStrictness)

/*! INTERNAL ONLY - Use enum accessors declared below. */
FOUNDATION_EXPORT OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_values_[];

/*!
 @brief No extra strictness.Mockito 1.x behavior.
 Recommended only if you cannot use <code>STRICT_STUBS</code>.
  <p>
  For more information see <code>Strictness</code>.
 @since 2.3.0
 */
inline OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_get_LENIENT(void);
J2OBJC_ENUM_CONSTANT(OrgMockitoQualityStrictness, LENIENT)

/*!
 @brief Helps keeping tests clean and improves debuggability only if you read the console output.
 Extra warnings emitted to the console, see <code>MockitoHint</code>.
  Default Mockito 2.x behavior.
  Recommended only if you cannot use <code>STRICT_STUBS</code> because console output is ignored most of the time. 
 <p>
  For more information see <code>Strictness</code>.
 @since 2.3.0
 */
inline OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_get_WARN(void);
J2OBJC_ENUM_CONSTANT(OrgMockitoQualityStrictness, WARN)

/*!
 @brief Ensures clean tests, reduces test code duplication, improves debuggability.
 Offers best combination of flexibility and productivity.
  Highly recommended.
  Planned as default for Mockito v3.
  Enable it via our JUnit support (<code>MockitoJUnit</code>) or <code>MockitoSession</code>.
  <p>
  Adds following behavior:
   <ul>
       <li>Improved productivity: the test fails early when code under test invokes
           stubbed method with different arguments (see <code>PotentialStubbingProblem</code>).</li>
       <li>Cleaner tests without unnecessary stubbings:
           the test fails when unused stubs are present (see <code>UnnecessaryStubbingException</code>).</li>
       <li>Cleaner, more DRY tests ("Don't Repeat Yourself"):
           If you use <code>org.mockito.Mockito.verifyNoMoreInteractions(Object...)</code>
           you no longer need to explicitly verify stubbed invocations.
           They are automatically verified for you.</li>
   </ul>
  For more information see <code>Strictness</code>.
 @since 2.3.0
 */
inline OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_get_STRICT_STUBS(void);
J2OBJC_ENUM_CONSTANT(OrgMockitoQualityStrictness, STRICT_STUBS)

FOUNDATION_EXPORT IOSObjectArray *OrgMockitoQualityStrictness_values(void);

FOUNDATION_EXPORT OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_valueOfWithNSString_(NSString *name);

FOUNDATION_EXPORT OrgMockitoQualityStrictness *OrgMockitoQualityStrictness_fromOrdinal(NSUInteger ordinal);

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoQualityStrictness)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoQualityStrictness")
