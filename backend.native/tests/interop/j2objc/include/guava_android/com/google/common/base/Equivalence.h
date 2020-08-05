//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/base/Equivalence.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonBaseEquivalence")
#ifdef RESTRICT_ComGoogleCommonBaseEquivalence
#define INCLUDE_ALL_ComGoogleCommonBaseEquivalence 0
#else
#define INCLUDE_ALL_ComGoogleCommonBaseEquivalence 1
#endif
#undef RESTRICT_ComGoogleCommonBaseEquivalence
#ifdef INCLUDE_ComGoogleCommonBaseEquivalence_Identity
#define INCLUDE_ComGoogleCommonBaseEquivalence 1
#endif
#ifdef INCLUDE_ComGoogleCommonBaseEquivalence_Equals
#define INCLUDE_ComGoogleCommonBaseEquivalence 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonBaseEquivalence_) && (INCLUDE_ALL_ComGoogleCommonBaseEquivalence || defined(INCLUDE_ComGoogleCommonBaseEquivalence))
#define ComGoogleCommonBaseEquivalence_

@class ComGoogleCommonBaseEquivalence_Wrapper;
@protocol ComGoogleCommonBaseFunction;
@protocol ComGoogleCommonBasePredicate;

/*!
 @brief A strategy for determining whether two instances are considered equivalent, and for computing
  hash codes in a manner consistent with that equivalence.Two examples of equivalences are the 
 identity equivalence and the "equals" equivalence.
 <p><b>For users targeting Android API level 24 or higher:</b> This class will eventually
  implement <code>BiPredicate<T, T></code> (as it does in the main Guava artifact), but we currently
  target a lower API level. In the meantime, if you have support for method references you can use
  an equivalence as a bi-predicate like this: <code>myEquivalence::equivalent</code>.
 @author Bob Lee
 @author Ben Yu
 @author Gregory Kick
 @since 10.0 (<a href="https://github.com/google/guava/wiki/Compatibility">mostly
      source-compatible</a> since 4.0)
 */
@interface ComGoogleCommonBaseEquivalence : NSObject

#pragma mark Public

/*!
 @brief Returns an equivalence that delegates to <code>Object.equals</code> and <code>Object.hashCode</code>.
 <code>Equivalence.equivalent</code> returns <code>true</code> if both values are null, or if neither
  value is null and <code>Object.equals</code> returns <code>true</code>. <code>Equivalence.hash</code> returns 
 <code>0</code> if passed a null value.
 @since 13.0
 @since 8.0 (in Equivalences with null-friendly behavior)
 @since 4.0 (in Equivalences)
 */
+ (ComGoogleCommonBaseEquivalence *)equals;

/*!
 @brief Returns <code>true</code> if the given objects are considered equivalent.
 <p>This method describes an <i>equivalence relation</i> on object references, meaning that for
  all references <code>x</code>, <code>y</code>, and <code>z</code> (any of which may be null): 
 <ul>
    <li><code>equivalent(x, x)</code> is true (<i>reflexive</i> property)
    <li><code>equivalent(x, y)</code> and <code>equivalent(y, x)</code> each return the same result
        (<i>symmetric</i> property)
    <li>If <code>equivalent(x, y)</code> and <code>equivalent(y, z)</code> are both true, then <code>equivalent(x, z)</code>
  is also true (<i>transitive</i> property) 
 </ul>
  
 <p>Note that all calls to <code>equivalent(x, y)</code> are expected to return the same result as
  long as neither <code>x</code> nor <code>y</code> is modified.
 */
- (jboolean)equivalentWithId:(id __nullable)a
                      withId:(id __nullable)b;

/*!
 @brief Returns a predicate that evaluates to true if and only if the input is equivalent to <code>target</code>
  according to this equivalence relation.
 @since 10.0
 */
- (id<ComGoogleCommonBasePredicate>)equivalentToWithId:(id __nullable)target;

/*!
 @brief Returns a hash code for <code>t</code>.
 <p>The <code>hash</code> has the following properties: 
 <ul>
    <li>It is <i>consistent</i>: for any reference <code>x</code>, multiple invocations of <code>hash(x</code>
 } consistently return the same value provided <code>x</code> remains unchanged
        according to the definition of the equivalence. The hash need not remain consistent from
        one execution of an application to another execution of the same application.   
 <li>It is <i>distributable across equivalence</i>: for any references <code>x</code> and <code>y</code>
 , if <code>equivalent(x, y)</code>, then <code>hash(x) == hash(y)</code>. It is <i>not</i>
        necessary that the hash be distributable across <i>inequivalence</i>. If <code>equivalence(x, y)</code>
  is false, <code>hash(x) == hash(y)</code> may still be true.
    <li><code>hash(null)</code> is <code>0</code>.
  </ul>
 */
