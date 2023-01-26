package com.r3.developers.csdetemplate.utxo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.developers.csdetemplate.utxo.dto.TokenIssueRequest
import com.r3.developers.csdetemplate.utxo.service.retrofit2.FlowService
import com.r3.developers.csdetemplate.utxo.service.retrofit2.VNodeService
import net.corda.flow.rpcops.v1.types.request.StartFlowParameters
import net.corda.v5.base.types.MemberX500Name
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

internal class IssueTokenFlowIntegTestWithRetrofit {

    lateinit var cnNodes: Map<String, String>
    lateinit var x500Nodes: Map<String, String>

    companion object {
        lateinit var client: OkHttpClient
        lateinit var mapper: ObjectMapper
        lateinit var retrofit: Retrofit
        lateinit var vNodeService: VNodeService
        lateinit var flowService: FlowService

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

            retrofit = Retrofit.Builder()
                .baseUrl("https://localhost:8888/api/v1/")
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .client(client)
                .build()

            vNodeService = retrofit.create(VNodeService::class.java)
            flowService = retrofit.create(FlowService::class.java)
        }
    }

    @Test
    fun getVNodesTest() {
        val response = vNodeService.getAll().execute()
        assertEquals(HttpURLConnection.HTTP_OK, response.code())

        val responseBody = response.body()
        assertNotNull(responseBody)

        val vNodes = responseBody!!
        assertEquals(5, vNodes.virtualNodes.size)

        cnNodes =
            vNodes.virtualNodes.associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName!! to it.holdingIdentity.shortHash }
        assertEquals(vNodes.virtualNodes.size, cnNodes.size)
        x500Nodes =
            vNodes.virtualNodes.associate { it.holdingIdentity.shortHash to it.holdingIdentity.x500Name }
    }

    @Test
    fun postFlowTest() {
        if (!::cnNodes.isInitialized) {
            getVNodesTest()
        }
        val aliceHash = cnNodes["Alice"]!!
        val bobHash = cnNodes["Bob"]

        val tokenIssueRequest = TokenIssueRequest(1, 1, x500Nodes[bobHash]!!)
        val startFlowParameters: StartFlowParameters = StartFlowParameters(
            "issue-${UUID.randomUUID()}",
            "com.r3.developers.csdetemplate.utxo.IssueTokenFlow",
            tokenIssueRequest
        )

        val call = flowService.start(aliceHash, startFlowParameters)
        val response = call.execute()
        assertEquals(HttpURLConnection.HTTP_ACCEPTED, response.code())

        val responseBody = response.body()
        assertNotNull(responseBody)

        val flowStarted = responseBody!!
        println(flowStarted)
        assertEquals(aliceHash, flowStarted.holdingIdentityShortHash)
        assertEquals(startFlowParameters.clientRequestId, flowStarted.clientRequestId)
    }
}