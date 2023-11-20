package com.r3.developers.testapp.workflows

import com.r3.developers.testapp.DraftTx
import com.r3.developers.testapp.contracts.GenericStateContract
import com.r3.developers.testapp.states.GenericState
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.application.serialization.SerializationService
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "create-draft-tx-flow")
class CreateDraftTxFlow : ClientStartableFlow {

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var serializationService: SerializationService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java.enclosingClass)!!
    }

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        logger.info("Starting CreateDraftTxFlow")
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, CreateFlowArgs::class.java)
        logger.info("Transaction peer is ${flowArgs.receiverX500Name}")

        val returnValue = try {

            // 1. We self issue an asset on ledger
            val me = memberLookup.myInfo().ledgerKeys.first()
            val state = GenericState(me, me)

            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(notaryLookup.notaryServices.first().name)
                .addOutputState(state)
                .addCommand(GenericStateContract.GenericCommands.Issue)
                .addSignatories(state.issuer)
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))

            val stx = txBuilder.toSignedTransaction()

            val transaction = ledgerService.finalize(stx, emptyList()).transaction

            logger.info("Issued GenericState via transaction with ID ${transaction.id}")

            // 2. Start transaction to change owner
            val newOwnerKey = memberLookup.lookup(flowArgs.receiverX500Name)!!.ledgerKeys.first()

            val inputState = transaction.outputStateAndRefs.single()
            val outputState = GenericState((inputState.state.contractState as GenericState).issuer, newOwnerKey)
            val moveStx = ledgerService.createTransactionBuilder()
                .setNotary(notaryLookup.notaryServices.first().name)
                .addInputState(inputState.ref)
                .addOutputState(outputState)
                .addCommand(GenericStateContract.GenericCommands.Move)
                .addSignatories(listOf(me, newOwnerKey))
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .toSignedTransaction()

            // 3. Store transaction to be finalized later
            val bytes = serializationService.serialize(moveStx)
            val draftTx = DraftTx(UUID.randomUUID(), bytes.bytes)
            persistenceService.persist(draftTx)
            draftTx.id.toString()
        } catch (e: Exception) {
            logger.error("Something went wrong", e)
            "Error: ${e.message}"
        }

        return jsonMarshallingService.format(returnValue)
    }
}

data class CreateFlowArgs(val receiverX500Name: MemberX500Name)