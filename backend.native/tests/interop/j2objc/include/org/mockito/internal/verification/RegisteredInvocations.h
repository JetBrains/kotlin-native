//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/internal/verification/RegisteredInvocations.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoInternalVerificationRegisteredInvocations")
#ifdef RESTRICT_OrgMockitoInternalVerificationRegisteredInvocations
#define INCLUDE_ALL_OrgMockitoInternalVerificationRegisteredInvocations 0
#else
#define INCLUDE_ALL_OrgMockitoInternalVerificationRegisteredInvocations 1
#endif
#undef RESTRICT_OrgMockitoInternalVerificationRegisteredInvocations

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoInternalVerificationRegisteredInvocations_) && (INCLUDE_ALL_OrgMockitoInternalVerificationRegisteredInvocations || defined(INCLUDE_OrgMockitoInternalVerificationRegisteredInvocations))
#define OrgMockitoInternalVerificationRegisteredInvocations_

@protocol JavaUtilList;
@protocol OrgMockitoInvocationInvocation;

@protocol OrgMockitoInternalVerificationRegisteredInvocations < JavaObject >

- (void)addWithOrgMockitoInvocationInvocation:(id<OrgMockitoInvocationInvocation>)invocation;

- (void)removeLast;

- (id<JavaUtilList>)getAll;

- (void)clear;

- (jboolean)isEmpty;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoInternalVerificationRegisteredInvocations)

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoInternalVerificationRegisteredInvocations)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoInternalVerificationRegisteredInvocations")
