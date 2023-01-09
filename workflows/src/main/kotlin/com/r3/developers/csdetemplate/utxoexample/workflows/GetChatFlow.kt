package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.util.*


data class GetChatFlowArgs(val id: UUID, val numberOfRecords: Int)

class GetChatFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, GetChatFlowArgs::class.java)

        val states = ledgerService.findUnconsumedStatesByType(ChatState::class.java)
        val state = states.singleOrNull {it.state.contractState.id == flowArgs.id}
            ?: throw Exception("contract state not found **fix error message**")

        val messages = state.state.contractState.messages.takeLast(flowArgs.numberOfRecords)

        return jsonMarshallingService.format(messages)

    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "get-3",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.GetChatFlow",
    "requestData": {
        "id":"e1e0e45d-1b8f-41df-821f-fe3052784f45",
        "numberOfRecords":"4"
    }
}
 */