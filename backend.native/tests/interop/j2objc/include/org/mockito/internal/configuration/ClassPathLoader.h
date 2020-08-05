//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/testing/mockito/build_result/java/org/mockito/internal/configuration/ClassPathLoader.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_OrgMockitoInternalConfigurationClassPathLoader")
#ifdef RESTRICT_OrgMockitoInternalConfigurationClassPathLoader
#define INCLUDE_ALL_OrgMockitoInternalConfigurationClassPathLoader 0
#else
#define INCLUDE_ALL_OrgMockitoInternalConfigurationClassPathLoader 1
#endif
#undef RESTRICT_OrgMockitoInternalConfigurationClassPathLoader

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (OrgMockitoInternalConfigurationClassPathLoader_) && (INCLUDE_ALL_OrgMockitoInternalConfigurationClassPathLoader || defined(INCLUDE_OrgMockitoInternalConfigurationClassPathLoader))
#define OrgMockitoInternalConfigurationClassPathLoader_

@protocol OrgMockitoConfigurationIMockitoConfiguration;

/*!
 @brief Loads configuration or extension points available in the classpath.
 <p>
  <ul>
      <li>
          Can load the mockito configuration. The user who want to provide his own mockito configuration
          should write the class <code>org.mockito.configuration.MockitoConfiguration</code> that implements
          <code>IMockitoConfiguration</code>. For example :
          <pre class="code"><code class="java">
  package org.mockito.configuration;
  //...
  public class MockitoConfiguration implements IMockitoConfiguration {
      boolean enableClassCache() { return false; }
      // ...
  }
      </code>
@endcode
      </li>
      <li>
          Can load available mockito extensions. Currently Mockito only have one extension point the         
 <code>MockMaker</code>. This extension point allows a user to provide his own bytecode engine to build mocks.
          <br>Suppose you wrote an extension to create mocks with some <em>Awesome</em> library, in order to tell
          Mockito to use it you need to put in your classpath         
 <ol style="list-style-type: lower-alpha">
              <li>The implementation itself, for example <code>org.awesome.mockito.AwesomeMockMaker</code>.</li>
              <li>A file named <code>org.mockito.plugins.MockMaker</code> in a folder named
              <code>mockito-extensions</code>, the content of this file need to have <strong>one</strong> line with
              the qualified name <code>org.awesome.mockito.AwesomeMockMaker</code>.</li>
          </ol>
      </li>
  </ul>
  </p>
 */
@interface OrgMockitoInternalConfigurationClassPathLoader : NSObject
@property (readonly, copy, class) NSString *MOCKITO_CONFIGURATION_CLASS_NAME NS_SWIFT_NAME(MOCKITO_CONFIGURATION_CLASS_NAME);

+ (NSString *)MOCKITO_CONFIGURATION_CLASS_NAME;

#pragma mark Public

- (instancetype __nonnull)init;

/*!
 @return configuration loaded from classpath or null
 */
- (id<OrgMockitoConfigurationIMockitoConfiguration>)loadConfiguration;

@end

J2OBJC_EMPTY_STATIC_INIT(OrgMockitoInternalConfigurationClassPathLoader)

inline NSString *OrgMockitoInternalConfigurationClassPathLoader_get_MOCKITO_CONFIGURATION_CLASS_NAME(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT NSString *OrgMockitoInternalConfigurationClassPathLoader_MOCKITO_CONFIGURATION_CLASS_NAME;
J2OBJC_STATIC_FIELD_OBJ_FINAL(OrgMockitoInternalConfigurationClassPathLoader, MOCKITO_CONFIGURATION_CLASS_NAME, NSString *)

FOUNDATION_EXPORT void OrgMockitoInternalConfigurationClassPathLoader_init(OrgMockitoInternalConfigurationClassPathLoader *self);

FOUNDATION_EXPORT OrgMockitoInternalConfigurationClassPathLoader *new_OrgMockitoInternalConfigurationClassPathLoader_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT OrgMockitoInternalConfigurationClassPathLoader *create_OrgMockitoInternalConfigurationClassPathLoader_init(void);

J2OBJC_TYPE_LITERAL_HEADER(OrgMockitoInternalConfigurationClassPathLoader)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_OrgMockitoInternalConfigurationClassPathLoader")
