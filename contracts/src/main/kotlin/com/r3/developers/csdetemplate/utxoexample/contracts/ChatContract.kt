package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ChatContract: Contract {

    class Create: Command
    class Update: Command

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.commands.singleOrNull() ?: throw Exception("Require a single command ")

        "The output state should have two and only two participants" using {
            val output = transaction.outputContractStates.single() as ChatState
            output.participants.size== 2
        }
        when(command) {
            is Create -> {
                "When command is Create there should be no input states" using (transaction.inputContractStates.isEmpty())
                "When command is Create there should be one and only one output state" using (transaction.outputContractStates.size == 1)

            }
            is Update -> {
                "When command is Update there should be one and only one input states" using (transaction.inputContractStates.size == 1)
                "When command is Update there should be one and only one output state" using (transaction.outputContractStates.size == 1)
            }
        }
    }

    infix fun String.using(expr: Boolean) {
        if (!expr) throw IllegalArgumentException("Failed requirement: $this")
    }
    infix fun String.using(expr: () -> Boolean) {
        if (!expr.invoke()) throw IllegalArgumentException("Failed requirement: $this")
    }
}