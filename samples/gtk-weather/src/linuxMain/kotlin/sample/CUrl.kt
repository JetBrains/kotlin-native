package sample

import kotlinx.cinterop.*
import libgtk3.*
import platform.posix.*


fun curl_read_all_content(url: String) : String? {
    initRuntimeIfNeeded()

    val memory = StringBuilder()
    curl_global_init(CURL_GLOBAL_ALL)
    val curl = curl_easy_init()
    curl_easy_setopt(curl, CURLOPT_URL, url)

    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, staticCFunction {
            contents: CPointer<ByteVar>?, size: size_t, nmemb: size_t, userp : COpaquePointer? ->
        initRuntimeIfNeeded()
        val mem = userp?.asStableRef<StringBuilder>()?.get()
        mem?.append(contents?.toKString())
        return@staticCFunction size * nmemb
    })

    curl_easy_setopt(curl, CURLOPT_WRITEDATA, StableRef.create(memory).asCPointer())
    curl_easy_setopt(curl, CURLOPT_USERAGENT, "libcurl-agent/1.0")

    val error = curl_easy_perform(curl)
    curl_easy_cleanup(curl)
    curl_global_cleanup()

    return if (error==0u) memory.toString() else null
}


fun CPointer<GtkWidget>?.connect(name: String,function: () -> Unit) {
    g_signal_connect_data(this, name, staticCFunction { pointer: COpaquePointer?, data: gpointer? ->
        data?.asStableRef<() -> Unit>()?.get()?.let { it() }
        null as COpaquePointer?
    }.reinterpret(), StableRef.create(function).asCPointer(), null, 0U)
}