package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.days
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.util.*


data class UpdateChatFlowArgs(val id: UUID, val message: String)

@InitiatingFlow("update-chat-protocol")
class UpdateChatFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("UpdateNewChatFlow.call() called")

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, UpdateChatFlowArgs::class.java)

            // look up state (this is very inefficient)
            val stateAndRef = ledgerService.findUnconsumedStatesByType(ChatState::class.java).singleOrNull {
                it.state.contractState.id == flowArgs.id
            } ?: throw Exception("Multiple or zero Chat states with id ${flowArgs.id} found")

            val myInfo = memberLookup.myInfo()
            val state = stateAndRef.state.contractState

            val members = state.participants.map {
                memberLookup.lookup(it) ?: throw Exception("Member not found from Key")}

            val otherMember = (members - myInfo).single()

            val newChatState = state.updateMessage(myInfo.name, flowArgs.message)

            val txBuilder= ledgerService.getTransactionBuilder()
                .setNotary(stateAndRef.state.notary)
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(1.days.toMillis()))
                .addOutputState(newChatState)
                .addInputState(stateAndRef.ref)
                .addCommand(ChatContract.Update())
                .addSignatories(newChatState.participants)

            @Suppress("DEPRECATION")
            val signedTransaction = txBuilder.toSignedTransaction(myInfo.ledgerKeys.first())

            return flowEngine.subFlow(AppendChatSubFlow(signedTransaction, otherMember.name))

        } catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "update-2",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.UpdateChatFlow",
    "requestData": {
        "id":"e1e0e45d-1b8f-41df-821f-fe3052784f45",
        "message": "How are you today?"
        }
}
 */