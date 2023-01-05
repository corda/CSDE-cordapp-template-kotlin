package com.r3.developers.utxodemo.contracts

import com.r3.developers.utxodemo.states.TokenState
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.getInputStates
import net.corda.v5.ledger.utxo.transaction.getOutputStates


class TokenContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.r3.developers.csdetemplate.TokenContract"
    }

    interface Commands : Command {
        class Issue : Commands
        class Transfer : Command
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.commands
        require(commands.size == 1) { "There must be a single command but are ${commands.size}." }

        when (commands.single()) {
            is Commands.Issue -> {
                require(transaction.getInputStates<TokenState>().isEmpty())
                { "There must be 0 input state." }
                require(transaction.getOutputStates<TokenState>().isNotEmpty())
                { "There must be at least one output state" }
            }

            is Commands.Transfer -> {
                require(transaction.getInputStates<TokenState>().isNotEmpty())
                { "There must be at least one input state." }
            }

            else -> throw IllegalArgumentException("Unsupported command type: ${commands[0]}")
        }
    }
}