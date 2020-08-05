//
//  Generated by the J2ObjC translator.  DO NOT EDIT!
//  source: /Users/tball/src/j2objc/guava/android/build_result/java/com/google/common/reflect/ClassPath.java
//

#include "J2ObjC_header.h"

#pragma push_macro("INCLUDE_ALL_ComGoogleCommonReflectClassPath")
#ifdef RESTRICT_ComGoogleCommonReflectClassPath
#define INCLUDE_ALL_ComGoogleCommonReflectClassPath 0
#else
#define INCLUDE_ALL_ComGoogleCommonReflectClassPath 1
#endif
#undef RESTRICT_ComGoogleCommonReflectClassPath
#ifdef INCLUDE_ComGoogleCommonReflectClassPath_DefaultScanner
#define INCLUDE_ComGoogleCommonReflectClassPath_Scanner 1
#endif
#ifdef INCLUDE_ComGoogleCommonReflectClassPath_ClassInfo
#define INCLUDE_ComGoogleCommonReflectClassPath_ResourceInfo 1
#endif

#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

#if __has_feature(nullability)
#pragma clang diagnostic push
#pragma GCC diagnostic ignored "-Wnullability"
#pragma GCC diagnostic ignored "-Wnullability-completeness"
#endif

#if !defined (ComGoogleCommonReflectClassPath_) && (INCLUDE_ALL_ComGoogleCommonReflectClassPath || defined(INCLUDE_ComGoogleCommonReflectClassPath))
#define ComGoogleCommonReflectClassPath_

@class ComGoogleCommonCollectImmutableSet;
@class JavaIoFile;
@class JavaLangClassLoader;
@class JavaNetURL;

/*!
 @brief Scans the source of a <code>ClassLoader</code> and finds all loadable classes and resources.
 <p><b>Warning:</b> Current limitations: 
 <ul>
    <li>Looks only for files and JARs in URLs available from <code>URLClassLoader</code> instances or
        the system class loader.
    <li>Only understands <code>file:</code> URLs. 
 </ul>
  
 <p>In the case of directory classloaders, symlinks are supported but cycles are not traversed.
  This guarantees discovery of each <em>unique</em> loadable resource. However, not all possible
  aliases for resources on cyclic paths will be listed.
 @author Ben Yu
 @since 14.0
 */
@interface ComGoogleCommonReflectClassPath : NSObject

#pragma mark Public

/*!
 @brief Returns a <code>ClassPath</code> representing all classes and resources loadable from <code>classloader</code>
  and its ancestor class loaders.
 <p><b>Warning:</b> <code>ClassPath</code> can find classes and resources only from: 
 <ul>
    <li><code>URLClassLoader</code> instances' <code>file:</code> URLs
    <li>the system class loader. To search the
        system class loader even when it is not a <code>URLClassLoader</code> (as in Java 9), <code>ClassPath</code>
  searches the files from the <code>java.class.path</code> system property. 
 </ul>
 @throw IOExceptionif the attempt to read class path resources (jar files or directories)
      failed.
 */
+ (ComGoogleCommonReflectClassPath *)fromWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader;

/*!
 @brief Returns all classes loadable from the current class path.
 @since 16.0
 */
- (ComGoogleCommonCollectImmutableSet *)getAllClasses;

/*!
 @brief Returns all resources loadable from the current class path, including the class files of all
  loadable classes but excluding the "META-INF/MANIFEST.MF" file.
 */
- (ComGoogleCommonCollectImmutableSet *)getResources;

/*!
 @brief Returns all top level classes loadable from the current class path.
 */
- (ComGoogleCommonCollectImmutableSet *)getTopLevelClasses;

/*!
 @brief Returns all top level classes whose package name is <code>packageName</code>.
 */
- (ComGoogleCommonCollectImmutableSet *)getTopLevelClassesWithNSString:(NSString * __nonnull)packageName;

/*!
 @brief Returns all top level classes whose package name is <code>packageName</code> or starts with <code>packageName</code>
  followed by a '.'.
 */
- (ComGoogleCommonCollectImmutableSet *)getTopLevelClassesRecursiveWithNSString:(NSString * __nonnull)packageName;

#pragma mark Package-Private

+ (NSString *)getClassNameWithNSString:(NSString * __nonnull)filename;

