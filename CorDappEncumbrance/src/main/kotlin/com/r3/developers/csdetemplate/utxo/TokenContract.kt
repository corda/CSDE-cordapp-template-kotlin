package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.Issue
import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

/* Our contract, governing how our state will evolve over time. */
class TokenContract : Contract {

    interface Commands : Command {
        class Issue : Commands
        class Move : Commands
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.commands
        require(commands.size == 1) { "Commands must be one!" }
        when (commands[0]) {
            is Issue -> {
                verifyIssueCmd(transaction)
                return
            }

            is Commands.Move -> {
                verifyMoveCmd(transaction)
                return
            }

            else -> {
                throw IllegalArgumentException("Unsupported command ${commands[0]} !")
            }
        }
    }

    private fun verifyIssueCmd(transaction: UtxoLedgerTransaction) {
        //shape == params
        val inputStates = transaction.getInputStates(ContractState::class.java)
        require(inputStates.isEmpty()) { "Inputs must be empty!" }

        val outputStates = transaction.getOutputStates(TokenState::class.java)
        require(outputStates.size == 1) { "Outputs must be one!" }
        val tokenState = outputStates[0]

        //content == logic
        require(tokenState.amount > 0) { "Output.TokenState.Amount must be > 0!" }

        //signers
        if (!transaction.signatories.contains(tokenState.issuer.owningKey)) {
            throw IllegalArgumentException("Output.TokenState.Issuer must be required signer!");
        }
    }

    private fun verifyMoveCmd(transaction: UtxoLedgerTransaction) {
        // will do it later on ...
    }
}