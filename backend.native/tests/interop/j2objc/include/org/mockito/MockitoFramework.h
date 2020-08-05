//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/MockitoFramework.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoMockitoFramework")
#ifdef RESTRICT_OrgMockitoMockitoFramework
#define INCLUDE_ALL_OrgMockitoMockitoFramework 0
#else
#define INCLUDE_ALL_OrgMockitoMockitoFramework 1
#endif
#undef RESTRICT_OrgMockitoMockitoFramework

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoMockitoFramework_) && (INCLUDE_ALL_OrgMockitoMockitoFramework || defined(INCLUDE_OrgMockitoMockitoFramework))
#define OrgMockitoMockitoFramework_

@protocol OrgMockitoInvocationInvocationFactory;
@protocol OrgMockitoListenersMockitoListener;
@protocol OrgMockitoPluginsMockitoPlugins;

/*!
 @brief Mockito framework settings and lifecycle listeners, for advanced users or for integrating with other frameworks.
 <p>
  To get <code>MockitoFramework</code> instance use <code>Mockito.framework()</code>.
  <p>
  For more info on listeners see <code>addListener(MockitoListener)</code>.
 @since 2.1.0
 */
@protocol OrgMockitoMockitoFramework < JavaObject >

/*!
 @brief Adds listener to Mockito.
 For a list of supported listeners, see the interfaces that extend <code>MockitoListener</code>.
  <p>
  Listeners can be useful for engs that extend Mockito framework.
  They are used in the implementation of unused stubbings warnings (<code>org.mockito.quality.MockitoHint</code>).
  <p>
  Make sure you remove the listener when the job is complete, see <code>removeListener(MockitoListener)</code>.
  Currently the listeners list is thread local so you need to remove listener from the same thread otherwise
  remove is ineffectual.
  In typical scenarios, it is not a problem, because adding & removing listeners typically happens in the same thread. 
 <p>
  If you are trying to add the listener but a listener of the same type was already added (and not removed)
  this method will throw <code>RedundantListenerException</code>.
  This is a safeguard to ensure users actually remove the listeners via <code>removeListener(MockitoListener)</code>.
  We do not anticipate the use case where adding the same listener type multiple times is useful.
  If this safeguard is problematic, please contact us via Mockito issue tracker. 
 <p>
  For usage examples, see Mockito codebase.
  If you have ideas and feature requests about Mockito listeners API
  we are very happy to hear about it via our issue tracker or mailing list. 
 <pre class="code"><code class="java">
    Mockito.framework().addListener(myListener); 
 </code>
@endcode
 @param listener to add to Mockito
 @return this instance of mockito framework (fluent builder pattern)
 @since 2.1.0
 */
- (id<OrgMockitoMockitoFramework>)addListenerWithOrgMockitoListenersMockitoListener:(id<OrgMockitoListenersMockitoListener>)listener;

/*!
 @brief When you add listener using <code>addListener(MockitoListener)</code> make sure to remove it.
 Currently the listeners list is thread local so you need to remove listener from the same thread otherwise
  remove is ineffectual.
  In typical scenarios, it is not a problem, because adding & removing listeners typically happens in the same thread. 
 <p>
  For usage examples, see Mockito codebase.
  If you have ideas and feature requests about Mockito listeners API
  we are very happy to hear about it via our issue tracker or mailing list.
 @param listener to remove
 @return this instance of mockito framework (fluent builder pattern)
 @since 2.1.0
 */
- (id<OrgMockitoMockitoFramework>)removeListenerWithOrgMockitoListenersMockitoListener:(id<OrgMockitoListenersMockitoListener>)listener;

/*!
 @brief Returns an object that has access to Mockito plugins.
 An example plugin is <code>org.mockito.plugins.MockMaker</code>.
  For information why and how to use this method see <code>MockitoPlugins</code>.
 @return object that gives access to mockito plugins
 @since 2.10.0
 */
- (id<OrgMockitoPluginsMockitoPlugins>)getPlugins;

/*!
 @brief Returns a factory that can create instances of <code>Invocation</code>.
 It is useful for framework integrations, because <code>Invocation</code> is <code>NotExtensible</code>.
 @return object that can construct invocations
 @since 2.10.0
 */
- (id<OrgMockitoInvocationInvocationFactory>)getInvocationFactory;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoMockitoFramework)

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoMockitoFramework)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoMockitoFramework")