+ (JavaIoFile *)toFileWithJavaNetURL:(JavaNetURL * __nonnull)url;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_STATIC_INIT(ComGoogleCommonReflectClassPath)

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath *ComGoogleCommonReflectClassPath_fromWithJavaLangClassLoader_(JavaLangClassLoader *classloader);

FOUNDATION_EXPORT NSString *ComGoogleCommonReflectClassPath_getClassNameWithNSString_(NSString *filename);

FOUNDATION_EXPORT JavaIoFile *ComGoogleCommonReflectClassPath_toFileWithJavaNetURL_(JavaNetURL *url);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonReflectClassPath)

#endif

#if !defined (ComGoogleCommonReflectClassPath_ResourceInfo_) && (INCLUDE_ALL_ComGoogleCommonReflectClassPath || defined(INCLUDE_ComGoogleCommonReflectClassPath_ResourceInfo))
#define ComGoogleCommonReflectClassPath_ResourceInfo_

@class ComGoogleCommonIoByteSource;
@class ComGoogleCommonIoCharSource;
@class JavaLangClassLoader;
@class JavaNetURL;
@class JavaNioCharsetCharset;

/*!
 @brief Represents a class path resource that can be either a class file or any other resource file
  loadable from the class path.
 @since 14.0
 */
@interface ComGoogleCommonReflectClassPath_ResourceInfo : NSObject {
 @public
  JavaLangClassLoader *loader_;
}

#pragma mark Public

/*!
 @brief Returns a <code>ByteSource</code> view of the resource from which its bytes can be read.
 @throw NoSuchElementExceptionif the resource cannot be loaded through the class loader,
      despite physically existing in the class path.
 @since 20.0
 */
- (ComGoogleCommonIoByteSource *)asByteSource;

/*!
 @brief Returns a <code>CharSource</code> view of the resource from which its bytes can be read as
  characters decoded with the given <code>charset</code>.
 @throw NoSuchElementExceptionif the resource cannot be loaded through the class loader,
      despite physically existing in the class path.
 @since 20.0
 */
- (ComGoogleCommonIoCharSource *)asCharSourceWithJavaNioCharsetCharset:(JavaNioCharsetCharset * __nonnull)charset;

- (jboolean)isEqual:(id __nonnull)obj;

/*!
 @brief Returns the fully qualified name of the resource.Such as "com/mycomp/foo/bar.txt".
 */
- (NSString *)getResourceName;

- (NSUInteger)hash;

- (NSString *)description;

/*!
 @brief Returns the url identifying the resource.
 <p>See <code>ClassLoader.getResource</code>
 @throw NoSuchElementExceptionif the resource cannot be loaded through the class loader,
      despite physically existing in the class path.
 */
- (JavaNetURL *)url;

#pragma mark Package-Private

- (instancetype __nonnull)initWithNSString:(NSString * __nonnull)resourceName
                   withJavaLangClassLoader:(JavaLangClassLoader * __nonnull)loader;

+ (ComGoogleCommonReflectClassPath_ResourceInfo *)ofWithNSString:(NSString * __nonnull)resourceName
                                         withJavaLangClassLoader:(JavaLangClassLoader * __nonnull)loader;

// Disallowed inherited constructors, do not use.

- (instancetype __nonnull)init NS_UNAVAILABLE;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonReflectClassPath_ResourceInfo)

J2OBJC_FIELD_SETTER(ComGoogleCommonReflectClassPath_ResourceInfo, loader_, JavaLangClassLoader *)

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_ResourceInfo *ComGoogleCommonReflectClassPath_ResourceInfo_ofWithNSString_withJavaLangClassLoader_(NSString *resourceName, JavaLangClassLoader *loader);

FOUNDATION_EXPORT void ComGoogleCommonReflectClassPath_ResourceInfo_initWithNSString_withJavaLangClassLoader_(ComGoogleCommonReflectClassPath_ResourceInfo *self, NSString *resourceName, JavaLangClassLoader *loader);

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_ResourceInfo *new_ComGoogleCommonReflectClassPath_ResourceInfo_initWithNSString_withJavaLangClassLoader_(NSString *resourceName, JavaLangClassLoader *loader) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_ResourceInfo *create_ComGoogleCommonReflectClassPath_ResourceInfo_initWithNSString_withJavaLangClassLoader_(NSString *resourceName, JavaLangClassLoader *loader);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonReflectClassPath_ResourceInfo)

