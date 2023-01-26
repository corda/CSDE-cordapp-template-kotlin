package com.r3.developers.csdetemplate.utxo.service.craft5

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import net.corda.craft5.http.Headers
import net.corda.craft5.http.Http
import net.corda.craft5.http.util.json
import net.corda.flow.rpcops.v1.types.request.StartFlowParameters
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponse
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponses
import net.corda.v5.base.types.MemberX500Name
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

//TODO: emko: retry
//TODO: emko: error handling
//TODO: emko: logging
class FlowService {

    companion object {
        var path = "flow"

        //JsonMarshallingService uses some custom serializers, so we need custom deserializers for that !!!
        val memberX500NameModule = SimpleModule()
            .addDeserializer(MemberX500Name::class.java, object : JsonDeserializer<MemberX500Name>() {
                override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): MemberX500Name {
                    return MemberX500Name.parse(p!!.valueAsString)
                }
            })
        var mapper: ObjectMapper = jacksonObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(memberX500NameModule)

        fun waitForFlowCompletion(http: Http, initialFlow: FlowStatusResponse): FlowStatusResponse {
            TimeUnit.SECONDS.sleep(5)
            for (i in 0..10) {
                val flow = getFlow(http, initialFlow.holdingIdentityShortHash, initialFlow.clientRequestId!!)
                if (flow.flowStatus != "COMPLETED") {
                    TimeUnit.SECONDS.sleep(5)
                } else {
                    return flow
                }
            }
            throw TimeoutException()
        }

        /*
        /flow/{holdingidentityshorthash}
        This method returns an array containing the statuses of all flows running for a specified holding identity.
        An empty array is returned if there are no flows running.
         */
        fun listFlows(http: Http, holdingIdentityShortHash: String): FlowStatusResponses {
            http.baseUri = URI(VNodeService.url)
            http.baseHeaders = mapOf(
                Headers.basicAuthorization("admin", "admin")
            )

            http.get("$path/$holdingIdentityShortHash")
            val response = http.response()
            if (response.status() == HttpURLConnection.HTTP_OK) {
                return response.json()
            }
            throw IOException("${response.uri()} => ${response.body()}")
        }

        /*
        /flow/{holdingidentityshorthash}/{clientrequestid}
        This method gets the current status of the specified flow instance.
         */
        fun getFlow(http: Http, holdingIdentityShortHash: String, clientRequestId: String): FlowStatusResponse {
            http.baseUri = URI(VNodeService.url)
            http.baseHeaders = mapOf(
                Headers.basicAuthorization("admin", "admin")
            )

            http.get("$path/$holdingIdentityShortHash/$clientRequestId")
            val response = http.response()
            if (response.status() == HttpURLConnection.HTTP_OK) {
                return response.json()
            }
            throw IOException("${response.uri()} => ${response.body()}")
        }

        /*
        /flow/{holdingidentityshorthash}
        This method starts a new instance for the specified flow for the specified holding identity.
         */
        fun startFlow(
            http: Http,
            holdingIdentityShortHash: String,
            startFlowParameters: StartFlowParameters
        ): FlowStatusResponse {
            http.baseUri = URI(VNodeService.url)
            http.baseHeaders = mapOf(
                Headers.basicAuthorization("admin", "admin")
            )

            http.post("$path/$holdingIdentityShortHash", mapper.writeValueAsString(startFlowParameters))
            val response = http.response()
            if (response.status() == HttpURLConnection.HTTP_ACCEPTED) {
                return response.json()
            }
            throw IOException("${response.uri()} => ${response.body()}")
        }

        fun startFlow(
            http: Http,
            holdingIdentityShortHash: String,
            flowClassName: String,
            requestData: Any
        ): FlowStatusResponse {
            val startFlowParameters = StartFlowParameters(
                "${flowClassName.split(".").last()}-${UUID.randomUUID()}",
                flowClassName,
                requestData
            )
            return startFlow(http, holdingIdentityShortHash, startFlowParameters)
        }
    }
}
