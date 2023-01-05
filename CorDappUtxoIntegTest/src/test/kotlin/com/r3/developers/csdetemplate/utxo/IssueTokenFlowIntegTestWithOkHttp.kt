package com.r3.developers.csdetemplate.utxo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class IssueTokenFlowIntegTestWithOkHttp {

    companion object {
        lateinit var client: OkHttpClient
        lateinit var mapper: ObjectMapper

        @BeforeAll
        @JvmStatic
        fun init() {
            // see https://www.baeldung.com/okhttp-client-trust-all-certificates
            val trustAllCerts: Array<TrustManager> = arrayOf(
                object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return emptyArray()
                    }
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())

            client = OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(Interceptor { chain ->
                    // see https://gist.github.com/namhyun-gu/df668e651fe445a55c836284cfbdb215
                    val request = chain.request()
                    val builder = request.newBuilder()
                        .header("Authorization", Credentials.basic("admin", "admin"))
                    chain.proceed(builder.build())
                })
                .build()

            mapper = jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    @Test
    fun getVNodesTest() {
        val request = Request.Builder()
            .url("https://localhost:8888/api/v1/virtualnode")
            .get()
            .build()

        val response = client.newCall(request).execute()
        assertEquals(HttpURLConnection.HTTP_OK, response.code)

        val responseBody = response.body
        assertNotNull(responseBody)

        val vNodes = mapper.readValue(responseBody?.bytes(), VirtualNodes::class.java)
        assertEquals(5, vNodes.virtualNodes.size)

        val cnNodes =
            vNodes.virtualNodes.associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName to it.holdingIdentity.shortHash }
        assertEquals(vNodes.virtualNodes.size, cnNodes.size)
    }
}