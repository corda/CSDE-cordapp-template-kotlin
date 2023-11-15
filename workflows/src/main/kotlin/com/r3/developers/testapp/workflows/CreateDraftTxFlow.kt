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
        val returnValue = try {
            val me = memberLookup.myInfo().ledgerKeys.first()
            val state = GenericState(me, me)

            val txBuilder = ledgerService.createTransactionBuilder()
                .setNotary(notaryLookup.notaryServices.first().name)
                .addOutputState(state)
                .addCommand(GenericStateContract.GenericCommands.Issue)
                .addSignatories(state.participants)
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))

            val stx = txBuilder.toSignedTransaction()

            // store transaction to be finalized later
            val bytes = serializationService.serialize(stx)
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