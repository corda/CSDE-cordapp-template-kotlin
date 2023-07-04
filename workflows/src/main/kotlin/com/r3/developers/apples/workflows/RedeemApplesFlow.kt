package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

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

    private data class redeemApplesRequest(
        val buyer : MemberX500Name,
        val stampId : UUID
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,redeemApplesRequest::class.java)
        val buyerName = request.buyer
        val stampId = request.stampId

        val notaryInfo = notaryLookup.notaryServices.single()

        val myKey = memberLookup.myInfo().let { it.ledgerKeys.first() } //Why is it we use let here but not in the other examples

        val buyer = memberLookup.lookup(buyerName)
            ?.let{it.ledgerKeys.first()}
            ?: throw IllegalArgumentException("The buyer doesn't exist within the network")

        val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull{ stateAndRef -> stateAndRef.state.contractState.id == stampId } //What is state and ref
            ?:throw IllegalArgumentException("No apple stamp matching stamp id $stampId")

        val basketOfApplesStateAndRef = utxoLedgerService.findUnconsumedStatesByType(BasketOfApples::class.java)
            .firstOrNull{ basketStateAndRef -> basketStateAndRef.state.contractState.owner == appleStampStateAndRef.state.contractState.issuer}
            ?:throw IllegalArgumentException("There are no eligible baskets of apples")

        val originalBasketOfApples = basketOfApplesStateAndRef.state.contractState

        val updatedBasket = originalBasketOfApples.changeOwner(buyer)

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputStates(appleStampStateAndRef.ref,basketOfApplesStateAndRef.ref) //The state is being made historic
            .addOutputState(updatedBasket)
            .addCommand(AppleCommands.Redeem())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey,buyer))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(buyerName)

        return try{
            utxoLedgerService.finalize(transaction, listOf(session)).toString()
        }catch(e: Exception){
            "Flow failed, message: ${e.message}"
        }
    }
}