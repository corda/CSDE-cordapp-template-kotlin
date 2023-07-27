package com.r3.cordapp.obligation.contract

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.math.BigDecimal

class ObligationContract : Contract {

    internal companion object{
        const val CONTRACT_RULE_COMMANDS =
            "On obligation contract executing, a single command of type ObligationContractCommand must be present in the transaction."
    }

    override fun verify(transaction: UtxoLedgerTransaction) {
        val command = checkNotNull(transaction.getCommands(ObligationContractCommand::class.java).singleOrNull()) {CONTRACT_RULE_COMMANDS}
        command.verify(transaction)
    }

    private interface ObligationContractCommand : Command{
        fun verify(transaction: UtxoLedgerTransaction)
    }

    class Create : ObligationContractCommand{

        internal companion object{
            const val CONTRACT_RULE_INPUTS =
                "On obligation creating, zero obligation states must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On obligation creating, only one obligation state must be created."

            const val CONTRACT_RULE_AMOUNT =
                "On obligation creating, the amount must be greater than zero."

            const val CONTRACT_RULE_PARTICIPANTS =
                "On obligation creating, the debtor and creditor must not be the same participant."

            const val CONTRACT_RULE_SIGNATORIES =
                "On obligation creating, the debtor must sign the transaction"
        }

        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(Obligation::class.java)
            val outputs = transaction.getOutputStates(Obligation::class.java)

            check(inputs.isEmpty()){CONTRACT_RULE_INPUTS}
            check(outputs.size == 1){CONTRACT_RULE_OUTPUTS}

            val output = outputs.single()

            check(output.amount>BigDecimal.ZERO){CONTRACT_RULE_AMOUNT}
            check(output.debtor != output.creditor){CONTRACT_RULE_PARTICIPANTS}
            check(output.debtor in transaction.signatories ){CONTRACT_RULE_SIGNATORIES}
        }

    }

    class Settle : ObligationContractCommand{

        internal companion object{
            const val CONTRACT_RULE_INPUTS =
                "On obligation settling, only one obligation state must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On obligation settling, only one obligation state must be created."

            const val CONTRACT_RULE_CHANGES =
                "On obligation settling, the debtor, creditor, symbol and id must not change."

            const val CONTRACT_RULE_AMOUNT =
                "On obligation settling, the amount of the consumed obligation must be greater than the amount of the created obligation."

            const val CONTRACT_RULE_AMOUNT_NON_NEGATIVE =
                "On obligation settling, the amount of the created obligation must be greater than, or equal to zero."

            const val CONTRACT_RULE_SIGNATORIES =
                "On obligation settling, the debtor must sign the transaction."
        }


        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(Obligation::class.java)
            val outputs = transaction.getOutputStates(Obligation::class.java)

            check(inputs.size==1) {CONTRACT_RULE_INPUTS}
            check(outputs.size==1) {CONTRACT_RULE_OUTPUTS}

            val input = inputs.single()
            val output = outputs.single()

            check(input.immutableEquals(output)) {CONTRACT_RULE_CHANGES}
            check(input.amount>output.amount) {CONTRACT_RULE_AMOUNT}
            check(output.amount>=BigDecimal.ZERO) {CONTRACT_RULE_AMOUNT_NON_NEGATIVE}
            check(output.debtor in transaction.signatories) {CONTRACT_RULE_SIGNATORIES}
        }
    }


    class Close : ObligationContractCommand{

        internal companion object{
            const val CONTRACT_RULE_INPUTS =
                "On obligation closing, only one obligation state must be consumed."

            const val CONTRACT_RULE_OUTPUTS =
                "On obligation closing, zero obligation states must be created."

            const val CONTRACT_RULE_AMOUNT =
                "On obligation closing, the amount of the consumed obligation must be zero."

            const val CONTRACT_RULE_SIGNATORIES =
                "On obligation closing, the debtor and creditor must sign the transaction."
        }


        override fun verify(transaction: UtxoLedgerTransaction) {
            val inputs = transaction.getInputStates(Obligation::class.java)
            val outputs = transaction.getOutputStates(Obligation::class.java)

            check(inputs.size==1) {CONTRACT_RULE_INPUTS}
            check(outputs.size==0) {CONTRACT_RULE_OUTPUTS}

            val input = inputs.single()

            check(input.amount.compareTo(BigDecimal.ZERO) == 0) {CONTRACT_RULE_AMOUNT}
            check(input.participants.all{ it in transaction.signatories }) {CONTRACT_RULE_SIGNATORIES}
        }
    }

}