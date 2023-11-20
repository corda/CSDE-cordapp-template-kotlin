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

        val returnValue = try {
            // read draft tx
            val draftTx = persistenceService.find(DraftTx::class.java, flowArgs.draftTxId)!!
            logger.info("Draft TX BYTES ARE: ${draftTx.tx}")
            val stx = serializationService.deserialize(draftTx.tx!!, UtxoSignedTransaction::class.java)
            logger.info("DESERIALIZED DRAFT TX IS: $stx")
            // finalize
            val peerKey = (stx.signatories - memberLookup.myInfo().ledgerKeys.first()).single()
            val peer = memberLookup.lookup(peerKey)
            val sessions = listOf(flowMessaging.initiateFlow(peer!!.name))
            val transactionId = ledgerService.finalize(stx, sessions).transaction.id.toString()
            logger.info("Transaction $transactionId finalized")
            "Transaction with ID $transactionId finalized"
        } catch (e: Exception) {
            logger.error("Something went wrong", e)
            "Error: ${e.message}"
        }

        return jsonMarshallingService.format(returnValue)
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
        try {
            ledgerService.receiveFinality(session) {
                val state = it.getInputStates(GenericState::class.java).single()
                logger.info("Responder finalizing transaction with id ${it.id}")
                logger.info("State $state changed owners")
            }
        } catch (e: Exception) {
            logger.error("Responder flow finished with exception", e)
        }
    }

}

data class MyFlowArgs(val draftTxId: UUID)