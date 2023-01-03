package net.corda.v5.dc

import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class TokenDefinitionContract : Contract {

    private companion object {
        val log = contextLogger()
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.commands
        require(commands.size == 1) { "There must be only one command, but there are ${commands.size}!" }
        when (commands[0]) {
            is TokenDefinitionCommands.Create -> {
                //TODO("Not implemented!")
                return
            }

            is TokenDefinitionCommands.Update -> {
                //TODO("Not implemented!")
                return
            }

            is TokenDefinitionCommands.Destroy -> {
                //TODO("Not implemented!")
                return
            }

            else -> {
                throw IllegalArgumentException("Unsupported command ${commands[0].javaClass} !")
            }
        }
    }
}