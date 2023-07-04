package com.r3.developers.airlines.contracts

import com.r3.developers.airlines.states.Money
import com.r3.developers.airlines.states.Ticket
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.lang.IllegalArgumentException

class TicketContract : Contract{
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()){
            is TicketCommands.IssueTicket -> {
                val output = transaction.getOutputStates(Ticket::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only output one ticket state"
                }
                require(output.price > 0){
                    "The ticket should have a price greater than 0"
                }
                require(output.seat.isNotBlank()){
                    "The ticket should have a valid seat number"
                }
                require(output.departureDate.isNotBlank()){
                    "The ticket should have a valid seat number"
                }
            }
            is TicketCommands.Transact -> {
                require(transaction.inputContractStates.size == 2){
                    "This transaction should consume two states"
                }
                val buyingMoney = transaction.getInputStates(Money::class.java)
                val buyingTicket = transaction.getInputStates(Ticket::class.java)
                require(buyingMoney.isNotEmpty() && buyingTicket.isNotEmpty()){
                    "This transaction should have exactly one money and one ticket"
                }
                require(buyingMoney.single().value == buyingTicket.single().price){
                    "The value of the money used in the transaction must be equal to the price of the ticket"
                }
            }
            else -> {
                throw IllegalArgumentException("Incorrect type of Ticket commands: ${command::class.java.name}")
            }
        }
    }
}