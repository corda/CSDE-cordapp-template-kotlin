package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.base.util.days
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant


data class CreateNewChatFlowArgs(val chatName: String, val message: String, val otherMember: String)

@InitiatingFlow("create-chat-protocol")
class CreateNewChatFlow: RPCStartableFlow {

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
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var flowEngine: FlowEngine

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("CreateNewChatFlow.call() called")

        try {
            val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateNewChatFlowArgs::class.java)

            val myInfo = memberLookup.myInfo()
            val otherMember = memberLookup.lookup(MemberX500Name.parse(flowArgs.otherMember)) ?: throw IllegalArgumentException("can't find other member")

            val chatState = ChatState(
                chatName = flowArgs.chatName,
                messageFrom = myInfo.name,
                message = flowArgs.message,
                participants = listOf(myInfo.ledgerKeys.first(), otherMember.ledgerKeys.first())
            )

            val notary = notaryLookup.notaryServices.first()
            val notaryKey = memberLookup.lookup().first {
                it.memberProvidedContext["corda.notary.service.name"] == notary.name.toString()
            }.ledgerKeys.first()

            val txBuilder= ledgerService.getTransactionBuilder()
                .setNotary(Party(notary.name, notaryKey))
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(1.days.toMillis()))
                .addOutputState(chatState)
                .addCommand(ChatContract.Create())
                .addSignatories(chatState.participants)

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
    "clientRequestId": "create-1",
    "flowClassName": "com.r3.developers.csdetemplate.utxoexample.workflows.CreateNewChatFlow",
    "requestData": {
        "chatName":"Chat with Bob",
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
        "message": "Hello Bob"
        }
}
 */