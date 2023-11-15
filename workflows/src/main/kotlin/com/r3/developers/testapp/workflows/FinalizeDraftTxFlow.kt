package com.r3.developers.testapp.workflows

import com.r3.developers.testapp.DraftTx
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
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
            val transactionId = ledgerService.finalize(stx, emptyList()).transaction.id.toString()
            logger.info("Transaction $transactionId finalized")
            "Transaction with ID $transactionId finalized"
        } catch (e: Exception) {
            logger.error("Something went wrong", e)
            "Error: ${e.message}"
        }

        return jsonMarshallingService.format(returnValue)
    }
}

data class MyFlowArgs(val draftTxId: UUID)