#endif

#if !defined (ComGoogleCommonReflectClassPath_ClassInfo_) && (INCLUDE_ALL_ComGoogleCommonReflectClassPath || defined(INCLUDE_ComGoogleCommonReflectClassPath_ClassInfo))
#define ComGoogleCommonReflectClassPath_ClassInfo_

@class IOSClass;
@class JavaLangClassLoader;

/*!
 @brief Represents a class that can be loaded through <code>load</code>.
 @since 14.0
 */
@interface ComGoogleCommonReflectClassPath_ClassInfo : ComGoogleCommonReflectClassPath_ResourceInfo

#pragma mark Public

/*!
 @brief Returns the fully qualified name of the class.
 <p>Behaves identically to <code>Class.getName()</code> but does not require the class to be
  loaded.
 */
- (NSString *)getName;

/*!
 @brief Returns the package name of the class, without attempting to load the class.
 <p>Behaves identically to <code>Package.getName()</code> but does not require the class (or
  package) to be loaded.
 */
- (NSString *)getPackageName;

/*!
 @brief Returns the simple name of the underlying class as given in the source code.
 <p>Behaves identically to <code>Class.getSimpleName()</code> but does not require the class to be
  loaded.
 */
- (NSString *)getSimpleName;

/*!
 @brief Loads (but doesn't link or initialize) the class.
 @throw LinkageErrorwhen there were errors in loading classes that this class depends on.
      For example, <code>NoClassDefFoundError</code>.
 */
- (IOSClass *)load__;

- (NSString *)description;

#pragma mark Package-Private

- (instancetype __nonnull)initWithNSString:(NSString * __nonnull)resourceName
                   withJavaLangClassLoader:(JavaLangClassLoader * __nonnull)loader;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonReflectClassPath_ClassInfo)

FOUNDATION_EXPORT void ComGoogleCommonReflectClassPath_ClassInfo_initWithNSString_withJavaLangClassLoader_(ComGoogleCommonReflectClassPath_ClassInfo *self, NSString *resourceName, JavaLangClassLoader *loader);

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_ClassInfo *new_ComGoogleCommonReflectClassPath_ClassInfo_initWithNSString_withJavaLangClassLoader_(NSString *resourceName, JavaLangClassLoader *loader) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_ClassInfo *create_ComGoogleCommonReflectClassPath_ClassInfo_initWithNSString_withJavaLangClassLoader_(NSString *resourceName, JavaLangClassLoader *loader);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonReflectClassPath_ClassInfo)

#endif

#if !defined (ComGoogleCommonReflectClassPath_Scanner_) && (INCLUDE_ALL_ComGoogleCommonReflectClassPath || defined(INCLUDE_ComGoogleCommonReflectClassPath_Scanner))
#define ComGoogleCommonReflectClassPath_Scanner_

@class ComGoogleCommonCollectImmutableList;
@class ComGoogleCommonCollectImmutableMap;
@class ComGoogleCommonCollectImmutableSet;
@class JavaIoFile;
@class JavaLangClassLoader;
@class JavaNetURL;
@class JavaUtilJarJarFile;
@class JavaUtilJarManifest;

/*!
 @brief Abstract class that scans through the class path represented by a <code>ClassLoader</code> and calls 
 <code>scanDirectory</code> and <code>scanJarFile</code> for directories and jar files on the class path
  respectively.
 */
@interface ComGoogleCommonReflectClassPath_Scanner : NSObject

#pragma mark Public

- (void)scanWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader;

#pragma mark Protected

/*!
 @brief Called when a directory is scanned for resource files.
 */
- (void)scanDirectoryWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)loader
                              withJavaIoFile:(JavaIoFile * __nonnull)directory;

/*!
 @brief Called when a jar file is scanned for resource entries.
 */
- (void)scanJarFileWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)loader
                    withJavaUtilJarJarFile:(JavaUtilJarJarFile * __nonnull)file;

#pragma mark Package-Private

- (instancetype __nonnull)init;

+ (ComGoogleCommonCollectImmutableMap *)getClassPathEntriesWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader;

/*!
 @brief Returns the absolute uri of the Class-Path entry value as specified in <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">
 JAR
  File Specification</a>.Even though the specification only talks about relative urls,
  absolute urls are actually supported too (for example, in Maven surefire plugin).
 */
