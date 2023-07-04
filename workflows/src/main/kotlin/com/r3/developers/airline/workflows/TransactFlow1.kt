package com.r3.developers.airline.workflows

import com.r3.developers.airlines.contracts.TicketCommands
import com.r3.developers.airlines.states.Money
import com.r3.developers.airlines.states.TicketRep
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@InitiatingFlow(protocol = "transact-ticket-1")
class TransactFlow1 : ClientStartableFlow {

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
        val airline : MemberX500Name,
        val ticketId : UUID
    )

    @CordaSerializable
    data class requestPayload(
        val id: UUID
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService,transactRequest::class.java)
        val ticketId = request.ticketId

        val notaryInfo = notaryLookup.notaryServices.single()
        val myKey = memberLookup.myInfo().ledgerKeys.first()
        val airline = memberLookup.lookup(request.airline)
            ?.let{it.ledgerKeys.first()}
            ?:throw IllegalArgumentException("The airline doesn't exist in the network!")

        val session = flowMessaging.initiateFlow(request.airline)

        val response = session.sendAndReceive(
            TicketRep::class.java,
            requestPayload(ticketId)
        )

        val unconsumedMoneyStates = utxoLedgerService.findUnconsumedStatesByType(Money::class.java)
            .firstOrNull {stateAndRef -> stateAndRef.state.contractState.value == response.price}
            ?:throw IllegalArgumentException("No money found with the value of the ticket")

        val money = unconsumedMoneyStates.state.contractState
        val updatedMoney = money.changeOwner(airline)

        val txBuilder = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addInputState(unconsumedMoneyStates.ref)
            .addOutputState(updatedMoney)
            .addCommand(TicketCommands.Transact())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(myKey,airline))

        val newTxBuilder = utxoLedgerService.sendAndReceiveTransactionBuilder(txBuilder,session)

        val signedTransaction = newTxBuilder.toSignedTransaction()

        return try{
            utxoLedgerService.finalize(signedTransaction, listOf(session)).toString()
        }catch (e:Exception){
            "Flow failed, message: ${e.message}"
        }
    }
}



