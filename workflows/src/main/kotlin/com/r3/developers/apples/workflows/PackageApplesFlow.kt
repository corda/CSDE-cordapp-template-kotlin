package com.r3.developers.apples.workflows
import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit


class PackageApplesFlow : ClientStartableFlow {
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private data class PackageApplesRequest(
        val appleDescription: String,
        val weight : Int
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, PackageApplesRequest::class.java)
        val appleDescription = request.appleDescription
        val weight = request.weight

        val notary = notaryLookup.notaryServices.single()

        val myKey = memberLookup.myInfo().ledgerKeys.first()

        val basket = BasketOfApples(
            description = appleDescription,
            farm = myKey,
            owner = myKey,
            weight = weight,
            participants = listOf(myKey)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(basket)
            .addCommand(AppleCommands.PackBasket())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey))
            .toSignedTransaction()

        return try {
            utxoLedgerService.finalize(transaction, emptyList()).toString()
        }catch(e: Exception){
            "Flow failed, message: ${e.message}"
        }
    }
}