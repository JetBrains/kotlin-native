/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * JNI specification, as defined by Sun:
 * http://java.sun.com/javase/6/docs/technotes/guides/jni/spec/jniTOC.html
 *
 * Everything here is expected to be VM-neutral.
 */

#ifndef JNI_H_
#define JNI_H_

#include <stdarg.h>
#include "J2ObjC_types.h"

/* "cardinal indices and sizes" */
typedef jint            jsize;

#if defined(__OBJC__)

@class IOSArray;
@class IOSObjectArray;
@class IOSBooleanArray;
@class IOSByteArray;
@class IOSCharArray;
@class IOSShortArray;
@class IOSIntArray;
@class IOSLongArray;
@class IOSFloatArray;
@class IOSDoubleArray;
@class IOSClass;

/*
 * Reference types, in Objective-C.
 */
typedef id               jobject;
typedef IOSClass*        jclass;
typedef NSString*        jstring;
typedef IOSArray*        jarray;
typedef IOSObjectArray*  jobjectArray;
typedef IOSBooleanArray* jbooleanArray;
typedef IOSByteArray*    jbyteArray;
typedef IOSCharArray*    jcharArray;
typedef IOSShortArray*   jshortArray;
typedef IOSIntArray*     jintArray;
typedef IOSLongArray*    jlongArray;
typedef IOSFloatArray*   jfloatArray;
typedef IOSDoubleArray*  jdoubleArray;
typedef jobject          jthrowable;
typedef jobject          jweak;

#elif defined(__cplusplus)
/*
 * Reference types, in C++
 */
class _jobject {};
class _jclass : public _jobject {};
class _jstring : public _jobject {};
class _jarray : public _jobject {};
class _jobjectArray : public _jarray {};
class _jbooleanArray : public _jarray {};
class _jbyteArray : public _jarray {};
class _jcharArray : public _jarray {};
class _jshortArray : public _jarray {};
class _jintArray : public _jarray {};
class _jlongArray : public _jarray {};
class _jfloatArray : public _jarray {};
class _jdoubleArray : public _jarray {};
class _jthrowable : public _jobject {};

typedef _jobject*       jobject;
typedef _jclass*        jclass;
typedef _jstring*       jstring;
typedef _jarray*        jarray;
typedef _jobjectArray*  jobjectArray;
typedef _jbooleanArray* jbooleanArray;
typedef _jbyteArray*    jbyteArray;
typedef _jcharArray*    jcharArray;
typedef _jshortArray*   jshortArray;
typedef _jintArray*     jintArray;
typedef _jlongArray*    jlongArray;
typedef _jfloatArray*   jfloatArray;
typedef _jdoubleArray*  jdoubleArray;
typedef _jthrowable*    jthrowable;
typedef _jobject*       jweak;


#else /* not __cplusplus */

/*
 * Reference types, in C.
 */
typedef void*           jobject;
typedef jobject         jclass;
typedef jobject         jstring;
typedef jobject         jarray;
typedef jarray          jobjectArray;
typedef jarray          jbooleanArray;
typedef jarray          jbyteArray;
typedef jarray          jcharArray;
typedef jarray          jshortArray;
typedef jarray          jintArray;
typedef jarray          jlongArray;
typedef jarray          jfloatArray;
typedef jarray          jdoubleArray;
typedef jobject         jthrowable;
typedef jobject         jweak;

#endif /* not __cplusplus */

typedef union jvalue {
    jboolean z;
    jbyte    b;
    jchar    c;
    jshort   s;
    jint     i;
    jlong    j;
    jfloat   f;
    jdouble  d;
#if __has_feature(objc_arc)
    __unsafe_unretained jobject  l;
#else
    jobject  l;
#endif
} jvalue;

struct _jfieldID;
typedef struct _jfieldID *jfieldID;

struct _jmethodID;
typedef struct _jmethodID *jmethodID;

/* Forward declaration for JNIEnv */
struct _JNIEnv;
typedef const struct JNINativeInterface* C_JNIEnv;
extern C_JNIEnv J2ObjC_JNIEnv;

#if defined(__cplusplus)
typedef _JNIEnv JNIEnv;
#else
typedef const struct JNINativeInterface* JNIEnv;
#endif

/* Forward declaration for JavaVM */
struct _JavaVM;
typedef const struct JNIInvokeInterface* C_JavaVM;
extern C_JavaVM J2ObjC_JavaVM;

#if defined(__cplusplus)
typedef _JavaVM JavaVM;
#else
typedef const struct JNIInvokeInterface* JavaVM;
#endif

/*
 * Table of interface function pointers for JNIEnv.
 */
struct JNINativeInterface {
  jint          (*GetVersion)(JNIEnv *);

  jclass        (*FindClass)(JNIEnv*, const char*);
  jclass        (*GetSuperclass)(JNIEnv*, jclass);
  jboolean      (*IsAssignableFrom)(JNIEnv*, jclass, jclass);
  jint          (*Throw)(JNIEnv*, jthrowable);
  jint          (*ThrowNew)(JNIEnv *, jclass, const char *);
  void          (*ExceptionClear)(JNIEnv *);

  jobject       (*NewGlobalRef)(JNIEnv*, jobject);
  jobject       (*NewLocalRef)(JNIEnv*, jobject);
  void          (*DeleteGlobalRef)(JNIEnv*, jobject);
  void          (*DeleteLocalRef)(JNIEnv*, jobject);
  jboolean      (*IsSameObject)(JNIEnv*, jobject, jobject);
  jclass        (*GetObjectClass)(JNIEnv*, jobject);
  jboolean      (*IsInstanceOf)(JNIEnv*, jobject, jclass);

