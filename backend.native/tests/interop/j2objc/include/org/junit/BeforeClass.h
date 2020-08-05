//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/junit/build_result/java/org/junit/BeforeClass.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgJunitBeforeClass")
#ifdef RESTRICT_OrgJunitBeforeClass
#define INCLUDE_ALL_OrgJunitBeforeClass 0
#else
#define INCLUDE_ALL_OrgJunitBeforeClass 1
#endif
#undef RESTRICT_OrgJunitBeforeClass

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgJunitBeforeClass_) && (INCLUDE_ALL_OrgJunitBeforeClass || defined(INCLUDE_OrgJunitBeforeClass))
#define OrgJunitBeforeClass_

#define RESTRICT_JavaLangAnnotationAnnotation 1
#define INCLUDE_JavaLangAnnotationAnnotation 1
#include "java/lang/annotation/Annotation.h"

@class IOSClass;

/*!
 @brief <p>Sometimes several tests need to share computationally expensive setup
  (like logging into a database).
 While this can compromise the independence of
  tests, sometimes it is a necessary optimization. Annotating a <code>public static void</code> no-arg method
  with <code>@@BeforeClass</code> causes it to be run once before any of
  the test methods in the class. The <code>@@BeforeClass</code> methods of superclasses
  will be run before those the current class, unless they are shadowed in the current class.</p>
  For example: 
 @code

  public class Example {
        &#064;BeforeClass public static void onlyOnce() {
        ...
     }    
    &#064;Test public void one() {
        ...
     }    
    &#064;Test public void two() {
        ...
     }   }   
  
@endcode
 - seealso: org.junit.AfterClass
 @since 4.0
 */
@protocol OrgJunitBeforeClass < JavaLangAnnotationAnnotation >

- (jboolean)isEqual:(id)obj;

- (NSUInteger)hash;

@end

@interface OrgJunitBeforeClass : NSObject < OrgJunitBeforeClass >

@end

J2OBJC_EMPTY_STATIC_INIT(OrgJunitBeforeClass)

FOUNDATION_EXPORT id<OrgJunitBeforeClass> create_OrgJunitBeforeClass(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgJunitBeforeClass)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgJunitBeforeClass")
