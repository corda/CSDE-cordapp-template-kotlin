package com.r3.developers.testapp.workflows

import com.r3.developers.testapp.DraftTx
import com.r3.developers.testapp.states.GenericState
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import org.slf4j.LoggerFactory
import java.util.UUID

@InitiatingFlow(protocol = "finalize-draft-tx-flow")
class FinalizeDraftTxFlow : ClientStartableFlow {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var serializationService: SerializationService

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var memberLookup: MemberLookup

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        logger.info("Starting FinalizeDraftTxFlow")
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, MyFlowArgs::class.java)
        logger.info("Draft TX ID is ${flowArgs.draftTxId}")

        // read draft tx
        val draftTx = persistenceService.find(DraftTx::class.java, flowArgs.draftTxId)!!
        logger.info("Draft TX BYTES ARE: ${draftTx.tx}")
        val stx = serializationService.deserialize(draftTx.tx!!, UtxoSignedTransaction::class.java)
        logger.info("DESERIALIZED DRAFT TX IS: $stx")
        // finalize
        val peerKey = (stx.signatories - memberLookup.myInfo().ledgerKeys.first()).single()
        val peer = memberLookup.lookup(peerKey)
        val sessions = listOf(flowMessaging.initiateFlow(peer!!.name))

        try {
            ledgerService.finalize(stx, sessions).transaction.id.toString()
            logger.info("Transaction ${stx.id} finalized")
        } catch (e: Exception) {
            logger.error("Flow finalization failed. ", e)
        }

        logger.info("Receiving status message")
        val statusMessage = sessions.single().receive(String::class.java)
        logger.info("Received status message $statusMessage")

        if (!statusMessage.contains("finalized")) {
            throw (CordaRuntimeException("Received message from receiver: $statusMessage"))
        }

        return jsonMarshallingService.format("Received message from receiver: $statusMessage")
    }
}

@InitiatedBy(protocol = "finalize-draft-tx-flow")
class FinalizeDraftTxFlowResponder : ResponderFlow {

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    private companion object {
        val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Suspendable
    override fun call(session: FlowSession) {
        logger.info("Received request to finalize transaction")

        var statusMessage: String? = null
        try {
            ledgerService.receiveFinality(session) {
                val state = it.getInputStates(GenericState::class.java).single()
//                throwError(state)
                statusMessage = "Transaction with ID ${it.id} finalized"
                logger.info("Responder finalizing transaction with id ${it.id}")
                logger.info("State $state changed owners")
            }
        } catch (e: Exception) {
            logger.error("Transaction finalization failed", e)
            statusMessage = e.message
        }

        logger.info("Sending back status message $statusMessage")
        session.send(statusMessage!!)
    }

//    private fun throwError(state: ContractState) {
//        if (state is GenericState)
//            throw IllegalArgumentException("Wrong state!!!!")
//    }
}

data class MyFlowArgs(val draftTxId: UUID)