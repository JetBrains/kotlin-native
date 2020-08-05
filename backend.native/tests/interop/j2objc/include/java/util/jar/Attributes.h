//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: android/platform/libcore/ojluni/src/main/java/java/util/jar/Attributes.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_JavaUtilJarAttributes")
#ifdef RESTRICT_JavaUtilJarAttributes
#define INCLUDE_ALL_JavaUtilJarAttributes 0
#else
#define INCLUDE_ALL_JavaUtilJarAttributes 1
#endif
#undef RESTRICT_JavaUtilJarAttributes

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (JavaUtilJarAttributes_) && (INCLUDE_ALL_JavaUtilJarAttributes || defined(INCLUDE_JavaUtilJarAttributes))
#define JavaUtilJarAttributes_

#define RESTRICT_JavaUtilMap 1
#define INCLUDE_JavaUtilMap 1
#include "java/util/Map.h"

@class IOSByteArray;
@class JavaIoDataOutputStream;
@class JavaUtilJarAttributes_Name;
@class JavaUtilJarManifest_FastInputStream;
@protocol JavaUtilCollection;
@protocol JavaUtilFunctionBiConsumer;
@protocol JavaUtilFunctionBiFunction;
@protocol JavaUtilFunctionFunction;
@protocol JavaUtilSet;

/*!
 @brief The Attributes class maps Manifest attribute names to associated string
  values.Valid attribute names are case-insensitive, are restricted to
  the ASCII characters in the set [0-9a-zA-Z_-], and cannot exceed 70
  characters in length.
 Attribute values can contain any characters and
  will be UTF8-encoded when written to the output stream.  See the 
 <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html">JAR File Specification</a>
  for more information about valid attribute names and values.
 @author David Connelly
 - seealso: Manifest
 @since 1.2
 */
@interface JavaUtilJarAttributes : NSObject < JavaUtilMap, NSCopying > {
 @public
  /*!
   @brief The attribute name-value mappings.
   */
  id<JavaUtilMap> map_;
}

#pragma mark Public

/*!
 @brief Constructs a new, empty Attributes object with default size.
 */
- (instancetype __nonnull)init;

/*!
 @brief Constructs a new Attributes object with the same attribute name-value
  mappings as in the specified Attributes.
 @param attr the specified Attributes
 */
- (instancetype __nonnull)initWithJavaUtilJarAttributes:(JavaUtilJarAttributes *)attr;

/*!
 @brief Constructs a new, empty Attributes object with the specified
  initial size.
 @param size the initial number of attributes
 */
- (instancetype __nonnull)initWithInt:(jint)size;

/*!
 @brief Removes all attributes from this Map.
 */
- (void)clear;

/*!
 @brief Returns a copy of the Attributes, implemented as follows:
 @code

      public Object clone() { return new Attributes(this); } 
  
@endcode
  Since the attribute names and values are themselves immutable,
  the Attributes returned can be safely modified without affecting
  the original.
 */
- (id)java_clone;

/*!
 @brief Returns true if this Map contains the specified attribute name (key).
 @param name the attribute name
 @return true if this Map contains the specified attribute name
 */
- (jboolean)containsKeyWithId:(id)name;

/*!
 @brief Returns true if this Map maps one or more attribute names (keys)
  to the specified value.
 @param value the attribute value
 @return true if this Map maps one or more attribute names to
          the specified value
 */
- (jboolean)containsValueWithId:(id)value;

/*!
 @brief Returns a Collection view of the attribute name-value mappings
  contained in this Map.
 */
- (id<JavaUtilSet>)entrySet;

/*!
 @brief Compares the specified Attributes object with this Map for equality.
 Returns true if the given object is also an instance of Attributes
  and the two Attributes objects represent the same mappings.
 @param o the Object to be compared
 @return true if the specified Object is equal to this Map
 */
- (jboolean)isEqual:(id)o;

/*!
 @brief Returns the value of the specified attribute name, or null if the
  attribute name was not found.
 @param name the attribute name
 @return the value of the specified attribute name, or null if
          not found.
 */
- (id)getWithId:(id)name;

/*!
 @brief Returns the value of the specified Attributes.Name, or null if the
  attribute was not found.
 <p>
  This method is defined as: 
 @code

      return (String)get(name); 
  
@endcode
 @param name the Attributes.Name object
 @return the String value of the specified Attribute.Name, or null if
          not found.
 */
- (NSString *)getValueWithJavaUtilJarAttributes_Name:(JavaUtilJarAttributes_Name *)name;

