package com.r3.developers.csdetemplate.utxo.service.craft5

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.developers.csdetemplate.utxo.service.craft5.C5RestService.Companion.getResponse
import com.r3.developers.csdetemplate.utxo.service.craft5.C5RestService.Companion.setupConnection
import net.corda.craft5.common.seconds
import net.corda.craft5.http.Http
import net.corda.craft5.util.retry
import net.corda.flow.rpcops.v1.types.request.StartFlowParameters
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponse
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponses
import net.corda.v5.base.types.MemberX500Name
import java.net.HttpURLConnection
import java.util.*

//TODO: emko: retry -> net.corda.craft5.util.retry ?
//TODO: emko: error handling
//TODO: emko: logging -> net.corda.craft5.logging.CraftLogger ?
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
            return retry(attempts = 12, cooldown = 5.seconds) {
                val flow = getFlow(http, initialFlow.holdingIdentityShortHash, initialFlow.clientRequestId!!)
                failFalse(flow.flowStatus == "COMPLETED", "Flow is '${flow.flowStatus}', but must be 'COMPLETED'")
                flow
            }
        }

        /*
        /flow/{holdingidentityshorthash}
        This method returns an array containing the statuses of all flows running for a specified holding identity.
        An empty array is returned if there are no flows running.
         */
        fun listFlows(http: Http, holdingIdentityShortHash: String): FlowStatusResponses {
            setupConnection(http)
            http.get("$path/$holdingIdentityShortHash")
            return getResponse(http, HttpURLConnection.HTTP_OK)
        }

        /*
        /flow/{holdingidentityshorthash}/{clientrequestid}
        This method gets the current status of the specified flow instance.
         */
        fun getFlow(http: Http, holdingIdentityShortHash: String, clientRequestId: String): FlowStatusResponse {
            setupConnection(http)
            http.get("$path/$holdingIdentityShortHash/$clientRequestId")
            return getResponse(http, HttpURLConnection.HTTP_OK)
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
            setupConnection(http)
            http.post("$path/$holdingIdentityShortHash", mapper.writeValueAsString(startFlowParameters))
            return getResponse(http, HttpURLConnection.HTTP_OK)
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
