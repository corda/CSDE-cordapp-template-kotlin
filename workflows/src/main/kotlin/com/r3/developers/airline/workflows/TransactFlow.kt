package com.r3.developers.airline.workflows

import com.r3.developers.airlines.contracts.TicketCommands
import com.r3.developers.airlines.states.Money
import com.r3.developers.airlines.states.Ticket
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

@InitiatingFlow(protocol = "transact-ticket")
class TransactFlow : ClientStartableFlow{

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

    private data class transactRequest(
        val buyer : MemberX500Name,
        val ticketId : UUID
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,transactRequest::class.java)
        val ticketId = request.ticketId

        val notaryInfo = notaryLookup.notaryServices.single()

        val myKey = memberLookup.myInfo().ledgerKeys.first()
        val buyer = memberLookup.lookup(request.buyer)
            ?.let{it.ledgerKeys.first()}
            ?: throw IllegalArgumentException("The buyer doesn't exist in the network")

        val ticketToBeUsedRef = utxoLedgerService.findUnconsumedStatesByType(Ticket::class.java)
            .firstOrNull{stateAndRef -> stateAndRef.state.contractState.id == ticketId}
            ?:throw IllegalArgumentException("No ticket exists with Id $ticketId")

//        val moneyToBeUsedRef = utxoLedgerService.findUnconsumedStatesByType(Money::class.java)
//            .firstOrNull{stateAndRef -> stateAndRef.state.contractState.id == moneyId}
//            ?:throw IllegalArgumentException("No money exists with Id $moneyId")



        val ticket = ticketToBeUsedRef.state.contractState

//        if(!money.checkOwner(buyer)){
//            throw IllegalArgumentException("The owner of the money is different and hence cannot be used by the buyer")
//        }
        if(ticket.checkOwner(buyer)){
            throw IllegalArgumentException("The owner of the ticket cannot buy their own ticket")
        }
//        else if(!money.returnValue().equals(ticket.returnPrice())){
//            throw IllegalArgumentException("There is not enough money to purchase the ticket")
//        }

//        val updatedMoney = money.changeOwner(myKey)
        val updatedTicket = ticket.changeOwner(buyer)

        val txBuilder = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputState(ticketToBeUsedRef.ref)
            .addOutputState(updatedTicket)
            .addCommand(TicketCommands.Transact())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey,buyer))

        val session = flowMessaging.initiateFlow(request.buyer)

        val newTxBuilder = utxoLedgerService.sendAndReceiveTransactionBuilder(txBuilder,session)

        val signedTransaction = newTxBuilder.toSignedTransaction()

        return try{
            utxoLedgerService.finalize(signedTransaction, listOf(session)).toString()
        }catch (e:Exception){
            "Flow failed, message: ${e.message}"
        }
    }
}




//        val transaction = utxoLedgerService.createTransactionBuilder()
//            .setNotary(notaryInfo.name)
//            .addInputStates(moneyToBeUsedRef.ref,ticketToBeUsedRef.ref)
//            .addOutputStates(updatedMoney,updatedTicket)
//            .addCommand(TicketCommands.Transact())
//            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
//            .addSignatories(listOf(myKey,buyer))
//            .toSignedTransaction()