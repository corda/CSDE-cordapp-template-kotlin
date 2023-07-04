package com.r3.developers.airline.workflows

import com.r3.developers.airlines.contracts.TicketCommands
import com.r3.developers.airlines.states.Ticket
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@InitiatingFlow(protocol = "create-ticket")
class IssueTicketFlow : ClientStartableFlow {

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

    private data class CreateAndIssueTicket (
        val seat : String,
        val departureDate : String,
        val price : Int
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, CreateAndIssueTicket::class.java)

        val seat = request.seat
        val departureDate = request.departureDate
        val price = request.price

        val notary = notaryLookup.notaryServices.single()

        val myKey = memberLookup.myInfo().ledgerKeys.first()

        val issuedTicket = Ticket(
            id = UUID.randomUUID(),
            issuer = myKey,
            holder = myKey,
            seat = seat,
            departureDate = departureDate,
            price = price,
            participants = listOf(myKey)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notary.name)
            .addOutputState(issuedTicket)
            .addCommand(TicketCommands.IssueTicket())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey))
            .toSignedTransaction()

        return try{
            utxoLedgerService.finalize(transaction, emptyList())
            issuedTicket.id.toString()
        }catch(e : Exception){
            "Flow failed, message: ${e.message}"
        }
    }
}