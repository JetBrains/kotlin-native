/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.network

import kotlin.js.Promise            // TODO - migrate to multiplatform.
import kotlin.js.json               // TODO - migrate to multiplatform.
import kotlin.js.Date

// Placeholder as analog for indexable type in TS.
external interface `T$0` {
    @nativeGetter
    operator fun get(key: String): String?
    @nativeSetter
    operator fun set(key: String, value: String)
}

@JsModule("aws-sdk")
@JsNonModule
external object AWSInstance {

    // Replace dynamic with some real type
    class Endpoint(domain: String)
    open class HttpRequest(endpoint: Endpoint, region: String) {
        open fun pathname(): String
        open var search: String
        open var body: String?
        open var endpoint: Endpoint
        open var headers: `T$0`
        open var method: String
        open var path: String
    }
    class HttpClient() {
        val handleRequest: dynamic
    }
    class SharedIniFileCredentials(options: Map<String, String>) {
        val accessKeyId: String
    }
    class Signers() {
        class V4(request: HttpRequest, subsystem: String) {
            fun addAuthorization(credentials: SharedIniFileCredentials, date: Date)
        }
    }
}

class AWSNetworkConnector : NetworkConnector() {
    // TODO: read from config file.
    val AWSDomain = "vpc-kotlin-perf-service-5e6ldakkdv526ii5hbclzcmpny.eu-west-1.es.amazonaws.com"
    val AWSRegion = "eu-west-1"

    override fun <T: String?> sendBaseRequest(method: RequestMethod, path: String, user: String?, password: String?,
                                              acceptJsonContentType: Boolean, body: String?,
                                              errorHandler:(url: String, response: dynamic) -> Nothing?): Promise<T> {
        val AWSEndpoint = AWSInstance.Endpoint(AWSDomain)
        var request = AWSInstance.HttpRequest(AWSEndpoint, AWSRegion)
        request.method = method.toString()
        request.path += path
        request.body = body

        request.headers["host"] = this.AWSDomain

        if (acceptJsonContentType) {
            //headers.add("Accept" to "*/*")
            request.headers["Content-Type"] = "application/json"
            //headers.add("Content-Length" to js("Buffer").byteLength(request.body))
        }

        val credentials = AWSInstance.SharedIniFileCredentials(mapOf<String, String>())
        val signer = AWSInstance.Signers.V4(request, "es")
        signer.addAuthorization(credentials, Date())

        val client = AWSInstance.HttpClient()
        return Promise { resolve, reject ->
            client.handleRequest(request, null, { response ->
                var responseBody = ""
                response.on("data") { chunk ->
                    responseBody += chunk
                    chunk
                }
                response.on("end") { chunk ->
                    resolve(responseBody as T)
                }
            }, { error ->
                errorHandler(path, error.stack)
                reject(error)
            })
        }
    }
}