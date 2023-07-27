package com.r3.cordapp.obligation.contract

import com.r3.corda.ledger.utxo.testing.buildTransaction
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ObligationContractCreateCommandTests : ObligationContractTest() {

    @Test
    fun `On obligation creating, the transaction should verify successfully`(){
        val transaction = buildTransaction{
            addOutputState(obligationState)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertVerifies(transaction)
    }

    @Test
    fun `On obligation contract executing, a single command of type ObligationContractCommand must be present in the transaction (create command)`(){
        val transaction = buildTransaction {
            addOutputState(obligationState)
            addSignatories(aliceKey)
        }

        assertFailsWith(transaction, ObligationContract.CONTRACT_RULE_COMMANDS)
    }


    @Test
    fun `On obligation creating, zero obligation states must be consumed`(){
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertFailsWith(transaction, ObligationContract.Create.CONTRACT_RULE_INPUTS)
    }

    @Test
    fun `On obligation creating, only one obligation state must be created`(){
        val transaction = buildTransaction {
            addOutputState(obligationState)
            addOutputState(obligationState)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertFailsWith(transaction, ObligationContract.Create.CONTRACT_RULE_OUTPUTS)
    }

    @Test
    fun `On obligation creating, the amount must be greater than zero`(){
        val transaction = buildTransaction {
            addOutputState(obligationState) {copy(amount = BigDecimal.ZERO)}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertFailsWith(transaction, ObligationContract.Create.CONTRACT_RULE_AMOUNT)
    }

    @Test
    fun `On obligation creating, the debtor and creditor must not be the same participant`(){
        val transaction = buildTransaction {
            addOutputState(obligationState) {copy(debtor = aliceKey, creditor = aliceKey)}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertFailsWith(transaction, ObligationContract.Create.CONTRACT_RULE_PARTICIPANTS)
    }

    @Test
    fun `On obligation creating, the debtor must sign the transaction`(){
        val transaction = buildTransaction {
            addOutputState(obligationState)
            addCommand(ObligationContract.Create())
        }

        assertFailsWith(transaction, ObligationContract.Create.CONTRACT_RULE_SIGNATORIES)
    }
}