+ (JavaNetURL *)getClassPathEntryWithJavaIoFile:(JavaIoFile * __nonnull)jarFile
                                   withNSString:(NSString * __nonnull)path;

/*!
 @brief Returns the class path URIs specified by the <code>Class-Path</code> manifest attribute, according
  to <a href="http://docs.oracle.com/javase/8/docs/technotes/guides/jar/jar.html#Main_Attributes">
 JAR
  File Specification</a>.If <code>manifest</code> is null, it means the jar file has no manifest,
  and an empty set will be returned.
 */
+ (ComGoogleCommonCollectImmutableSet *)getClassPathFromManifestWithJavaIoFile:(JavaIoFile * __nonnull)jarFile
                                                       withJavaUtilJarManifest:(JavaUtilJarManifest * __nullable)manifest;

/*!
 @brief Returns the URLs in the class path specified by the <code>java.class.path</code> system property
 .
 */
+ (ComGoogleCommonCollectImmutableList *)parseJavaClassPath;

- (void)scanWithJavaIoFile:(JavaIoFile * __nonnull)file
   withJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonReflectClassPath_Scanner)

FOUNDATION_EXPORT void ComGoogleCommonReflectClassPath_Scanner_init(ComGoogleCommonReflectClassPath_Scanner *self);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableSet *ComGoogleCommonReflectClassPath_Scanner_getClassPathFromManifestWithJavaIoFile_withJavaUtilJarManifest_(JavaIoFile *jarFile, JavaUtilJarManifest *manifest);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableMap *ComGoogleCommonReflectClassPath_Scanner_getClassPathEntriesWithJavaLangClassLoader_(JavaLangClassLoader *classloader);

FOUNDATION_EXPORT ComGoogleCommonCollectImmutableList *ComGoogleCommonReflectClassPath_Scanner_parseJavaClassPath(void);

FOUNDATION_EXPORT JavaNetURL *ComGoogleCommonReflectClassPath_Scanner_getClassPathEntryWithJavaIoFile_withNSString_(JavaIoFile *jarFile, NSString *path);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonReflectClassPath_Scanner)

#endif

#if !defined (ComGoogleCommonReflectClassPath_DefaultScanner_) && (INCLUDE_ALL_ComGoogleCommonReflectClassPath || defined(INCLUDE_ComGoogleCommonReflectClassPath_DefaultScanner))
#define ComGoogleCommonReflectClassPath_DefaultScanner_

@class ComGoogleCommonCollectImmutableSet;
@class JavaIoFile;
@class JavaLangClassLoader;
@class JavaUtilJarJarFile;

@interface ComGoogleCommonReflectClassPath_DefaultScanner : ComGoogleCommonReflectClassPath_Scanner

#pragma mark Protected

- (void)scanDirectoryWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader
                              withJavaIoFile:(JavaIoFile * __nonnull)directory;

- (void)scanJarFileWithJavaLangClassLoader:(JavaLangClassLoader * __nonnull)classloader
                    withJavaUtilJarJarFile:(JavaUtilJarJarFile * __nonnull)file;

#pragma mark Package-Private

- (instancetype __nonnull)init;

- (ComGoogleCommonCollectImmutableSet *)getResources;

@end

J2OBJC_EMPTY_STATIC_INIT(ComGoogleCommonReflectClassPath_DefaultScanner)

FOUNDATION_EXPORT void ComGoogleCommonReflectClassPath_DefaultScanner_init(ComGoogleCommonReflectClassPath_DefaultScanner *self);

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_DefaultScanner *new_ComGoogleCommonReflectClassPath_DefaultScanner_init(void) NS_RETURNS_RETAINED;

FOUNDATION_EXPORT ComGoogleCommonReflectClassPath_DefaultScanner *create_ComGoogleCommonReflectClassPath_DefaultScanner_init(void);

J2OBJC_TYPE_LITERAL_HEADER(ComGoogleCommonReflectClassPath_DefaultScanner)

#endif


#if __has_feature(nullability)
#pragma clang diagnostic pop
#endif

#pragma clang diagnostic pop
#pragma pop_macro("INCLUDE_ALL_ComGoogleCommonReflectClassPath")