  jstring       (*NewString)(JNIEnv*, const jchar*, jsize);
  jsize         (*GetStringLength)(JNIEnv*, jstring);
  const jchar*  (*GetStringChars)(JNIEnv*, jstring, jboolean*);
  void          (*ReleaseStringChars)(JNIEnv*, jstring, const jchar*);
  jstring       (*NewStringUTF)(JNIEnv*, const char*);
  jsize         (*GetStringUTFLength)(JNIEnv*, jstring);
  const char*   (*GetStringUTFChars)(JNIEnv*, jstring, jboolean*);
  void          (*ReleaseStringUTFChars)(JNIEnv*, jstring, const char*);

  jsize         (*GetArrayLength)(JNIEnv*, jarray);
  jobjectArray  (*NewObjectArray)(JNIEnv*, jsize, jclass, jobject);
  jobject       (*GetObjectArrayElement)(JNIEnv*, jobjectArray, jsize);
  void          (*SetObjectArrayElement)(JNIEnv*, jobjectArray, jsize, jobject);

  jbooleanArray (*NewBooleanArray)(JNIEnv*, jsize);
  jbyteArray    (*NewByteArray)(JNIEnv*, jsize);
  jcharArray    (*NewCharArray)(JNIEnv*, jsize);
  jshortArray   (*NewShortArray)(JNIEnv*, jsize);
  jintArray     (*NewIntArray)(JNIEnv*, jsize);
  jlongArray    (*NewLongArray)(JNIEnv*, jsize);
  jfloatArray   (*NewFloatArray)(JNIEnv*, jsize);
  jdoubleArray  (*NewDoubleArray)(JNIEnv*, jsize);

  jboolean*     (*GetBooleanArrayElements)(JNIEnv*, jbooleanArray, jboolean*);
  jbyte*        (*GetByteArrayElements)(JNIEnv*, jbyteArray, jboolean*);
  jchar*        (*GetCharArrayElements)(JNIEnv*, jcharArray, jboolean*);
  jshort*       (*GetShortArrayElements)(JNIEnv*, jshortArray, jboolean*);
  jint*         (*GetIntArrayElements)(JNIEnv*, jintArray, jboolean*);
  jlong*        (*GetLongArrayElements)(JNIEnv*, jlongArray, jboolean*);
  jfloat*       (*GetFloatArrayElements)(JNIEnv*, jfloatArray, jboolean*);
  jdouble*      (*GetDoubleArrayElements)(JNIEnv*, jdoubleArray, jboolean*);

  void          (*ReleaseBooleanArrayElements)(JNIEnv*, jbooleanArray, jboolean*, jint);
  void          (*ReleaseByteArrayElements)(JNIEnv*, jbyteArray, jbyte*, jint);
  void          (*ReleaseCharArrayElements)(JNIEnv*, jcharArray, jchar*, jint);
  void          (*ReleaseShortArrayElements)(JNIEnv*, jshortArray, jshort*, jint);
  void          (*ReleaseIntArrayElements)(JNIEnv*, jintArray, jint*, jint);
  void          (*ReleaseLongArrayElements)(JNIEnv*, jlongArray, jlong*, jint);
  void          (*ReleaseFloatArrayElements)(JNIEnv*, jfloatArray, jfloat*, jint);
  void          (*ReleaseDoubleArrayElements)(JNIEnv*, jdoubleArray, jdouble*, jint);

  void          (*GetBooleanArrayRegion)(JNIEnv*, jbooleanArray, jsize, jsize, jboolean*);
  void          (*GetByteArrayRegion)(JNIEnv*, jbyteArray, jsize, jsize, jbyte*);
  void          (*GetCharArrayRegion)(JNIEnv*, jcharArray, jsize, jsize, jchar*);
  void          (*GetShortArrayRegion)(JNIEnv*, jshortArray, jsize, jsize, jshort*);
  void          (*GetIntArrayRegion)(JNIEnv*, jintArray, jsize, jsize, jint*);
  void          (*GetLongArrayRegion)(JNIEnv*, jlongArray, jsize, jsize, jlong*);
  void          (*GetFloatArrayRegion)(JNIEnv*, jfloatArray, jsize, jsize, jfloat*);
  void          (*GetDoubleArrayRegion)(JNIEnv*, jdoubleArray, jsize, jsize, jdouble*);

  void          (*SetBooleanArrayRegion)(JNIEnv*, jbooleanArray, jsize, jsize, const jboolean*);
  void          (*SetByteArrayRegion)(JNIEnv*, jbyteArray, jsize, jsize, const jbyte*);
  void          (*SetCharArrayRegion)(JNIEnv*, jcharArray, jsize, jsize, const jchar*);
  void          (*SetShortArrayRegion)(JNIEnv*, jshortArray, jsize, jsize, const jshort*);
  void          (*SetIntArrayRegion)(JNIEnv*, jintArray, jsize, jsize, const jint*);
  void          (*SetLongArrayRegion)(JNIEnv*, jlongArray, jsize, jsize, const jlong*);
  void          (*SetFloatArrayRegion)(JNIEnv*, jfloatArray, jsize, jsize, const jfloat*);
  void          (*SetDoubleArrayRegion)(JNIEnv*, jdoubleArray, jsize, jsize, const jdouble*);

  void          (*GetStringRegion)(JNIEnv*, jstring, jsize, jsize, jchar*);
  void          (*GetStringUTFRegion)(JNIEnv*, jstring, jsize, jsize, char*);
  void*         (*GetPrimitiveArrayCritical)(JNIEnv*, jarray, jboolean*);
  void          (*ReleasePrimitiveArrayCritical)(JNIEnv*, jarray, void*, jint);
  const jchar*  (*GetStringCritical)(JNIEnv*, jstring, jboolean*);
  void          (*ReleaseStringCritical)(JNIEnv*, jstring, const jchar*);

