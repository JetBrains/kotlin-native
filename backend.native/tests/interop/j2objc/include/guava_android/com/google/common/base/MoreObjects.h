//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/base/MoreObjects.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonBaseMoreObjects")
#ifdef RESTRICT_ComGoogleCommonBaseMoreObjects
#define INCLUDE_ALL_ComGoogleCommonBaseMoreObjects 0
#else
#define INCLUDE_ALL_ComGoogleCommonBaseMoreObjects 1
#endif
#undef RESTRICT_ComGoogleCommonBaseMoreObjects

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonBaseMoreObjects_) && (INCLUDE_ALL_ComGoogleCommonBaseMoreObjects || defined(INCLUDE_ComGoogleCommonBaseMoreObjects))
#define ComGoogleCommonBaseMoreObjects_

@class ComGoogleCommonBaseMoreObjects_ToStringHelper;
@class IOSClass;

/*!
 @brief Helper functions that operate on any <code>Object</code>, and are not already provided in <code>java.util.Objects</code>
 .
 <p>See the Guava User Guide on <a href="https://github.com/google/guava/wiki/CommonObjectUtilitiesExplained">
 writing <code>Object</code>
  methods with <code>MoreObjects</code></a>.
 @author Laurence Gonsalves
 @since 18.0 (since 2.0 as <code>Objects</code>)
 */
@interface ComGoogleCommonBaseMoreObjects : NSObject

#pragma mark Public

/*!
 @brief Returns the first of two given parameters that is not <code>null</code>, if either is, or otherwise
  throws a <code>NullPointerException</code>.
 <p>To find the first non-null element in an iterable, use <code>Iterables.find(iterable,
  Predicates.notNull())</code>
 . For varargs, use <code>Iterables.find(Arrays.asList(a, b, c, ...),
  Predicates.notNull())</code>
 , static importing as necessary. 
 <p><b>Note:</b> if <code>first</code> is represented as an <code>Optional</code>, this can be
  accomplished with <code>first.or(second)</code>. That approach also allows for
  lazy evaluation of the fallback instance, using <code>first.or(supplier)</code>
 .
 @return <code>first</code> if it is non-null; otherwise <code>second</code> if it is non-null
 @throw NullPointerExceptionif both <code>first</code> and <code>second</code> are null
 @since 18.0 (since 3.0 as <code>Objects.firstNonNull()</code>).
 */
+ (id)firstNonNullWithId:(id __nullable)first
                  withId:(id __nullable)second;

/*!
 @brief Creates an instance of <code>ToStringHelper</code> in the same manner as <code>toStringHelper(Object)</code>
 , but using the simple name of <code>clazz</code> instead of using an
  instance's <code>Object.getClass()</code>.
 <p>Note that in GWT, class names are often obfuscated.
 @param clazz the <code>Class</code>  of the instance
 @since 18.0 (since 7.0 as <code>Objects.toStringHelper()</code>).
 */
+ (ComGoogleCommonBaseMoreObjects_ToStringHelper *)toStringHelperWithIOSClass:(IOSClass * __nonnull)clazz;

/*!
 @brief Creates an instance of <code>ToStringHelper</code>.
 <p>This is helpful for implementing <code>Object.toString()</code>. Specification by example: 
 @code
 // Returns "ClassName{}"
  MoreObjects.toStringHelper(this)
      .toString();
  // Returns "ClassName{x=1}"
  MoreObjects.toStringHelper(this)
      .add("x", 1)
      .toString();
  // Returns "MyObject{x=1}"
  MoreObjects.toStringHelper("MyObject")
      .add("x", 1)
      .toString();
  // Returns "ClassName{x=1, y=foo}"
  MoreObjects.toStringHelper(this)
      .add("x", 1)
      .add("y", "foo")
      .toString();
  // Returns "ClassName{x=1}"
  MoreObjects.toStringHelper(this)
      .omitNullValues()
      .add("x", 1)
      .add("y", null)
      .toString(); 
 
@endcode
  
 <p>Note that in GWT, class names are often obfuscated.
 @param self_ the object to generate the string for (typically <code>this</code> ), used only for its
       class name
 @since 18.0 (since 2.0 as <code>Objects.toStringHelper()</code>).
 */
