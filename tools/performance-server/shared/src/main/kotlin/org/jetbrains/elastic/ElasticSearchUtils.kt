/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.elastic

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.js.json               // TODO - migrate to multiplatform.

// Now implemenation for network connection only for Node.js. TODO - multiplatform.
external fun require(module: String): dynamic

fun getAuth(user: String, password: String): String {
    val buffer = js("Buffer").from(user + ":" + password)
    val based64String = buffer.toString("base64")
    return "Basic " + based64String
}

enum class RequestMethod {
    POST, GET, PUT
}

internal fun <T: String?> sendBaseRequest(method: RequestMethod, url: String, user: String? = null, password: String? = null,
                             acceptJsonContentType: Boolean = true, body: String? = null,
                             errorHandler:(url: String, response: dynamic) -> Nothing?): Promise<T> {
    val request = require("node-fetch")
    val headers = mutableListOf<Pair<String, String>>()
    if (user != null && password != null) {
        headers.add("Authorization" to getAuth(user, password))
    }
    if (acceptJsonContentType) {
        headers.add("Accept" to "*/*")
        headers.add("Content-Type" to "application/json")
    }
    return request(url,
            json(
                    "method" to method.toString(),
                    "headers" to json(*(headers.toTypedArray())),
                    "body" to body
            )
    ).then { response ->
        if (!response.ok) {
            errorHandler(url, response)
        } else {
            response.text()
        }
    }
}

fun sendRequest(method: RequestMethod, url: String, user: String? = null, password: String? = null,
                acceptJsonContentType: Boolean = true, body: String? = null): Promise<String> =
    sendBaseRequest<String>(method, url, user, password, acceptJsonContentType, body) { url, response ->
        error ("Error during getting response from $url\n${response.text()}")
    }


fun sendOptionalRequest(method: RequestMethod, url: String, user: String? = null, password: String? = null,
                acceptJsonContentType: Boolean = true, body: String? = null): Promise<String?> =
    sendBaseRequest<String?>(method, url, user, password, acceptJsonContentType, body) { url, response ->
        println ("Error during getting response from $url\n${response.text()}")
        null
    }