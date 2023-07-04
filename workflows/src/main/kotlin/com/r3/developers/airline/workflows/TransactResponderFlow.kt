package com.r3.developers.airline.workflows

import com.r3.developers.airlines.states.Money
import com.r3.developers.airlines.states.Ticket
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.lang.IllegalArgumentException

@InitiatedBy(protocol = "transact-ticket")
class TransactResponderFlow : ResponderFlow {

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(session: FlowSession) {
        val transactionBuilder = utxoLedgerService.receiveTransactionBuilder(session)

        val myInfo = memberLookup.myInfo().ledgerKeys.first()
        val airline = requireNotNull(memberLookup.lookup(session.counterparty)?.ledgerKeys?.first()){
            throw IllegalArgumentException("Member does not exist in the group")
        }

        val unconsumedMoneyStates = utxoLedgerService.findUnconsumedStatesByType(Money::class.java)
            .firstOrNull{stateAndRef ->  stateAndRef.state.contractState.issuer == myInfo}
            ?:throw IllegalArgumentException("No money found")

        val money = unconsumedMoneyStates.state.contractState

        if(!money.checkOwner(myInfo)){
            throw IllegalArgumentException("The owner of the money is different and hence cannot be used by the buyer")
        }

        val newMoney = money.changeOwner(airline)

        transactionBuilder
            .addInputState(unconsumedMoneyStates.ref)
            .addOutputState(newMoney)

        utxoLedgerService.sendUpdatedTransactionBuilder(transactionBuilder,session)
        utxoLedgerService.receiveFinality(session){
            transaction ->
            val state = transaction.getOutputStates(Ticket::class.java).singleOrNull()?:
            throw CordaRuntimeException("Failed verification- transaction did not have one output Ticket state")
            if (state.price != money.value){
                throw CordaRuntimeException("Failed verification - value of money is not enough to purchase the ticket")
            }
        }
    }
}