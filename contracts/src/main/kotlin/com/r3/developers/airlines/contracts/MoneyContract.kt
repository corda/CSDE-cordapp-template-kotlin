package com.r3.developers.airlines.contracts

import com.r3.developers.airlines.states.Money
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.lang.IllegalArgumentException

class MoneyContract : Contract{
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()){
            is TicketCommands.IssueMoney -> {
                val output = transaction.getOutputStates(Money::class.java).first()
                require(transaction.outputContractStates.size == 1){
                    "This transaction should only output one Money state"
                }
                require(output.value > 0) {
                    "The output money should have a value greater than zero"
                }
            }
            is TicketCommands.Spend -> {
                val inputs = transaction.getInputStates(Money::class.java)
                require(transaction.inputContractStates.size == 1){
                    "This transaction should only have one Money as input"
                }
                require(transaction.signatories.contains(inputs.first().holder)){
                    "The holder of the input Money must be a signatory to the transaction"
                }
            }
            is TicketCommands.Transact -> {
                val inputs = transaction.getInputStates(Money::class.java)
                require(inputs.size == 1){
                    "This transaction should only have one money as input"
                }
                require (transaction.signatories.contains(inputs.first().holder)){
                    "The holder of the input money must be a signatory to the transaction"
                }
            }
            else -> {
                throw IllegalArgumentException("Incorrect type of Money commands: ${command::class.java.name}")
            }
        }
    }

}