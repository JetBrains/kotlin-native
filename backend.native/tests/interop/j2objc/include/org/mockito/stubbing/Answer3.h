//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/stubbing/Answer3.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoStubbingAnswer3")
#ifdef RESTRICT_OrgMockitoStubbingAnswer3
#define INCLUDE_ALL_OrgMockitoStubbingAnswer3 0
#else
#define INCLUDE_ALL_OrgMockitoStubbingAnswer3 1
#endif
#undef RESTRICT_OrgMockitoStubbingAnswer3

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoStubbingAnswer3_) && (INCLUDE_ALL_OrgMockitoStubbingAnswer3 || defined(INCLUDE_OrgMockitoStubbingAnswer3))
#define OrgMockitoStubbingAnswer3_

/*!
 @brief Generic interface to be used for configuring mock's answer for a three argument invocation.
 Answer specifies an action that is executed and a return value that is returned when you interact with the mock. 
 <p>
  Example of stubbing a mock with this custom answer: 
 <pre class="code"><code class="java">
  import static org.mockito.AdditionalAnswers.answer;
  when(mock.someMethod(anyInt(), anyString(), anyChar())).then(answer(
      new Answer3&lt;StringBuilder, Integer, String, Character&gt;() {
          public StringBuilder answer(Integer i, String s, Character c) {
              return new StringBuilder().append(i).append(s).append(c);
          }
  }));
  //Following will print "3xyz"
  System.out.println(mock.someMethod(3, "xy", 'z')); 
 </code>
@endcode
 - seealso: Answer
 */
@protocol OrgMockitoStubbingAnswer3 < JavaObject >

/*!
 @param argument0 the first argument.
 @param argument1 the second argument.
 @param argument2 the third argument.
 @return the value to be returned.
 @throw Throwablethe throwable to be thrown
 */
- (id)answerWithId:(id)argument0
            withId:(id)argument1
            withId:(id)argument2;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoStubbingAnswer3)

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoStubbingAnswer3)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoStubbingAnswer3")
