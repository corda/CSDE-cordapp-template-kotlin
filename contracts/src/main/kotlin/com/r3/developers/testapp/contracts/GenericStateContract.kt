package com.r3.developers.testapp.contracts

import com.r3.developers.testapp.states.GenericState
import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class GenericStateContract : Contract {
    sealed class GenericCommands : Command {
        object Issue : GenericCommands()
        object Move : GenericCommands()
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(GenericCommands::class.java).singleOrNull()
            ?: throw CordaRuntimeException("Requires a single command.")
        if (command == GenericCommands.Move) {
            val inputState = transaction.getInputStates(GenericState::class.java).single()
            val outputState = transaction.getOutputStates(GenericState::class.java).single()
            require(inputState.owner != outputState.owner)
        }
    }
}