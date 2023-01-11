package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.StateAndRef
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
            ?: throw Exception("did not find an unique ChatState")

        return jsonMarshallingService.format(resolveMessagesFromBackchain(state, flowArgs.numberOfRecords ))
    }


    // todo: include who sent the message
    @Suspendable
    private fun resolveMessagesFromBackchain(stateAndRef: StateAndRef<ChatState>, numberOfRecords: Int): List<Pair<String,String>>{

        val messages = mutableListOf<Pair<String,String>>()

        var currentStateAndRef = stateAndRef
        var recordsToFetch = numberOfRecords
        var moreBackchain = true

        while (moreBackchain) {
            // Get transaction containing the state
            val transactionId = currentStateAndRef.ref.transactionHash

                val transaction = ledgerService.findLedgerTransaction(transactionId)
                    ?: throw Exception("Transaction $transactionId not found")

                // record message
                val output = transaction.getOutputStates(ChatState::class.java).singleOrNull()
                    ?: throw Exception("Expecting one and only one ChatState output for transaction $transactionId")
                messages.add(Pair(output.messageFrom.toString(), output.message))
                recordsToFetch--

                // check that there is a single input, if not break
                val inputStateAndRefs = transaction.inputStateAndRefs

                if (inputStateAndRefs.isEmpty() || recordsToFetch == 0) {
                    moreBackchain = false
                } else if (inputStateAndRefs.size > 1) {
                    throw Exception("More than one input state found for transaction $transactionId.")
                } else {
                    @Suppress("UNCHECKED_CAST")
                    currentStateAndRef = inputStateAndRefs.single() as StateAndRef<ChatState>
                }
        }
     return messages.toList()
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "get-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.GetChatFlow",
    "requestData": {
        "id":"** fill in id **",
        "numberOfRecords":"4"
    }
}
 */