- (jint)hash__WithId:(id __nullable)t;

/*!
 @brief Returns an equivalence that uses <code>==</code> to compare values and <code>System.identityHashCode(Object)</code>
  to compute the hash code.
 <code>Equivalence.equivalent</code>
  returns <code>true</code> if <code>a == b</code>, including in the case that a and b are both null.
 @since 13.0
 @since 4.0 (in Equivalences)
 */
+ (ComGoogleCommonBaseEquivalence *)identity;

/*!
 @brief Returns a new equivalence relation for <code>F</code> which evaluates equivalence by first applying 
 <code>function</code> to the argument, then evaluating using <code>this</code>.That is, for any pair of
  non-null objects <code>x</code> and <code>y</code>, <code>equivalence.onResultOf(function).equivalent(a,
  b)</code>
  is true if and only if <code>equivalence.equivalent(function.apply(a), function.apply(b))</code>
  is true.
 <p>For example: 
 @code
 Equivalence<Person> SAME_AGE = Equivalence.equals().onResultOf(GET_PERSON_AGE); 
 
@endcode
  
 <p><code>function</code> will never be invoked with a null value. 
 <p>Note that <code>function</code> must be consistent according to <code>this</code> equivalence
  relation. That is, invoking <code>Function.apply</code> multiple times for a given value must return
  equivalent results. For example, <code>Equivalence.identity().onResultOf(Functions.toStringFunction())</code>
  is broken because it's not
  guaranteed that <code>Object.toString</code>) always returns the same string instance.
 @since 10.0
 */
- (ComGoogleCommonBaseEquivalence *)onResultOfWithComGoogleCommonBaseFunction:(id<ComGoogleCommonBaseFunction> __nonnull)function;

/*!
 @brief Returns an equivalence over iterables based on the equivalence of their elements.More
  specifically, two iterables are considered equivalent if they both contain the same number of
  elements, and each pair of corresponding elements is equivalent according to <code>this</code>.
 Null
  iterables are equivalent to one another. 
 <p>Note that this method performs a similar function for equivalences as <code>com.google.common.collect.Ordering.lexicographical</code>
  does for orderings.
 @since 10.0
 */
- (ComGoogleCommonBaseEquivalence *)pairwise;

/*!
 @brief Returns a wrapper of <code>reference</code> that implements <code>Object.equals()</code>
  such that <code>wrap(a).equals(wrap(b))</code> if and only if <code>equivalent(a,
  b)</code>
 .
 @since 10.0
 */
- (ComGoogleCommonBaseEquivalence_Wrapper *)wrapWithId:(id __nullable)reference;

#pragma mark Protected

/*!
 @brief Constructor for use by subclasses.
 */
- (instancetype __nonnull)init;

/*!
 @brief This method should not be called except by <code>equivalent</code>.When <code>equivalent</code> calls
  this method, <code>a</code> and <code>b</code> are guaranteed to be distinct, non-null instances.
 @since 10.0 (previously, subclasses would override equivalent())
 */
- (jboolean)doEquivalentWithId:(id __nonnull)a
                        withId:(id __nonnull)b;

/*!
 @brief Implemented by the user to return a hash code for <code>t</code>, subject to the requirements
  specified in <code>hash</code>.
 <p>This method should not be called except by <code>hash</code>. When <code>hash</code> calls this
  method, <code>t</code> is guaranteed to be non-null.
 @since 10.0 (previously, subclasses would override hash())
 */
- (jint)doHashWithId:(id __nonnull)t;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonBaseEquivalence)

FOUNDATION_EXPORT void ComGoogleCommonBaseEquivalence_init(ComGoogleCommonBaseEquivalence *self);

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence *ComGoogleCommonBaseEquivalence_equals(void);

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence *ComGoogleCommonBaseEquivalence_identity(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseEquivalence)

#endif

#if !defined (ComGoogleCommonBaseEquivalence_Wrapper_) && (INCLUDE_ALL_ComGoogleCommonBaseEquivalence || defined(INCLUDE_ComGoogleCommonBaseEquivalence_Wrapper))
#define ComGoogleCommonBaseEquivalence_Wrapper_

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

/*!
 @brief Wraps an object so that <code>equals(Object)</code> and <code>hashCode()</code> delegate to an <code>Equivalence</code>
 .
 <p>For example, given an <code>Equivalence</code> for <code>strings</code> named <code>equiv</code>
  that tests equivalence using their lengths: 
 @code
 equiv.wrap("a").equals(equiv.wrap("b")) // true
  equiv.wrap("a").equals(equiv.wrap("hello")) // false 
 
@endcode
  
 <p>Note in particular that an equivalence wrapper is never equal to the object it wraps. 
 @code
 equiv.wrap(obj).equals(obj) // always false 
 
@endcode
 @since 10.0
 */
