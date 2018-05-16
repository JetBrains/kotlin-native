/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package NativeApplication

import kotlinx.cinterop.*
import platform.android.*

data class JniClass(val jclass: jclass?)
data class JniObject(val jobject: jobject?) {
    val isNull
        get() = jobject == null
    val isNotNull
        get() = jobject != null
}
data class JniMethod(val jmethod: jmethodID?)

class JniBridge(val vm: CPointer<JavaVMVar>) {
    private val vmFunctions: JNIInvokeInterface = vm.pointed.pointed!!
    private val jniEnv by lazy {
        memScoped {
            val envStorage = alloc<CPointerVar<JNIEnvVar>>()
            if (vmFunctions.AttachCurrentThreadAsDaemon!!(vm, envStorage.ptr, null) != 0)
                throw Error("Cannot attach thread to the VM")
            envStorage.pointed!!
        }
    }

    private val envFunctions: JNINativeInterface by lazy {
        jniEnv.pointed!!
    }

    private val fNewStringUTF = envFunctions.NewStringUTF!!
    private val fFindClass = envFunctions.FindClass!!
    private val fGetMethodID = envFunctions.GetMethodID!!
    private val fCallVoidMethodA = envFunctions.CallVoidMethodA!!
    private val fCallObjectMethodA = envFunctions.CallObjectMethodA!!
    private val fExceptionCheck = envFunctions.ExceptionCheck!!
    private val fExceptionDescribe = envFunctions.ExceptionDescribe!!
    private val fExceptionClear = envFunctions.ExceptionClear!!

    private fun check() {
        if (fExceptionCheck(jniEnv.ptr) != 0.toByte()) {
            fExceptionDescribe(jniEnv.ptr)
            fExceptionClear(jniEnv.ptr)
            throw Error("JVM exception thrown")
        }
    }

    fun toJString(string: String) = memScoped {
        val result = JniObject(fNewStringUTF(jniEnv.ptr, string.cstr.ptr))
        check()
        result
    }

    fun toJValues(arguments: Array<out Any?>, scope: MemScope): CPointer<jvalue>? {
        val result = scope.allocArray<jvalue>(arguments.size)
        arguments.mapIndexed { index, it ->
            when (it) {
                null -> result[index].l = null
                is JniObject -> result[index].l = it.jobject
                is String -> result[index].l = toJString(it).jobject
                is Int -> result[index].i = it
                is Long -> result[index].j = it
                else -> throw Error("Unsupported conversion for ${it::class.simpleName}")
            }
        }
        return result
    }

    fun FindClass(name: String) = memScoped {
        val result = JniClass(fFindClass(jniEnv.ptr, name.cstr.ptr))
        check()
        result
    }

    fun GetMethodID(clazz: JniClass, name: String, signature: String) = memScoped {
        val result = JniMethod(fGetMethodID(jniEnv.ptr, clazz.jclass, name.cstr.ptr, signature.cstr.ptr))
        check()
         result
    }

    fun CallVoidMethod(receiver: JniObject?, method: JniMethod, vararg arguments: Any?) = memScoped {
        fCallVoidMethodA(jniEnv.ptr, receiver?.jobject, method.jmethod,
                toJValues(arguments, this@memScoped))
        check()
    }

    fun CallObjectMethod(receiver: JniObject?, method: JniMethod, vararg arguments: Any?) = memScoped {
        val result = JniObject(fCallObjectMethodA(jniEnv.ptr, receiver?.jobject, method.jmethod,
                toJValues(arguments, this@memScoped)))
        check()
        result
    }
}