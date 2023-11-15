package com.r3.developers.testapp.contracts

import net.corda.v5.base.exceptions.CordaRuntimeException
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class GenericStateContract : Contract {
    sealed class GenericCommands : Command {
        object Issue : GenericCommands()
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = transaction.getCommands(GenericCommands::class.java).singleOrNull()
            ?: throw CordaRuntimeException("Requires a single command.")
    }
}