/*!
 @brief Returns the value of the specified attribute name, specified as
  a string, or null if the attribute was not found.The attribute
  name is case-insensitive.
 <p>
  This method is defined as: 
 @code

       return (String)get(new Attributes.Name((String)name)); 
  
@endcode
 @param name the attribute name as a string
 @return the String value of the specified attribute name, or null if
          not found.
 @throw IllegalArgumentExceptionif the attribute name is invalid
 */
- (NSString *)getValueWithNSString:(NSString *)name;

/*!
 @brief Returns the hash code value for this Map.
 */
- (NSUInteger)hash;

/*!
 @brief Returns true if this Map contains no attributes.
 */
- (jboolean)isEmpty;

/*!
 @brief Returns a Set view of the attribute names (keys) contained in this Map.
 */
- (id<JavaUtilSet>)keySet;

/*!
 @brief Associates the specified value with the specified attribute name
  (key) in this Map.If the Map previously contained a mapping for
  the attribute name, the old value is replaced.
 @param name the attribute name
 @param value the attribute value
 @return the previous value of the attribute, or null if none
 @throw ClassCastExceptionif the name is not a Attributes.Name
             or the value is not a String
 */
- (id)putWithId:(id)name
         withId:(id)value;

/*!
 @brief Copies all of the attribute name-value mappings from the specified
  Attributes to this Map.Duplicate mappings will be replaced.
 @param attr the Attributes to be stored in this map
 @throw ClassCastExceptionif attr is not an Attributes
 */
- (void)putAllWithJavaUtilMap:(id<JavaUtilMap>)attr;

/*!
 @brief Associates the specified value with the specified attribute name,
  specified as a String.The attributes name is case-insensitive.
 If the Map previously contained a mapping for the attribute name,
  the old value is replaced. 
 <p>
  This method is defined as: 
 @code

       return (String)put(new Attributes.Name(name), value); 
  
@endcode
 @param name the attribute name as a string
 @param value the attribute value
 @return the previous value of the attribute, or null if none
 @throw IllegalArgumentExceptionif the attribute name is invalid
 */
- (NSString *)putValueWithNSString:(NSString *)name
                      withNSString:(NSString *)value;

/*!
 @brief Removes the attribute with the specified name (key) from this Map.
 Returns the previous attribute value, or null if none.
 @param name attribute name
 @return the previous value of the attribute, or null if none
 */
- (id)removeWithId:(id)name;

/*!
 @brief Returns the number of attributes in this Map.
 */
- (jint)size;

/*!
 @brief Returns a Collection view of the attribute values contained in this Map.
 */
- (id<JavaUtilCollection>)values;

#pragma mark Package-Private

- (void)readWithJavaUtilJarManifest_FastInputStream:(JavaUtilJarManifest_FastInputStream *)is
                                      withByteArray:(IOSByteArray *)lbuf;

- (void)writeWithJavaIoDataOutputStream:(JavaIoDataOutputStream *)os;

- (void)writeMainWithJavaIoDataOutputStream:(JavaIoDataOutputStream *)outArg;

@end

J2OBJC_EMPTY_STATIC_INIT(JavaUtilJarAttributes)

J2OBJC_FIELD_SETTER(JavaUtilJarAttributes, map_, id<JavaUtilMap>)

FOUNDATION_EXPORT void JavaUtilJarAttributes_init(JavaUtilJarAttributes *self);

FOUNDATION_EXPORT JavaUtilJarAttributes *new_JavaUtilJarAttributes_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilJarAttributes *create_JavaUtilJarAttributes_init(void);

FOUNDATION_EXPORT void JavaUtilJarAttributes_initWithInt_(JavaUtilJarAttributes *self, jint size);

FOUNDATION_EXPORT JavaUtilJarAttributes *new_JavaUtilJarAttributes_initWithInt_(jint size) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilJarAttributes *create_JavaUtilJarAttributes_initWithInt_(jint size);

FOUNDATION_EXPORT void JavaUtilJarAttributes_initWithJavaUtilJarAttributes_(JavaUtilJarAttributes *self, JavaUtilJarAttributes *attr);

FOUNDATION_EXPORT JavaUtilJarAttributes *new_JavaUtilJarAttributes_initWithJavaUtilJarAttributes_(JavaUtilJarAttributes *attr) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilJarAttributes *create_JavaUtilJarAttributes_initWithJavaUtilJarAttributes_(JavaUtilJarAttributes *attr);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilJarAttributes)

#endif

#if !defined (JavaUtilJarAttributes_Name_) && (INCLUDE_ALL_JavaUtilJarAttributes || defined(INCLUDE_JavaUtilJarAttributes_Name))
#define JavaUtilJarAttributes_Name_

