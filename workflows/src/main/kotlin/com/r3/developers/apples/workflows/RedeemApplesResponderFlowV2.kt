package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatedBy(protocol = "redeem-apples-2")
class RedeemApplesResponderFlowV2 : ResponderFlow {

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        try {
            val stampId = session.receive(UUID::class.java)

            // As the issuer, we should have a copy of the AppleStamp referenced by the id.
            val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
                .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
                ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")

            val notaryInfo = notaryLookup.notaryServices.single()

            val myKey = memberLookup.myInfo().let { it.ledgerKeys.first() }

            val stampHolder = appleStampStateAndRef.state.contractState.holder

            val holderName = memberLookup.lookup(stampHolder)
                ?.let { it.name }
                ?: throw IllegalArgumentException("The holder does not exist within the network")

            // Verify that the requester is the owner of the AppleStamp, then build a transaction to
            // spend and change owner of a basket in our vault if valid
            require(holderName == session.counterparty) {
                "The initiator of this request is not the holder of the AppleStamp"
            }

            val basketOfApplesStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
                .firstOrNull()
                ?: throw IllegalArgumentException("There are no baskets of apples")

            val originalBasketOfApples = basketOfApplesStampStateAndRef.state.contractState

            val updatedBasket = originalBasketOfApples.changeOwner(stampHolder)

            // Create the transaction
            val transaction = utxoLedgerService.createTransactionBuilder()
                .setNotary(notaryInfo.name)
                .addInputStates(appleStampStateAndRef.ref, basketOfApplesStampStateAndRef.ref)
                .addOutputState(updatedBasket)
                .addCommand(AppleCommands.Redeem())
                .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
                .addSignatories(listOf(myKey, stampHolder))
                .toSignedTransaction()

            session.send("Redeem responder flow ok so far, continuing")

            utxoLedgerService.finalize(transaction, listOf(session))
        }
        catch (e: IllegalArgumentException) {
            // Re-throw as a CordaRuntimeException to propogate the message back to the sender
            session.send("Redeem responder flow failed, reason: ${e.message}")
        }
    }
}
