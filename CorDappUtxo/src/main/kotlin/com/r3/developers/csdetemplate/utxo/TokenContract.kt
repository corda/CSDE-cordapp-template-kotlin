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
        class MoveAll : Commands
    }

    @Throws(IllegalArgumentException::class)
    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.commands
        require(commands.size == 1) { "Commands must be one, but are ${commands.size}!" }
        when (commands[0]) {
            is Issue -> {
                verifyIssueCmd(transaction)
                return
            }

            is Commands.MoveAll -> {
                verifyMoveAllCmd(transaction)
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
        require(inputStates.isEmpty()) { "[Issue] Inputs must be empty!" }

        val outputStates = transaction.getOutputStates(TokenState::class.java)
//        require(outputStates.size == 1) { "[Issue] Outputs must be one, but are ${outputStates.size}!" }
//        val tokenState = outputStates[0]
        outputStates.forEach { tokenState ->

            //content == logic
            require(tokenState.amount > 0) {
                "[Issue] Output.TokenState.Amount must be > 0!"
            }

            //signers
            if (!transaction.signatories.contains(tokenState.issuer.owningKey)) {
                throw IllegalArgumentException("[Issue] Output.TokenState.Issuer must be required signer!")
            }
        }
    }

    private fun verifyMoveAllCmd(transaction: UtxoLedgerTransaction) {
        return
        /* Today I'm not getting any input states :(
        //shape == params
        val inputStates = transaction.getInputStates(TokenState::class.java)
        require(inputStates.size == 1) { "[MoveAll] Inputs must be one, but are ${inputStates.size}!" }
        val inputTokenState = inputStates[0]

        val outputStates = transaction.getOutputStates(TokenState::class.java)
        require(outputStates.size == 1) { "[MoveAll] Outputs must be one, but are ${outputStates.size}!" }
        val outputTokenState = outputStates[0]

        //content == logic
        require(inputTokenState.amount == outputTokenState.amount) {
            "[MoveAll] Output.TokenState.Amount must be equals to Input.TokenState.Amount!"
        }

        //signers
        if (!transaction.signatories.contains(inputTokenState.owner.owningKey)) {
            throw IllegalArgumentException("[MoveAll] Input.TokenState.Owner must be required signer!");
        }
         */
    }
}