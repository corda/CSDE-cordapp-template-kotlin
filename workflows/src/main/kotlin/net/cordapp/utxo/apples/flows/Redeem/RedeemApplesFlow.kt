package net.cordapp.utxo.apples.flows.redeem

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.cordapp.utxo.apples.states.AppleStamp
import net.cordapp.utxo.apples.states.BasketOfApples
import net.cordapp.utxo.apples.contracts.BasketOfApplesContract
import java.time.Instant
import java.time.temporal.ChronoUnit
import net.corda.v5.base.types.MemberX500Name
import java.security.PublicKey
import java.util.UUID

data class RedeemApplesRequest(val buyer: MemberX500Name, val stampId: UUID)

@InitiatingFlow(protocol = "redeem-apples")
class RedeemApplesFlow : ClientStartableFlow {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs<RedeemApplesRequest>(jsonMarshallingService, RedeemApplesRequest::class.java)
        val buyerName = request.buyer
        val stampId = request.stampId

        // Retrieve the notaries public key (this will change)
        val notaryInfo = notaryLookup.notaryServices.single()
//        val notaryKey = memberLookup.lookup().single {
//            it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
//        }.ledgerKeys.first()
//        val notary = notaryLookup

        val ourIdentity = memberLookup.myInfo()

        val buyer = memberLookup.lookup(buyerName)
            ?: throw IllegalArgumentException("The buyer does not exist within the network")

        val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
            ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")

        val basketOfApplesStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull()
            ?: throw IllegalArgumentException("There are no baskets of apples")

        val originalBasketOfApples = basketOfApplesStampStateAndRef.state.contractState

        val updatedBasket = originalBasketOfApples.changeOwner(buyer)

        // Create the transaction
        @Suppress("DEPRECATION")
        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputStates(appleStampStateAndRef.ref, basketOfApplesStampStateAndRef.ref)
            .addOutputState(updatedBasket)
            .addCommand(BasketOfApplesContract.Commands.Redeem())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(mutableListOf(ourIdentity.ledgerKeys.first(), buyer.ledgerKeys.first()))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(buyerName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session)).toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}
