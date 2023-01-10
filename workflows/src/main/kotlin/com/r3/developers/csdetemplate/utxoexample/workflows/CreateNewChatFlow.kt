package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
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

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {


        log.info("CNCF: CreateNewChatFlow.call() called")

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


            val txBuilder= utxoLedgerService.getTransactionBuilder()
                .setNotary(Party(notary.name, notaryKey))
                .setTimeWindowBetween(Instant.now(), Instant.now().plusMillis(1.days.toMillis()))
                .addOutputState(chatState)
                .addCommand(ChatContract.Create())
                .addSignatories(chatState.participants)

            @Suppress("DEPRECATION")
            val signedTransaction = txBuilder.toSignedTransaction(myInfo.ledgerKeys.first())

            val session = flowMessaging.initiateFlow(otherMember.name)

            return try {
                val finalizedSignedTransaction = utxoLedgerService.finalize(
                    signedTransaction,
                    listOf(session)
                )
                finalizedSignedTransaction.id.toString().also {
                    log.info("Success! Response: $it")
                }
            } catch (e: Exception) {
                log.warn("Finality failed", e)
                "Finality failed, ${e.message}"
            }

        } catch (e: Exception) {
            log.warn("Failed to process utxo flow for request body '$requestBody' because:'${e.message}'")
            throw e
        }
    }
}

@InitiatedBy("create-chat-protocol")
class CreateNewChatResponderFlow: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        try {
            val finalizedSignedTransaction = utxoLedgerService.receiveFinality(session) { ledgerTransaction ->
                val state = ledgerTransaction.outputContractStates.first() as ChatState
                if (checkForBannedWords(state.message) && checkMessageFromMatchesKey(state)) throw IllegalStateException("Failed verification")
                log.info("Verified the transaction- ${ledgerTransaction.id}")
            }
            log.info("Finished responder flow - ${finalizedSignedTransaction.id}")
        } catch (e: Exception) {
            log.warn("Exceptionally finished responder flow", e)
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