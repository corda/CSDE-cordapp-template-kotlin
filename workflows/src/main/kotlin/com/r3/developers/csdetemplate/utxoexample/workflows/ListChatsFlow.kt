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


data class ChatStateResults(val id: UUID, val chatName: String,val messageFromName: String, val message: String)

class ListChatsFlow : RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

    val states = ledgerService.findUnconsumedStatesByType(ChatState::class.java)
    val results = states.map {
        ChatStateResults(
            it.state.contractState.id,
            it.state.contractState.chatName,
            it.state.contractState.messageFrom.toString(),
            it.state.contractState.message) }

        return jsonMarshallingService.format(results)
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "list-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.ListChatsFlow",
    "requestData": {}
}
*/
