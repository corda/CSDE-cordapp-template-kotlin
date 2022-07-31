package com.r3.examples

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable

data class ConcatInputMessage(
    val inText: String?
)

data class ConcatOutputMessage(
    val outText: String
)

class ConcatFlow : RPCStartableFlow {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val args = requestBody.getRequestBodyAs<ConcatInputMessage>(jsonMarshallingService)
        return jsonMarshallingService.format(ConcatOutputMessage("${args.inText ?: ""}$CONCAT_TEXT"))
    }
    companion object {
        val CONCAT_TEXT = "FOO"
    }
}