+ (ComGoogleCommonBaseMoreObjects_ToStringHelper *)toStringHelperWithId:(id __nonnull)self_;

/*!
 @brief Creates an instance of <code>ToStringHelper</code> in the same manner as <code>toStringHelper(Object)</code>
 , but using <code>className</code> instead of using an instance's <code>Object.getClass()</code>
 .
 @param className_ the name of the instance type
 @since 18.0 (since 7.0 as <code>Objects.toStringHelper()</code>).
 */
+ (ComGoogleCommonBaseMoreObjects_ToStringHelper *)toStringHelperWithNSString:(NSString * __nonnull)className_;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonBaseMoreObjects)

FOUNDATION_EXPORT id ComGoogleCommonBaseMoreObjects_firstNonNullWithId_withId_(id first, id second);

FOUNDATION_EXPORT ComGoogleCommonBaseMoreObjects_ToStringHelper *ComGoogleCommonBaseMoreObjects_toStringHelperWithId_(id self_);

FOUNDATION_EXPORT ComGoogleCommonBaseMoreObjects_ToStringHelper *ComGoogleCommonBaseMoreObjects_toStringHelperWithIOSClass_(IOSClass *clazz);

FOUNDATION_EXPORT ComGoogleCommonBaseMoreObjects_ToStringHelper *ComGoogleCommonBaseMoreObjects_toStringHelperWithNSString_(NSString *className_);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseMoreObjects)

#endif

#if !defined (ComGoogleCommonBaseMoreObjects_ToStringHelper_) && (INCLUDE_ALL_ComGoogleCommonBaseMoreObjects || defined(INCLUDE_ComGoogleCommonBaseMoreObjects_ToStringHelper))
#define ComGoogleCommonBaseMoreObjects_ToStringHelper_

/*!
 @brief Support class for <code>MoreObjects.toStringHelper</code>.
 @author Jason Lee
 @since 18.0 (since 2.0 as <code>Objects.ToStringHelper</code>).
 */
@interface ComGoogleCommonBaseMoreObjects_ToStringHelper : NSObject

#pragma mark Public

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                       withBoolean:(jboolean)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                          withChar:(jchar)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                        withDouble:(jdouble)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                         withFloat:(jfloat)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                           withInt:(jint)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.add()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                          withLong:(jlong)value;

/*!
 @brief Adds a name/value pair to the formatted output in <code>name=value</code> format.If <code>value</code>
  is <code>null</code>, the string <code>"null"</code> is used, unless <code>omitNullValues()</code> is
  called, in which case this name/value pair will not be added.
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addWithNSString:(NSString * __nonnull)name
                                                            withId:(id __nullable)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, boolean)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithBoolean:(jboolean)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, char)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithChar:(jchar)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, double)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithDouble:(jdouble)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, float)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithFloat:(jfloat)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, int)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithInt:(jint)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, long)</code> instead and give value a
  readable name.
 @since 18.0 (since 11.0 as <code>Objects.ToStringHelper.addValue()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithLong:(jlong)value;

/*!
 @brief Adds an unnamed value to the formatted output.
 <p>It is strongly encouraged to use <code>add(String, Object)</code> instead and give value a
  readable name.
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)addValueWithId:(id __nullable)value;

/*!
 @brief Configures the <code>ToStringHelper</code> so <code>toString()</code> will ignore properties with null
  value.The order of calling this method, relative to the <code>add()</code>/<code>addValue()</code>
  methods, is not significant.
 @since 18.0 (since 12.0 as <code>Objects.ToStringHelper.omitNullValues()</code>).
 */
- (ComGoogleCommonBaseMoreObjects_ToStringHelper *)omitNullValues;

/*!
 @brief Returns a string in the format specified by <code>MoreObjects.toStringHelper(Object)</code>.
 <p>After calling this method, you can keep adding more properties to later call toString()
  again and get a more complete representation of the same object; but properties cannot be
  removed, so this only allows limited reuse of the helper instance. The helper allows
  duplication of properties (multiple name/value pairs with the same name can be added).
 */
- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonBaseMoreObjects_ToStringHelper)

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonBaseMoreObjects_ToStringHelper)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonBaseMoreObjects")
