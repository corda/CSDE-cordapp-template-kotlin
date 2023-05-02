package net.cordapp.utxo.apples.flows.pack

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.cordapp.utxo.apples.states.BasketOfApples
import net.cordapp.utxo.apples.contracts.BasketOfApplesContract
import java.time.Instant
import java.time.temporal.ChronoUnit

data class PackApplesRequest(val appleDescription: String, val weight: Int)

class PackApplesFlow : ClientStartableFlow {

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
        val request = requestBody.getRequestBodyAs<PackApplesRequest>(jsonMarshallingService, PackApplesRequest::class.java)
        val appleDescription = request.appleDescription
        val weight = request.weight

        // Retrieve the notaries public key (this will change)
        val notary = notaryLookup.notaryServices.single()
        val notaryKey = memberLookup.lookup().single {
            it.memberProvidedContext["corda.notary.service.name"] == notary.name.toString()
        }.ledgerKeys.first()

        val myInfo = memberLookup.myInfo()


        // Building the output BasketOfApples state
        val basket = BasketOfApples(
            description = appleDescription,
            farm = myInfo,
            owner = myInfo,
            weight = weight,
            participants = listOf(myInfo.ledgerKeys.first())
        )

        // Create the transaction
        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(basket)
            .addCommand(BasketOfApplesContract.Commands.PackBasket())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(myInfo.ledgerKeys.first()))
            .toSignedTransaction()

        return try {
            // Record the transaction, no sessions are passed in as the transaction is only being
            // recorded locally
            utxoLedgerService.finalize(transaction, emptyList()).toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}