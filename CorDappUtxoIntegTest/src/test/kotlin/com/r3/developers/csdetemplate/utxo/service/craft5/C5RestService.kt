package com.r3.developers.csdetemplate.utxo.service.craft5

import net.corda.craft5.http.Headers
import net.corda.craft5.http.Http
import net.corda.craft5.http.util.json
import java.io.IOException
import java.net.URI

class C5RestService {

    companion object {
        fun setupConnection(http: Http): Http {
            http.baseUri = URI("https://localhost:8888/api/v1/")
            http.baseHeaders = mapOf(
                Headers.basicAuthorization("admin", "admin")
            )
            return http
        }

        inline fun <reified T> getResponse(http: Http, expectedStatus: Int): T {
            val response = http.response()
            if (response.status() == expectedStatus) {
                return response.json()
            }
            throw IOException("${response.uri()} => ${response.body()}")
        }
    }
}
