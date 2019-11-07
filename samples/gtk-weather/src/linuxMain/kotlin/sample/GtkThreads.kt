package sample

import kotlinx.cinterop.*
import libgtk3.gdk_threads_add_idle
import platform.posix.pthread_create
import platform.posix.pthread_t
import platform.posix.pthread_tVar
import kotlin.native.concurrent.freeze


fun createThread(f0 : () -> Unit) : pthread_t {
    val thread_id = memScoped { alloc<pthread_tVar>() }
    pthread_create(thread_id.ptr, null, staticCFunction { pointer: COpaquePointer? ->
        initRuntimeIfNeeded()
        pointer?.asStableRef<() -> Unit>()?.get()?.invoke()

        null as COpaquePointer?
    }.reinterpret(), StableRef.create(f0.freeze()).asCPointer())
    return thread_id.value
}

fun pthread_t.await() : COpaquePointer? {
    return kotlinx.cinterop.memScoped { alloc<COpaquePointerVar>() }.apply {
        platform.posix.pthread_join(this@await, this.ptr)
    }.value
}


fun gtk_ideal(f0 : () -> Unit) {
    gdk_threads_add_idle(staticCFunction { pointer: COpaquePointer? ->
        pointer?.asStableRef<() -> Unit>()?.get()?.invoke()

        null as COpaquePointer?
    }.reinterpret(), StableRef.create(f0.freeze()).asCPointer())
}