/*!
 @brief The Attributes.Name class represents an attribute name stored in
  this Map.Valid attribute names are case-insensitive, are restricted
  to the ASCII characters in the set [0-9a-zA-Z_-], and cannot exceed
  70 characters in length.
 Attribute values can contain any characters
  and will be UTF8-encoded when written to the output stream.  See the 
 <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html">JAR File Specification</a>
  for more information about valid attribute names and values.
 */
@interface JavaUtilJarAttributes_Name : NSObject
@property (readonly, class, strong) JavaUtilJarAttributes_Name *MANIFEST_VERSION NS_SWIFT_NAME(MANIFEST_VERSION);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *SIGNATURE_VERSION NS_SWIFT_NAME(SIGNATURE_VERSION);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *CONTENT_TYPE NS_SWIFT_NAME(CONTENT_TYPE);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *CLASS_PATH NS_SWIFT_NAME(CLASS_PATH);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *MAIN_CLASS NS_SWIFT_NAME(MAIN_CLASS);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *SEALED NS_SWIFT_NAME(SEALED);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *EXTENSION_LIST NS_SWIFT_NAME(EXTENSION_LIST);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *EXTENSION_NAME NS_SWIFT_NAME(EXTENSION_NAME);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *EXTENSION_INSTALLATION NS_SWIFT_NAME(EXTENSION_INSTALLATION);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *IMPLEMENTATION_TITLE NS_SWIFT_NAME(IMPLEMENTATION_TITLE);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *IMPLEMENTATION_VERSION NS_SWIFT_NAME(IMPLEMENTATION_VERSION);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *IMPLEMENTATION_VENDOR NS_SWIFT_NAME(IMPLEMENTATION_VENDOR);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *IMPLEMENTATION_VENDOR_ID NS_SWIFT_NAME(IMPLEMENTATION_VENDOR_ID);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *IMPLEMENTATION_URL NS_SWIFT_NAME(IMPLEMENTATION_URL);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *SPECIFICATION_TITLE NS_SWIFT_NAME(SPECIFICATION_TITLE);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *SPECIFICATION_VERSION NS_SWIFT_NAME(SPECIFICATION_VERSION);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *SPECIFICATION_VENDOR NS_SWIFT_NAME(SPECIFICATION_VENDOR);
@property (readonly, class, strong) JavaUtilJarAttributes_Name *NAME NS_SWIFT_NAME(NAME);

+ (JavaUtilJarAttributes_Name *)MANIFEST_VERSION;

+ (JavaUtilJarAttributes_Name *)SIGNATURE_VERSION;

+ (JavaUtilJarAttributes_Name *)CONTENT_TYPE;

+ (JavaUtilJarAttributes_Name *)CLASS_PATH;

+ (JavaUtilJarAttributes_Name *)MAIN_CLASS;

+ (JavaUtilJarAttributes_Name *)SEALED;

+ (JavaUtilJarAttributes_Name *)EXTENSION_LIST;

+ (JavaUtilJarAttributes_Name *)EXTENSION_NAME;

+ (JavaUtilJarAttributes_Name *)EXTENSION_INSTALLATION;

+ (JavaUtilJarAttributes_Name *)IMPLEMENTATION_TITLE;

+ (JavaUtilJarAttributes_Name *)IMPLEMENTATION_VERSION;

+ (JavaUtilJarAttributes_Name *)IMPLEMENTATION_VENDOR;

+ (JavaUtilJarAttributes_Name *)IMPLEMENTATION_VENDOR_ID;

+ (JavaUtilJarAttributes_Name *)IMPLEMENTATION_URL;

+ (JavaUtilJarAttributes_Name *)SPECIFICATION_TITLE;

+ (JavaUtilJarAttributes_Name *)SPECIFICATION_VERSION;

+ (JavaUtilJarAttributes_Name *)SPECIFICATION_VENDOR;

+ (JavaUtilJarAttributes_Name *)NAME;

#pragma mark Public

/*!
 @brief Constructs a new attribute name using the given string name.
 @param name the attribute string name
 @throw IllegalArgumentExceptionif the attribute name was
             invalid
 @throw NullPointerExceptionif the attribute name was null
 */
- (instancetype __nonnull)initWithNSString:(NSString *)name;

/*!
 @brief Compares this attribute name to another for equality.
 @param o the object to compare
 @return true if this attribute name is equal to the
          specified attribute object
 */
- (jboolean)isEqual:(id)o;

/*!
 @brief Computes the hash value for this attribute name.
 */
- (NSUInteger)hash;

/*!
 @brief Returns the attribute name as a String.
 */
- (NSString *)description;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(JavaUtilJarAttributes_Name)

