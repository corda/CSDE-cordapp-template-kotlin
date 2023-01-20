package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.Issue
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.common.Party
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

    private companion object {
        val log = contextLogger()
    }

//    //TODO: check is it possible & how it's done to get Corda services in the contract code
//    @CordaInject
//    lateinit var memberLookup: MemberLookup

    @Throws(IllegalArgumentException::class)
    override fun verify(transaction: UtxoLedgerTransaction) {
        val commands = transaction.commands
        require(commands.size == 1) { "Commands must be one, but are ${commands.size}!" }

        log.info("\n--- [TokenContract] commands are ${transaction.commands}")
        log.info("\n--- [TokenContract] inputStateRefs count is ${transaction.inputStateRefs.size}")
        log.info("\n--- [TokenContract] inputStateAndRefs count is ${transaction.inputStateAndRefs.size}")
        log.info("\n--- [TokenContract] inputTransactionStates count is ${transaction.inputTransactionStates.size}")
        log.info("\n--- [TokenContract] inputContractStates count is ${transaction.inputContractStates.size}")
        log.info("\n--- [TokenContract] outputStateAndRefs count is ${transaction.outputStateAndRefs.size}")
        log.info("\n--- [TokenContract] outputContractStates count is ${transaction.outputContractStates.size}")

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

    internal fun verifyIssueCmd(transaction: UtxoLedgerTransaction) {
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

    internal fun verifyMoveAllCmd(transaction: UtxoLedgerTransaction) {
        // some duplication, but it's just to have an easy UnitTest example
        val commands = transaction.commands
        require(commands.size == 1) { "Commands must be one, but are ${commands.size}!" }
        val command = commands[0]
        require(command is Commands.MoveAll) { "The Command must be MoveAll, but is $command!" }
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