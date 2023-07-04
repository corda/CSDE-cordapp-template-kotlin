package com.r3.developers.airline.workflows

import com.r3.developers.airlines.contracts.TicketCommands
import com.r3.developers.airlines.states.Money
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "create-money")
class IssueMoneyFlow : ClientStartableFlow {

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

    private data class CreateAndIssueMoney(
        val value : Int
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,CreateAndIssueMoney::class.java)
        val value = request.value

        val notary = notaryLookup.notaryServices.single()

        val myKey = memberLookup.myInfo().ledgerKeys.first()

        val issuedMoney = Money(
            id = UUID.randomUUID(),
            issuer = myKey,
            holder = myKey,
            value = value,
            participants = listOf(myKey)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(issuedMoney)
            .addCommand(TicketCommands.IssueMoney())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey))
            .toSignedTransaction()

        return try{
            utxoLedgerService.finalize(transaction, emptyList())
            issuedMoney.id.toString()
        }catch(e : Exception){
            "Flow failed, message: ${e.message}"
        }
    }

}