package com.cbarratt.cordapp.exprs

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.util.contextLogger
import java.time.LocalDateTime

data class WaitNMillisArgs(
    val nMillis: Long? = 0
)

class WaitNMillis : RPCStartableFlow {
    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    override fun call(requestBody: RPCRequestData): String {
        //  val args = requestBody.getRequestBodyAs<ArraySumArgs>(jsonMarshallingService)
        val args = requestBody.getRequestBodyAs<WaitNMillisArgs>(jsonMarshallingService)
        val nMillis = args.nMillis ?: 0
        Thread.sleep(nMillis)
        return jsonMarshallingService.format("Waited for ${nMillis}ms")
    }
}
