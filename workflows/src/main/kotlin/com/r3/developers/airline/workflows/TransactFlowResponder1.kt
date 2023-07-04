package com.r3.developers.airline.workflows

import com.r3.developers.airlines.states.Ticket
import com.r3.developers.airlines.states.TicketRep
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.lang.IllegalArgumentException
import java.util.*

@InitiatedBy(protocol = "transact-ticket-1")
class TransactFlowResponder1 :ResponderFlow{

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup


    @Suspendable
    override fun call(session: FlowSession) {
        val infoRequest = session.receive(TransactFlow1.requestPayload::class.java)
        val unconsumedTickets = utxoLedgerService.findUnconsumedStatesByType(Ticket::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == infoRequest.id }
            ?:throw IllegalArgumentException("No ticket of that ID was found")

        val ticket = unconsumedTickets.state.contractState

        val ticketInfoObject = TicketRep(
            seat = ticket.seat,
            departureDate = ticket.departureDate,
            price = ticket.price,
            participants = ticket.participants
        )
        session.send(ticketInfoObject)

        val transactionBuilder = utxoLedgerService.receiveTransactionBuilder(session)
        val buyer = requireNotNull(memberLookup.lookup(session.counterparty)?.ledgerKeys?.first()){
            throw IllegalArgumentException("Buyer doesn't exist in the group")
        }
        val newTicket = ticket.changeOwner(buyer)

        transactionBuilder
            .addInputState(unconsumedTickets.ref)
            .addOutputState(newTicket)

        utxoLedgerService.sendUpdatedTransactionBuilder(transactionBuilder,session)
        utxoLedgerService.receiveFinality(session){
            transaction ->
            val state = transaction.getOutputStates(Ticket::class.java).singleOrNull()?:
            throw CordaRuntimeException("Failed verification - transaction did not have one output ticket state")
        }
    }
}