  jobject       (*NewDirectByteBuffer)(JNIEnv*, void*, jlong);
  void*         (*GetDirectBufferAddress)(JNIEnv*, jobject);
  jlong         (*GetDirectBufferCapacity)(JNIEnv*, jobject);

  jfieldID      (*GetFieldID)(JNIEnv*, jclass, const char*, const char*);
  jfieldID      (*GetStaticFieldID)(JNIEnv*, jclass, const char*, const char*);
  jmethodID     (*GetMethodID)(JNIEnv*, jclass, const char*, const char*);
  jmethodID     (*GetStaticMethodID)(JNIEnv*, jclass, const char*, const char*);

  jobject       (*AllocObject)(JNIEnv*, jclass);
  jobject       (*NewObject)(JNIEnv*, jclass, jmethodID, ...);
  jobject       (*NewObjectV)(JNIEnv*, jclass, jmethodID, va_list);
  jobject       (*NewObjectA)(JNIEnv*, jclass, jmethodID, const jvalue*);

  jobject (*CallObjectMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jobject (*CallObjectMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jobject (*CallObjectMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

  jboolean (*CallBooleanMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jboolean (*CallBooleanMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jboolean (*CallBooleanMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

  jbyte (*CallByteMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jbyte (*CallByteMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jbyte (*CallByteMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jchar (*CallCharMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jchar (*CallCharMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jchar (*CallCharMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jshort (*CallShortMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jshort (*CallShortMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jshort (*CallShortMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jint (*CallIntMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jint (*CallIntMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jint (*CallIntMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jlong (*CallLongMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jlong (*CallLongMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jlong (*CallLongMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jfloat (*CallFloatMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jfloat (*CallFloatMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jfloat (*CallFloatMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  jdouble (*CallDoubleMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  jdouble (*CallDoubleMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  jdouble (*CallDoubleMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue *args);

  void (*CallVoidMethod)
    (JNIEnv *env, jobject obj, jmethodID methodID, ...);
  void (*CallVoidMethodV)
    (JNIEnv *env, jobject obj, jmethodID methodID, va_list args);
  void (*CallVoidMethodA)
    (JNIEnv *env, jobject obj, jmethodID methodID, const jvalue * args);

  jobject (*GetObjectField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jboolean (*GetBooleanField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jbyte (*GetByteField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jchar (*GetCharField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jshort (*GetShortField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jint (*GetIntField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jlong (*GetLongField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jfloat (*GetFloatField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);
  jdouble (*GetDoubleField)
    (JNIEnv *env, jobject obj, jfieldID fieldID);

  void (*SetObjectField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jobject val);
  void (*SetBooleanField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jboolean val);
  void (*SetByteField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jbyte val);
  void (*SetCharField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jchar val);
  void (*SetShortField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jshort val);
  void (*SetIntField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jint val);
  void (*SetLongField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jlong val);
  void (*SetFloatField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jfloat val);
  void (*SetDoubleField)
    (JNIEnv *env, jobject obj, jfieldID fieldID, jdouble val);

  jobject (*CallStaticObjectMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jobject (*CallStaticObjectMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jobject (*CallStaticObjectMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jboolean (*CallStaticBooleanMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jboolean (*CallStaticBooleanMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jboolean (*CallStaticBooleanMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jbyte (*CallStaticByteMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jbyte (*CallStaticByteMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jbyte (*CallStaticByteMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jchar (*CallStaticCharMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jchar (*CallStaticCharMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jchar (*CallStaticCharMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jshort (*CallStaticShortMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jshort (*CallStaticShortMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jshort (*CallStaticShortMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jint (*CallStaticIntMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jint (*CallStaticIntMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jint (*CallStaticIntMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jlong (*CallStaticLongMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jlong (*CallStaticLongMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jlong (*CallStaticLongMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jfloat (*CallStaticFloatMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jfloat (*CallStaticFloatMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jfloat (*CallStaticFloatMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  jdouble (*CallStaticDoubleMethod)
    (JNIEnv *env, jclass clazz, jmethodID methodID, ...);
  jdouble (*CallStaticDoubleMethodV)
    (JNIEnv *env, jclass clazz, jmethodID methodID, va_list args);
  jdouble (*CallStaticDoubleMethodA)
    (JNIEnv *env, jclass clazz, jmethodID methodID, const jvalue *args);

  void (*CallStaticVoidMethod)
    (JNIEnv *env, jclass cls, jmethodID methodID, ...);
  void (*CallStaticVoidMethodV)
    (JNIEnv *env, jclass cls, jmethodID methodID, va_list args);
  void (*CallStaticVoidMethodA)
    (JNIEnv *env, jclass cls, jmethodID methodID, const jvalue * args);

  jobject (*GetStaticObjectField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jboolean (*GetStaticBooleanField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jbyte (*GetStaticByteField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jchar (*GetStaticCharField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jshort (*GetStaticShortField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jint (*GetStaticIntField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jlong (*GetStaticLongField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jfloat (*GetStaticFloatField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);
  jdouble (*GetStaticDoubleField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID);

  void (*SetStaticObjectField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jobject value);
  void (*SetStaticBooleanField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jboolean value);
  void (*SetStaticByteField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jbyte value);
  void (*SetStaticCharField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jchar value);
  void (*SetStaticShortField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jshort value);
  void (*SetStaticIntField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jint value);
  void (*SetStaticLongField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jlong value);
  void (*SetStaticFloatField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jfloat value);
  void (*SetStaticDoubleField)
    (JNIEnv *env, jclass clazz, jfieldID fieldID, jdouble value);

  jint          (*GetJavaVM)(JNIEnv*, JavaVM**);
};

/*
 * JNIEnv C++ object wrapper.
 *
 * This is usually overlaid on a C struct whose first element is a
 * JNINativeInterface*.  We rely somewhat on compiler behavior.
 */
struct _JNIEnv {
    /* do not rename this; it does not seem to be entirely opaque */
    const struct JNINativeInterface* functions;

#if defined(__cplusplus)

    jint GetVersion()
    { return functions->GetVersion(this); }

    jclass FindClass(const char* name)
    { return functions->FindClass(this, name); }

    jclass GetSuperclass(jclass clazz)
    { return functions->GetSuperclass(this, clazz); }

    jboolean IsAssignableFrom(jclass clazz1, jclass clazz2)
    { return functions->IsAssignableFrom(this, clazz1, clazz2); }

    jint Throw(jthrowable obj)
    { return functions->Throw(this, obj); }

    jint ThrowNew(jclass clazz, const char* message)
    { return functions->ThrowNew(this, clazz, message); }

    void ExceptionClear()
    { functions->ExceptionClear(this); }

    jobject NewGlobalRef(jobject obj)
    { return functions->NewGlobalRef(this, obj); }

    jobject NewLocalRef(jobject obj)
    { return functions->NewLocalRef(this, obj); }

    void DeleteGlobalRef(jobject globalRef)
    { functions->DeleteGlobalRef(this, globalRef); }

    void DeleteLocalRef(jobject localRef)
    { functions->DeleteLocalRef(this, localRef); }

    jboolean IsSameObject(jobject ref1, jobject ref2)
    { return functions->IsSameObject(this, ref1, ref2); }

    jclass GetObjectClass(jobject obj)
    { return functions->GetObjectClass(this, obj); }

    jboolean IsInstanceOf(jobject obj, jclass clazz)
    { return functions->IsInstanceOf(this, obj, clazz); }

    jstring NewString(const jchar* unicodeChars, jsize len)
    { return functions->NewString(this, unicodeChars, len); }

    jsize GetStringLength(jstring string)
    { return functions->GetStringLength(this, string); }

    const jchar* GetStringChars(jstring string, jboolean* isCopy)
    { return functions->GetStringChars(this, string, isCopy); }

    void ReleaseStringChars(jstring string, const jchar* chars)
    { functions->ReleaseStringChars(this, string, chars); }

    jstring NewStringUTF(const char* bytes)
    { return functions->NewStringUTF(this, bytes); }

    jsize GetStringUTFLength(jstring string)
    { return functions->GetStringUTFLength(this, string); }

    const char* GetStringUTFChars(jstring string, jboolean* isCopy)
    { return functions->GetStringUTFChars(this, string, isCopy); }

    void ReleaseStringUTFChars(jstring string, const char* utf)
    { functions->ReleaseStringUTFChars(this, string, utf); }

    jsize GetArrayLength(jarray array)
    { return functions->GetArrayLength(this, array); }

    jobjectArray NewObjectArray(jsize length, jclass elementClass,
        jobject initialElement)
    { return functions->NewObjectArray(this, length, elementClass,
        initialElement); }

    jobject GetObjectArrayElement(jobjectArray array, jsize index)
    { return functions->GetObjectArrayElement(this, array, index); }

    void SetObjectArrayElement(jobjectArray array, jsize index, jobject value)
    { functions->SetObjectArrayElement(this, array, index, value); }

    jbooleanArray NewBooleanArray(jsize length)
    { return functions->NewBooleanArray(this, length); }
    jbyteArray NewByteArray(jsize length)
    { return functions->NewByteArray(this, length); }
    jcharArray NewCharArray(jsize length)
    { return functions->NewCharArray(this, length); }
    jshortArray NewShortArray(jsize length)
    { return functions->NewShortArray(this, length); }
    jintArray NewIntArray(jsize length)
    { return functions->NewIntArray(this, length); }
    jlongArray NewLongArray(jsize length)
    { return functions->NewLongArray(this, length); }
    jfloatArray NewFloatArray(jsize length)
    { return functions->NewFloatArray(this, length); }
    jdoubleArray NewDoubleArray(jsize length)
    { return functions->NewDoubleArray(this, length); }

    jboolean* GetBooleanArrayElements(jbooleanArray array, jboolean* isCopy)
    { return functions->GetBooleanArrayElements(this, array, isCopy); }
    jbyte* GetByteArrayElements(jbyteArray array, jboolean* isCopy)
    { return functions->GetByteArrayElements(this, array, isCopy); }
    jchar* GetCharArrayElements(jcharArray array, jboolean* isCopy)
    { return functions->GetCharArrayElements(this, array, isCopy); }
    jshort* GetShortArrayElements(jshortArray array, jboolean* isCopy)
    { return functions->GetShortArrayElements(this, array, isCopy); }
    jint* GetIntArrayElements(jintArray array, jboolean* isCopy)
    { return functions->GetIntArrayElements(this, array, isCopy); }
    jlong* GetLongArrayElements(jlongArray array, jboolean* isCopy)
    { return functions->GetLongArrayElements(this, array, isCopy); }
    jfloat* GetFloatArrayElements(jfloatArray array, jboolean* isCopy)
    { return functions->GetFloatArrayElements(this, array, isCopy); }
    jdouble* GetDoubleArrayElements(jdoubleArray array, jboolean* isCopy)
    { return functions->GetDoubleArrayElements(this, array, isCopy); }

    void ReleaseBooleanArrayElements(jbooleanArray array, jboolean* elems,
        jint mode)
    { functions->ReleaseBooleanArrayElements(this, array, elems, mode); }
    void ReleaseByteArrayElements(jbyteArray array, jbyte* elems,
        jint mode)
    { functions->ReleaseByteArrayElements(this, array, elems, mode); }
    void ReleaseCharArrayElements(jcharArray array, jchar* elems,
        jint mode)
    { functions->ReleaseCharArrayElements(this, array, elems, mode); }
    void ReleaseShortArrayElements(jshortArray array, jshort* elems,
        jint mode)
    { functions->ReleaseShortArrayElements(this, array, elems, mode); }
    void ReleaseIntArrayElements(jintArray array, jint* elems,
        jint mode)
    { functions->ReleaseIntArrayElements(this, array, elems, mode); }
    void ReleaseLongArrayElements(jlongArray array, jlong* elems,
        jint mode)
    { functions->ReleaseLongArrayElements(this, array, elems, mode); }
    void ReleaseFloatArrayElements(jfloatArray array, jfloat* elems,
        jint mode)
    { functions->ReleaseFloatArrayElements(this, array, elems, mode); }
    void ReleaseDoubleArrayElements(jdoubleArray array, jdouble* elems,
        jint mode)
    { functions->ReleaseDoubleArrayElements(this, array, elems, mode); }

    void GetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len,
        jboolean* buf)
    { functions->GetBooleanArrayRegion(this, array, start, len, buf); }
    void GetByteArrayRegion(jbyteArray array, jsize start, jsize len,
        jbyte* buf)
    { functions->GetByteArrayRegion(this, array, start, len, buf); }
    void GetCharArrayRegion(jcharArray array, jsize start, jsize len,
        jchar* buf)
    { functions->GetCharArrayRegion(this, array, start, len, buf); }
    void GetShortArrayRegion(jshortArray array, jsize start, jsize len,
        jshort* buf)
    { functions->GetShortArrayRegion(this, array, start, len, buf); }
    void GetIntArrayRegion(jintArray array, jsize start, jsize len,
        jint* buf)
    { functions->GetIntArrayRegion(this, array, start, len, buf); }
    void GetLongArrayRegion(jlongArray array, jsize start, jsize len,
        jlong* buf)
    { functions->GetLongArrayRegion(this, array, start, len, buf); }
    void GetFloatArrayRegion(jfloatArray array, jsize start, jsize len,
        jfloat* buf)
    { functions->GetFloatArrayRegion(this, array, start, len, buf); }
    void GetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len,
        jdouble* buf)
    { functions->GetDoubleArrayRegion(this, array, start, len, buf); }

    void SetBooleanArrayRegion(jbooleanArray array, jsize start, jsize len,
        const jboolean* buf)
    { functions->SetBooleanArrayRegion(this, array, start, len, buf); }
    void SetByteArrayRegion(jbyteArray array, jsize start, jsize len,
        const jbyte* buf)
    { functions->SetByteArrayRegion(this, array, start, len, buf); }
    void SetCharArrayRegion(jcharArray array, jsize start, jsize len,
        const jchar* buf)
    { functions->SetCharArrayRegion(this, array, start, len, buf); }
    void SetShortArrayRegion(jshortArray array, jsize start, jsize len,
        const jshort* buf)
    { functions->SetShortArrayRegion(this, array, start, len, buf); }
    void SetIntArrayRegion(jintArray array, jsize start, jsize len,
        const jint* buf)
    { functions->SetIntArrayRegion(this, array, start, len, buf); }
    void SetLongArrayRegion(jlongArray array, jsize start, jsize len,
        const jlong* buf)
    { functions->SetLongArrayRegion(this, array, start, len, buf); }
    void SetFloatArrayRegion(jfloatArray array, jsize start, jsize len,
        const jfloat* buf)
    { functions->SetFloatArrayRegion(this, array, start, len, buf); }
    void SetDoubleArrayRegion(jdoubleArray array, jsize start, jsize len,
        const jdouble* buf)
    { functions->SetDoubleArrayRegion(this, array, start, len, buf); }

    void GetStringRegion(jstring str, jsize start, jsize len, jchar* buf)
    { functions->GetStringRegion(this, str, start, len, buf); }

    void GetStringUTFRegion(jstring str, jsize start, jsize len, char* buf)
    { return functions->GetStringUTFRegion(this, str, start, len, buf); }

    void* GetPrimitiveArrayCritical(jarray array, jboolean* isCopy)
    { return functions->GetPrimitiveArrayCritical(this, array, isCopy); }

    void ReleasePrimitiveArrayCritical(jarray array, void* carray, jint mode)
    { functions->ReleasePrimitiveArrayCritical(this, array, carray, mode); }

    const jchar* GetStringCritical(jstring string, jboolean* isCopy)
    { return functions->GetStringCritical(this, string, isCopy); }

    void ReleaseStringCritical(jstring string, const jchar* carray)
    { functions->ReleaseStringCritical(this, string, carray); }

    jobject NewDirectByteBuffer(void* address, jlong capacity)
    { return functions->NewDirectByteBuffer(this, address, capacity); }

    void* GetDirectBufferAddress(jobject buf)
    { return functions->GetDirectBufferAddress(this, buf); }

    jlong GetDirectBufferCapacity(jobject buf)
    { return functions->GetDirectBufferCapacity(this, buf); }

    jfieldID GetFieldID(jclass clazz, const char *name, const char *sig)
    { return functions->GetFieldID(this, clazz, name, sig); }

    jfieldID GetStaticFieldID(jclass clazz, const char *name, const char *sig)
    { return functions->GetStaticFieldID(this, clazz, name, sig); }

    jmethodID GetMethodID(jclass clazz, const char *name, const char *sig)
    { return functions->GetMethodID(this, clazz, name, sig); }

    jmethodID GetStaticMethodID(jclass clazz, const char *name, const char *sig)
    { return functions->GetStaticMethodID(this, clazz, name, sig); }

    jobject AllocObject(jclass clazz) {
        return functions->AllocObject(this, clazz);
    }

    jobject NewObject(jclass clazz, jmethodID methodID, ...) {
      va_list args;
      jobject result;
      va_start(args, methodID);
      result = functions->NewObjectV(this, clazz, methodID, args);
      va_end(args);
      return result;
    }

    jobject NewObjectV(jclass clazz, jmethodID methodID, va_list args)
    { return functions->NewObjectV(this, clazz, methodID, args); }

    jobject NewObjectA(jclass clazz, jmethodID methodID, const jvalue *args)
    { return functions->NewObjectA(this, clazz, methodID, args); }


    jobject CallObjectMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jobject result;
        va_start(args,methodID);
        result = functions->CallObjectMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jobject CallObjectMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallObjectMethodV(this,obj,methodID,args);
    }
    jobject CallObjectMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallObjectMethodA(this,obj,methodID,args);
    }

    jboolean CallBooleanMethod(jobject obj,
                               jmethodID methodID, ...) {
        va_list args;
        jboolean result;
        va_start(args,methodID);
        result = functions->CallBooleanMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jboolean CallBooleanMethodV(jobject obj, jmethodID methodID,
                                va_list args) {
        return functions->CallBooleanMethodV(this,obj,methodID,args);
    }
    jboolean CallBooleanMethodA(jobject obj, jmethodID methodID,
                                const jvalue * args) {
        return functions->CallBooleanMethodA(this,obj,methodID, args);
    }

    jbyte CallByteMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jbyte result;
        va_start(args,methodID);
        result = functions->CallByteMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jbyte CallByteMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallByteMethodV(this,obj,methodID,args);
    }
    jbyte CallByteMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallByteMethodA(this,obj,methodID,args);
    }

    jchar CallCharMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jchar result;
        va_start(args,methodID);
        result = functions->CallCharMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jchar CallCharMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallCharMethodV(this,obj,methodID,args);
    }
    jchar CallCharMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallCharMethodA(this,obj,methodID,args);
    }

    jshort CallShortMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jshort result;
        va_start(args,methodID);
        result = functions->CallShortMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jshort CallShortMethodV(jobject obj, jmethodID methodID,
                            va_list args) {
        return functions->CallShortMethodV(this,obj,methodID,args);
    }
    jshort CallShortMethodA(jobject obj, jmethodID methodID,
                            const jvalue * args) {
        return functions->CallShortMethodA(this,obj,methodID,args);
    }

    jint CallIntMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jint result;
        va_start(args,methodID);
        result = functions->CallIntMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jint CallIntMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallIntMethodV(this,obj,methodID,args);
    }
    jint CallIntMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallIntMethodA(this,obj,methodID,args);
    }

    jlong CallLongMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jlong result;
        va_start(args,methodID);
        result = functions->CallLongMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jlong CallLongMethodV(jobject obj, jmethodID methodID,
                          va_list args) {
        return functions->CallLongMethodV(this,obj,methodID,args);
    }
    jlong CallLongMethodA(jobject obj, jmethodID methodID,
                          const jvalue * args) {
        return functions->CallLongMethodA(this,obj,methodID,args);
    }

    jfloat CallFloatMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jfloat result;
        va_start(args,methodID);
        result = functions->CallFloatMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jfloat CallFloatMethodV(jobject obj, jmethodID methodID,
                            va_list args) {
        return functions->CallFloatMethodV(this,obj,methodID,args);
    }
    jfloat CallFloatMethodA(jobject obj, jmethodID methodID,
                            const jvalue * args) {
        return functions->CallFloatMethodA(this,obj,methodID,args);
    }

    jdouble CallDoubleMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        jdouble result;
        va_start(args,methodID);
        result = functions->CallDoubleMethodV(this,obj,methodID,args);
        va_end(args);
        return result;
    }
    jdouble CallDoubleMethodV(jobject obj, jmethodID methodID,
                        va_list args) {
        return functions->CallDoubleMethodV(this,obj,methodID,args);
    }
    jdouble CallDoubleMethodA(jobject obj, jmethodID methodID,
                        const jvalue * args) {
        return functions->CallDoubleMethodA(this,obj,methodID,args);
    }

    void CallVoidMethod(jobject obj, jmethodID methodID, ...) {
        va_list args;
        va_start(args,methodID);
        functions->CallVoidMethodV(this,obj,methodID,args);
        va_end(args);
    }
    void CallVoidMethodV(jobject obj, jmethodID methodID,
                         va_list args) {
        functions->CallVoidMethodV(this,obj,methodID,args);
    }
    void CallVoidMethodA(jobject obj, jmethodID methodID,
                         const jvalue * args) {
        functions->CallVoidMethodA(this,obj,methodID,args);
    }

    jobject GetObjectField(jobject obj, jfieldID fieldID) {
        return functions->GetObjectField(this,obj,fieldID);
    }
    jboolean GetBooleanField(jobject obj, jfieldID fieldID) {
        return functions->GetBooleanField(this,obj,fieldID);
    }
    jbyte GetByteField(jobject obj, jfieldID fieldID) {
        return functions->GetByteField(this,obj,fieldID);
    }
    jchar GetCharField(jobject obj, jfieldID fieldID) {
        return functions->GetCharField(this,obj,fieldID);
    }
    jshort GetShortField(jobject obj, jfieldID fieldID) {
        return functions->GetShortField(this,obj,fieldID);
    }
    jint GetIntField(jobject obj, jfieldID fieldID) {
        return functions->GetIntField(this,obj,fieldID);
    }
    jlong GetLongField(jobject obj, jfieldID fieldID) {
        return functions->GetLongField(this,obj,fieldID);
    }
    jfloat GetFloatField(jobject obj, jfieldID fieldID) {
        return functions->GetFloatField(this,obj,fieldID);
    }
    jdouble GetDoubleField(jobject obj, jfieldID fieldID) {
        return functions->GetDoubleField(this,obj,fieldID);
    }

    void SetObjectField(jobject obj, jfieldID fieldID, jobject val) {
        functions->SetObjectField(this,obj,fieldID,val);
    }
    void SetBooleanField(jobject obj, jfieldID fieldID,
                         jboolean val) {
        functions->SetBooleanField(this,obj,fieldID,val);
    }
    void SetByteField(jobject obj, jfieldID fieldID,
                      jbyte val) {
        functions->SetByteField(this,obj,fieldID,val);
    }
    void SetCharField(jobject obj, jfieldID fieldID,
                      jchar val) {
        functions->SetCharField(this,obj,fieldID,val);
    }
    void SetShortField(jobject obj, jfieldID fieldID,
                       jshort val) {
        functions->SetShortField(this,obj,fieldID,val);
    }
    void SetIntField(jobject obj, jfieldID fieldID,
                     jint val) {
        functions->SetIntField(this,obj,fieldID,val);
    }
    void SetLongField(jobject obj, jfieldID fieldID,
                      jlong val) {
        functions->SetLongField(this,obj,fieldID,val);
    }
    void SetFloatField(jobject obj, jfieldID fieldID,
                       jfloat val) {
        functions->SetFloatField(this,obj,fieldID,val);
    }
    void SetDoubleField(jobject obj, jfieldID fieldID,
                        jdouble val) {
        functions->SetDoubleField(this,obj,fieldID,val);
    }

    jobject CallStaticObjectMethod(jclass clazz, jmethodID methodID,
                             ...) {
        va_list args;
        jobject result;
        va_start(args,methodID);
        result = functions->CallStaticObjectMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jobject CallStaticObjectMethodV(jclass clazz, jmethodID methodID,
                              va_list args) {
        return functions->CallStaticObjectMethodV(this,clazz,methodID,args);
    }
    jobject CallStaticObjectMethodA(jclass clazz, jmethodID methodID,
                              const jvalue *args) {
        return functions->CallStaticObjectMethodA(this,clazz,methodID,args);
    }

    jboolean CallStaticBooleanMethod(jclass clazz,
                                     jmethodID methodID, ...) {
        va_list args;
        jboolean result;
        va_start(args,methodID);
        result = functions->CallStaticBooleanMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jboolean CallStaticBooleanMethodV(jclass clazz,
                                      jmethodID methodID, va_list args) {
        return functions->CallStaticBooleanMethodV(this,clazz,methodID,args);
    }
    jboolean CallStaticBooleanMethodA(jclass clazz,
                                      jmethodID methodID, const jvalue *args) {
        return functions->CallStaticBooleanMethodA(this,clazz,methodID,args);
    }

    jbyte CallStaticByteMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jbyte result;
        va_start(args,methodID);
        result = functions->CallStaticByteMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jbyte CallStaticByteMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticByteMethodV(this,clazz,methodID,args);
    }
    jbyte CallStaticByteMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticByteMethodA(this,clazz,methodID,args);
    }

    jchar CallStaticCharMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jchar result;
        va_start(args,methodID);
        result = functions->CallStaticCharMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jchar CallStaticCharMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticCharMethodV(this,clazz,methodID,args);
    }
    jchar CallStaticCharMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticCharMethodA(this,clazz,methodID,args);
    }

    jshort CallStaticShortMethod(jclass clazz,
                                 jmethodID methodID, ...) {
        va_list args;
        jshort result;
        va_start(args,methodID);
        result = functions->CallStaticShortMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jshort CallStaticShortMethodV(jclass clazz,
                                  jmethodID methodID, va_list args) {
        return functions->CallStaticShortMethodV(this,clazz,methodID,args);
    }
    jshort CallStaticShortMethodA(jclass clazz,
                                  jmethodID methodID, const jvalue *args) {
        return functions->CallStaticShortMethodA(this,clazz,methodID,args);
    }

    jint CallStaticIntMethod(jclass clazz,
                             jmethodID methodID, ...) {
        va_list args;
        jint result;
        va_start(args,methodID);
        result = functions->CallStaticIntMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jint CallStaticIntMethodV(jclass clazz,
                              jmethodID methodID, va_list args) {
        return functions->CallStaticIntMethodV(this,clazz,methodID,args);
    }
    jint CallStaticIntMethodA(jclass clazz,
                              jmethodID methodID, const jvalue *args) {
        return functions->CallStaticIntMethodA(this,clazz,methodID,args);
    }

    jlong CallStaticLongMethod(jclass clazz,
                               jmethodID methodID, ...) {
        va_list args;
        jlong result;
        va_start(args,methodID);
        result = functions->CallStaticLongMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jlong CallStaticLongMethodV(jclass clazz,
                                jmethodID methodID, va_list args) {
        return functions->CallStaticLongMethodV(this,clazz,methodID,args);
    }
    jlong CallStaticLongMethodA(jclass clazz,
                                jmethodID methodID, const jvalue *args) {
        return functions->CallStaticLongMethodA(this,clazz,methodID,args);
    }

    jfloat CallStaticFloatMethod(jclass clazz,
                                 jmethodID methodID, ...) {
        va_list args;
        jfloat result;
        va_start(args,methodID);
        result = functions->CallStaticFloatMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jfloat CallStaticFloatMethodV(jclass clazz,
                                  jmethodID methodID, va_list args) {
        return functions->CallStaticFloatMethodV(this,clazz,methodID,args);
    }
    jfloat CallStaticFloatMethodA(jclass clazz,
                                  jmethodID methodID, const jvalue *args) {
        return functions->CallStaticFloatMethodA(this,clazz,methodID,args);
    }

    jdouble CallStaticDoubleMethod(jclass clazz,
                                   jmethodID methodID, ...) {
        va_list args;
        jdouble result;
        va_start(args,methodID);
        result = functions->CallStaticDoubleMethodV(this,clazz,methodID,args);
        va_end(args);
        return result;
    }
    jdouble CallStaticDoubleMethodV(jclass clazz,
                                    jmethodID methodID, va_list args) {
        return functions->CallStaticDoubleMethodV(this,clazz,methodID,args);
    }
    jdouble CallStaticDoubleMethodA(jclass clazz,
                                    jmethodID methodID, const jvalue *args) {
        return functions->CallStaticDoubleMethodA(this,clazz,methodID,args);
    }

    void CallStaticVoidMethod(jclass cls, jmethodID methodID, ...) {
        va_list args;
        va_start(args,methodID);
        functions->CallStaticVoidMethodV(this,cls,methodID,args);
        va_end(args);
    }
    void CallStaticVoidMethodV(jclass cls, jmethodID methodID,
                               va_list args) {
        functions->CallStaticVoidMethodV(this,cls,methodID,args);
    }
    void CallStaticVoidMethodA(jclass cls, jmethodID methodID,
                               const jvalue * args) {
        functions->CallStaticVoidMethodA(this,cls,methodID,args);
    }

    jobject GetStaticObjectField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticObjectField(this,clazz,fieldID);
    }
    jboolean GetStaticBooleanField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticBooleanField(this,clazz,fieldID);
    }
    jbyte GetStaticByteField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticByteField(this,clazz,fieldID);
    }
    jchar GetStaticCharField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticCharField(this,clazz,fieldID);
    }
    jshort GetStaticShortField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticShortField(this,clazz,fieldID);
    }
    jint GetStaticIntField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticIntField(this,clazz,fieldID);
    }
    jlong GetStaticLongField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticLongField(this,clazz,fieldID);
    }
    jfloat GetStaticFloatField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticFloatField(this,clazz,fieldID);
    }
    jdouble GetStaticDoubleField(jclass clazz, jfieldID fieldID) {
        return functions->GetStaticDoubleField(this,clazz,fieldID);
    }

    void SetStaticObjectField(jclass clazz, jfieldID fieldID,
                        jobject value) {
      functions->SetStaticObjectField(this,clazz,fieldID,value);
    }
    void SetStaticBooleanField(jclass clazz, jfieldID fieldID,
                        jboolean value) {
      functions->SetStaticBooleanField(this,clazz,fieldID,value);
    }
    void SetStaticByteField(jclass clazz, jfieldID fieldID,
                        jbyte value) {
      functions->SetStaticByteField(this,clazz,fieldID,value);
    }
    void SetStaticCharField(jclass clazz, jfieldID fieldID,
                        jchar value) {
      functions->SetStaticCharField(this,clazz,fieldID,value);
    }
    void SetStaticShortField(jclass clazz, jfieldID fieldID,
                        jshort value) {
      functions->SetStaticShortField(this,clazz,fieldID,value);
    }
    void SetStaticIntField(jclass clazz, jfieldID fieldID,
                        jint value) {
      functions->SetStaticIntField(this,clazz,fieldID,value);
    }
    void SetStaticLongField(jclass clazz, jfieldID fieldID,
                        jlong value) {
      functions->SetStaticLongField(this,clazz,fieldID,value);
    }
    void SetStaticFloatField(jclass clazz, jfieldID fieldID,
                        jfloat value) {
      functions->SetStaticFloatField(this,clazz,fieldID,value);
    }
    void SetStaticDoubleField(jclass clazz, jfieldID fieldID,
                        jdouble value) {
      functions->SetStaticDoubleField(this,clazz,fieldID,value);
    }

    jint GetJavaVM(JavaVM **vm)
    { return functions->GetJavaVM(this, vm); }
#endif /*__cplusplus*/
};

/*
 * Table of interface function pointers for JavaVM.
 */
struct JNIInvokeInterface {
  jint (*DestroyJavaVM)(JavaVM *vm);
  jint (*AttachCurrentThread)(JavaVM *vm, void **penv, void *args);
  jint (*DetachCurrentThread)(JavaVM *vm);
  jint (*GetEnv)(JavaVM *vm, void **penv, jint version);
  jint (*AttachCurrentThreadAsDaemon)(JavaVM *vm, void **penv, void *args);
};

/*
 * JavaVM C++ object wrapper.
 *
 * This is usually overlaid on a C struct whose first element is a
 * JNIInvokeInterface*.  We rely somewhat on compiler behavior.
 */
struct _JavaVM {
    /* do not rename this; it does not seem to be entirely opaque */
    const struct JNIInvokeInterface* functions;

#if defined(__cplusplus)

    jint DestroyJavaVM()
    { return functions->DestroyJavaVM(this); }

    jint AttachCurrentThread(void **penv, void *args)
    { return functions->AttachCurrentThread(this, penv, args); }

    jint DetachCurrentThread()
    { return functions->DetachCurrentThread(this); }

    jint GetEnv(void **penv, jint version)
    { return functions->GetEnv(this, penv, version); }

    jint AttachCurrentThreadAsDaemon(void **penv, void *args)
    { return functions->AttachCurrentThreadAsDaemon(this, penv, args); }
#endif /*__cplusplus*/
};

#ifdef __cplusplus
extern "C" {
#endif

#define JNIIMPORT
#define JNIEXPORT  __attribute__ ((visibility ("default")))
#define JNICALL

#ifdef __cplusplus
}
#endif


/*
 * Manifest constants.
 */
#define JNI_FALSE   false
#define JNI_TRUE    true

#define JNI_VERSION_1_1 0x00010001
#define JNI_VERSION_1_2 0x00010002
#define JNI_VERSION_1_4 0x00010004
#define JNI_VERSION_1_6 0x00010006

#define JNI_OK          (0)         /* no error */
#define JNI_ERR         (-1)        /* generic error */
#define JNI_EDETACHED   (-2)        /* thread detached from the VM */
#define JNI_EVERSION    (-3)        /* JNI version error */

#define JNI_COMMIT      1           /* copy content, do not free buffer */
#define JNI_ABORT       2           /* free buffer w/o copying back */

#endif  /* JNI_H_ */
