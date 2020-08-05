//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/internal/junit/DefaultStubbingLookupListener.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoInternalJunitDefaultStubbingLookupListener")
#ifdef RESTRICT_OrgMockitoInternalJunitDefaultStubbingLookupListener
#define INCLUDE_ALL_OrgMockitoInternalJunitDefaultStubbingLookupListener 0
#else
#define INCLUDE_ALL_OrgMockitoInternalJunitDefaultStubbingLookupListener 1
#endif
#undef RESTRICT_OrgMockitoInternalJunitDefaultStubbingLookupListener

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoInternalJunitDefaultStubbingLookupListener_) && (INCLUDE_ALL_OrgMockitoInternalJunitDefaultStubbingLookupListener || defined(INCLUDE_OrgMockitoInternalJunitDefaultStubbingLookupListener))
#define OrgMockitoInternalJunitDefaultStubbingLookupListener_

#define RESTRICT_OrgMockitoInternalListenersStubbingLookupListener 1
#define INCLUDE_OrgMockitoInternalListenersStubbingLookupListener 1
#include "org/mockito/internal/listeners/StubbingLookupListener.h"

@class OrgMockitoQualityStrictness;
@protocol OrgMockitoInternalListenersStubbingLookupEvent;

/*!
 @brief Default implementation of stubbing lookup listener.
 Fails early if stub called with unexpected arguments, but only if current strictness is set to STRICT_STUBS.
 */
@interface OrgMockitoInternalJunitDefaultStubbingLookupListener : NSObject < OrgMockitoInternalListenersStubbingLookupListener >

#pragma mark Public

- (void)onStubbingLookupWithOrgMockitoInternalListenersStubbingLookupEvent:(id<OrgMockitoInternalListenersStubbingLookupEvent>)event;

#pragma mark Package-Private

- (instancetype __nonnull)initPackagePrivateWithOrgMockitoQualityStrictness:(OrgMockitoQualityStrictness *)strictness;

/*!
 @brief Indicates that stubbing argument mismatch was reported
 */
- (jboolean)isMismatchesReported;

/*!
 @brief Enables resetting the strictness to desired level
 */
- (void)setCurrentStrictnessWithOrgMockitoQualityStrictness:(OrgMockitoQualityStrictness *)currentStrictness;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoInternalJunitDefaultStubbingLookupListener)

FOUNDATION_EXPORT void OrgMockitoInternalJunitDefaultStubbingLookupListener_initPackagePrivateWithOrgMockitoQualityStrictness_(OrgMockitoInternalJunitDefaultStubbingLookupListener *self, OrgMockitoQualityStrictness *strictness);

FOUNDATION_EXPORT OrgMockitoInternalJunitDefaultStubbingLookupListener *new_OrgMockitoInternalJunitDefaultStubbingLookupListener_initPackagePrivateWithOrgMockitoQualityStrictness_(OrgMockitoQualityStrictness *strictness) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgMockitoInternalJunitDefaultStubbingLookupListener *create_OrgMockitoInternalJunitDefaultStubbingLookupListener_initPackagePrivateWithOrgMockitoQualityStrictness_(OrgMockitoQualityStrictness *strictness);

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoInternalJunitDefaultStubbingLookupListener)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoInternalJunitDefaultStubbingLookupListener")
