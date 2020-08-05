//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/mock/MockCreationSettings.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoMockMockCreationSettings")
#ifdef RESTRICT_OrgMockitoMockMockCreationSettings
#define INCLUDE_ALL_OrgMockitoMockMockCreationSettings 0
#else
#define INCLUDE_ALL_OrgMockitoMockMockCreationSettings 1
#endif
#undef RESTRICT_OrgMockitoMockMockCreationSettings

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoMockMockCreationSettings_) && (INCLUDE_ALL_OrgMockitoMockMockCreationSettings || defined(INCLUDE_OrgMockitoMockMockCreationSettings))
#define OrgMockitoMockMockCreationSettings_

@class IOSClass;
@class IOSObjectArray;
@class OrgMockitoMockSerializableMode;
@protocol JavaUtilList;
@protocol JavaUtilSet;
@protocol OrgMockitoMockMockName;
@protocol OrgMockitoStubbingAnswer;

/*!
 @brief Informs about the mock settings.An immutable view of <code>org.mockito.MockSettings</code>.
 */
@protocol OrgMockitoMockMockCreationSettings < JavaObject >

/*!
 @brief Mocked type.An interface or class the mock should implement / extend.
 */
- (IOSClass *)getTypeToMock;

/*!
 @brief the extra interfaces the mock object should implement.
 */
- (id<JavaUtilSet>)getExtraInterfaces;

/*!
 @brief the name of this mock, as printed on verification errors; see <code>org.mockito.MockSettings.name</code>.
 */
- (id<OrgMockitoMockMockName>)getMockName;

/*!
 @brief the default answer for this mock, see <code>org.mockito.MockSettings.defaultAnswer</code>.
 */
- (id<OrgMockitoStubbingAnswer>)getDefaultAnswer;

/*!
 @brief the spied instance - needed for spies.
 */
- (id)getSpiedInstance;

/*!
 @brief if the mock is serializable, see <code>org.mockito.MockSettings.serializable</code>.
 */
- (jboolean)isSerializable;

/*!
 @return the serializable mode of this mock
 */
- (OrgMockitoMockSerializableMode *)getSerializableMode;

/*!
 @brief Whether the mock is only for stubbing, i.e.does not remember
  parameters on its invocation and therefore cannot
  be used for verification
 */
- (jboolean)isStubOnly;

/*!
 @brief Whether the mock should not make a best effort to preserve annotations.
 */
- (jboolean)isStripAnnotations;

/*!
 @brief <code>InvocationListener</code> instances attached to this mock, see <code>org.mockito.MockSettings.invocationListeners</code>.
 */
- (id<JavaUtilList>)getInvocationListeners;

/*!
 @brief <code>VerificationStartedListener</code> instances attached to this mock,
  see <code>org.mockito.MockSettings.verificationStartedListeners(VerificationStartedListener...)
 </code>
 @since 2.11.0
 */
- (id<JavaUtilList>)getVerificationStartedListeners;

/*!
 @brief Informs whether the mock instance should be created via constructor
 @since 1.10.12
 */
- (jboolean)isUsingConstructor;

/*!
 @brief Used when arguments should be passed to the mocked object's constructor, regardless of whether these
  arguments are supplied directly, or whether they include the outer instance.
 @return An array of arguments that are passed to the mocked object's constructor. If 
 <code>getOuterClassInstance()</code> is available, it is prepended to the passed arguments.
 @since 2.7.14
 */
- (IOSObjectArray *)getConstructorArgs;

/*!
 @brief Used when mocking non-static inner classes in conjunction with <code>isUsingConstructor()</code>
 @return the outer class instance used for creation of the mock object via the constructor.
 @since 1.10.12
 */
- (id)getOuterClassInstance;

/*!
 @brief Informs if the mock was created with "lenient" strictness, e.g.having <code>Strictness.LENIENT</code> characteristic.
 For more information about using mocks with lenient strictness, see <code>MockSettings.lenient()</code>.
 @since 2.20.0
 */
- (jboolean)isLenient;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoMockMockCreationSettings)

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoMockMockCreationSettings)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoMockMockCreationSettings")