@interface ComGoogleCommonBaseEquivalence_Wrapper : NSObject < JavaIoSerializable >

#pragma mark Public

/*!
 @brief Returns <code>true</code> if <code>Equivalence.equivalent(Object, Object)</code> applied to the wrapped
  references is <code>true</code> and both wrappers use the <code>same</code>
  equivalence.
 */
- (jboolean)isEqual:(id __nullable)obj;

/*!
 @brief Returns the (possibly null) reference wrapped by this instance.
 */
- (id __nullable)get;

/*!
 @brief Returns the result of <code>Equivalence.hash(Object)</code> applied to the wrapped reference.
 */
- (NSUInteger)hash;

/*!
 @brief Returns a string representation for this equivalence wrapper.The form of this string
  representation is not specified.
 */
- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonBaseEquivalence_Wrapper)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseEquivalence_Wrapper)

#endif

#if !defined (ComGoogleCommonBaseEquivalence_Equals_) && (INCLUDE_ALL_ComGoogleCommonBaseEquivalence || defined(INCLUDE_ComGoogleCommonBaseEquivalence_Equals))
#define ComGoogleCommonBaseEquivalence_Equals_

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@interface ComGoogleCommonBaseEquivalence_Equals : ComGoogleCommonBaseEquivalence < JavaIoSerializable >
@property (readonly, class, strong) ComGoogleCommonBaseEquivalence_Equals *INSTANCE NS_SWIFT_NAME(INSTANCE);

+ (ComGoogleCommonBaseEquivalence_Equals *)INSTANCE;

#pragma mark Protected

- (jboolean)doEquivalentWithId:(id __nonnull)a
                        withId:(id __nonnull)b;

- (jint)doHashWithId:(id __nonnull)o;

#pragma mark Package-Private

- (instancetype __nonnull)init;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonBaseEquivalence_Equals)

inline ComGoogleCommonBaseEquivalence_Equals *ComGoogleCommonBaseEquivalence_Equals_get_INSTANCE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Equals *ComGoogleCommonBaseEquivalence_Equals_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonBaseEquivalence_Equals, INSTANCE, ComGoogleCommonBaseEquivalence_Equals *)

FOUNDATION_EXPORT void ComGoogleCommonBaseEquivalence_Equals_init(ComGoogleCommonBaseEquivalence_Equals *self);

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Equals *new_ComGoogleCommonBaseEquivalence_Equals_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Equals *create_ComGoogleCommonBaseEquivalence_Equals_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseEquivalence_Equals)

#endif

#if !defined (ComGoogleCommonBaseEquivalence_Identity_) && (INCLUDE_ALL_ComGoogleCommonBaseEquivalence || defined(INCLUDE_ComGoogleCommonBaseEquivalence_Identity))
#define ComGoogleCommonBaseEquivalence_Identity_

#define RESTRICT_JavaIoSerializable 1
#define INCLUDE_JavaIoSerializable 1
#include "java/io/Serializable.h"

@interface ComGoogleCommonBaseEquivalence_Identity : ComGoogleCommonBaseEquivalence < JavaIoSerializable >
@property (readonly, class, strong) ComGoogleCommonBaseEquivalence_Identity *INSTANCE NS_SWIFT_NAME(INSTANCE);

+ (ComGoogleCommonBaseEquivalence_Identity *)INSTANCE;

#pragma mark Protected

- (jboolean)doEquivalentWithId:(id __nonnull)a
                        withId:(id __nonnull)b;

- (jint)doHashWithId:(id __nonnull)o;

#pragma mark Package-Private

- (instancetype __nonnull)init;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonBaseEquivalence_Identity)

inline ComGoogleCommonBaseEquivalence_Identity *ComGoogleCommonBaseEquivalence_Identity_get_INSTANCE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Identity *ComGoogleCommonBaseEquivalence_Identity_INSTANCE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(ComGoogleCommonBaseEquivalence_Identity, INSTANCE, ComGoogleCommonBaseEquivalence_Identity *)

FOUNDATION_EXPORT void ComGoogleCommonBaseEquivalence_Identity_init(ComGoogleCommonBaseEquivalence_Identity *self);

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Identity *new_ComGoogleCommonBaseEquivalence_Identity_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonBaseEquivalence_Identity *create_ComGoogleCommonBaseEquivalence_Identity_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseEquivalence_Identity)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonBaseEquivalence")
