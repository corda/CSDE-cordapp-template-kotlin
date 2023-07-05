package com.r3.developers.airlines.contracts

import com.r3.developers.airlines.states.Money
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.VisibilityChecker
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.lang.IllegalArgumentException

class MoneyContract : Contract{
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()){
            is MoneyCommands.IssueMoney -> {
                val output = transaction.getOutputStates(Money::class.java).first()
                require(transaction.outputContractStates.size == 1) {
                    "This transaction should only output one Money state"
                }
                require(output.value > 0) {
                    "The output money should have a value greater than zero"
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

    override fun isVisible(state: ContractState, checker: VisibilityChecker): Boolean {
        return when (state){
            is Money ->
                checker.containsMySigningKeys(listOf(state.holder))
            else ->
                super.isVisible(state, checker)
        }
    }

}