/*!
 @brief <code>Name</code> object for <code>Manifest-Version</code>
  manifest attribute.This attribute indicates the version number
  of the manifest standard to which a JAR file's manifest conforms.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html#JAR Manifest">
       Manifest and Signature Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_MANIFEST_VERSION(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_MANIFEST_VERSION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, MANIFEST_VERSION, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Signature-Version</code>
  manifest attribute used when signing JAR files.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html#JAR Manifest">
       Manifest and Signature Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_SIGNATURE_VERSION(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_SIGNATURE_VERSION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, SIGNATURE_VERSION, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Content-Type</code>
  manifest attribute.
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_CONTENT_TYPE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_CONTENT_TYPE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, CONTENT_TYPE, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Class-Path</code>
  manifest attribute.Bundled extensions can use this attribute
  to find other JAR files containing needed classes.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html#classpath">
       JAR file specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_CLASS_PATH(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_CLASS_PATH;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, CLASS_PATH, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Main-Class</code> manifest
  attribute used for launching applications packaged in JAR files.
 The <code>Main-Class</code> attribute is used in conjunction
  with the <code>-jar</code> command-line option of the 
 <tt>java</tt> application launcher.
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_MAIN_CLASS(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_MAIN_CLASS;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, MAIN_CLASS, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Sealed</code> manifest attribute
  used for sealing.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/jar/jar.html#sealing">
       Package Sealing</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_SEALED(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_SEALED;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, SEALED, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Extension-List</code> manifest attribute
  used for declaring dependencies on installed extensions.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/extensions/spec.html#dependency">
       Installed extension dependency</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_EXTENSION_LIST(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_EXTENSION_LIST;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, EXTENSION_LIST, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Extension-Name</code> manifest attribute
  used for declaring dependencies on installed extensions.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/extensions/spec.html#dependency">
       Installed extension dependency</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_EXTENSION_NAME(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_EXTENSION_NAME;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, EXTENSION_NAME, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Extension-Name</code> manifest attribute
  used for declaring dependencies on installed extensions.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/extensions/spec.html#dependency">
       Installed extension dependency</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_EXTENSION_INSTALLATION(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_EXTENSION_INSTALLATION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, EXTENSION_INSTALLATION, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Implementation-Title</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_IMPLEMENTATION_TITLE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_IMPLEMENTATION_TITLE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, IMPLEMENTATION_TITLE, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Implementation-Version</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_IMPLEMENTATION_VERSION(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_IMPLEMENTATION_VERSION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, IMPLEMENTATION_VERSION, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Implementation-Vendor</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_IMPLEMENTATION_VENDOR(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_IMPLEMENTATION_VENDOR;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, IMPLEMENTATION_VENDOR, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Implementation-Vendor-Id</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/extensions/versioning.html#applet">
       Optional Package Versioning</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_IMPLEMENTATION_VENDOR_ID(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_IMPLEMENTATION_VENDOR_ID;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, IMPLEMENTATION_VENDOR_ID, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Implementation-URL</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/extensions/versioning.html#applet">
       Optional Package Versioning</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_IMPLEMENTATION_URL(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_IMPLEMENTATION_URL;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, IMPLEMENTATION_URL, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Specification-Title</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_SPECIFICATION_TITLE(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_SPECIFICATION_TITLE;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, SPECIFICATION_TITLE, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Specification-Version</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_SPECIFICATION_VERSION(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_SPECIFICATION_VERSION;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, SPECIFICATION_VERSION, JavaUtilJarAttributes_Name *)

/*!
 @brief <code>Name</code> object for <code>Specification-Vendor</code>
  manifest attribute used for package versioning.
 - seealso: <a href="{@@docRoot}openjdk-redirect.html?v=8&path=/technotes/guides/versioning/spec/versioning2.html#wp90779">
       Java Product Versioning Specification</a>
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_SPECIFICATION_VENDOR(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_SPECIFICATION_VENDOR;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, SPECIFICATION_VENDOR, JavaUtilJarAttributes_Name *)

/*!
 */
inline JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_get_NAME(void);
/*! INTERNAL ONLY - Use accessor function from above. */
FOUNDATION_EXPORT JavaUtilJarAttributes_Name *JavaUtilJarAttributes_Name_NAME;
J2OBJC_STATIC_FIELD_OBJ_FINAL(JavaUtilJarAttributes_Name, NAME, JavaUtilJarAttributes_Name *)

FOUNDATION_EXPORT void JavaUtilJarAttributes_Name_initWithNSString_(JavaUtilJarAttributes_Name *self, NSString *name);

FOUNDATION_EXPORT JavaUtilJarAttributes_Name *new_JavaUtilJarAttributes_Name_initWithNSString_(NSString *name) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT JavaUtilJarAttributes_Name *create_JavaUtilJarAttributes_Name_initWithNSString_(NSString *name);

J2OBJC_TYPE_LITERAL_HEADER(JavaUtilJarAttributes_Name)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_JavaUtilJarAttributes")
