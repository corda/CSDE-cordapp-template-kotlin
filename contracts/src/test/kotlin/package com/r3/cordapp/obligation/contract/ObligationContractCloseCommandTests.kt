package com.r3.cordapp.obligation.contract

import com.r3.corda.ledger.utxo.testing.buildTransaction
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ObligationContractCloseCommandTests : ObligationContractTest() {


    @Test
    fun `On obligation closing, the transaction should verify successfully`(){
        val transaction = buildTransaction{
            addInputState(settledObligationRef)
            addSignatories(aliceKey)
            addSignatories(bobKey)
            addCommand(ObligationContract.Close())
        }

        assertVerifies(transaction)
    }

    @Test
    fun `On obligation closing, only one obligation state must be consumed`(){
        val transaction = buildTransaction{
            addInputState(settledObligationRef)
            addInputState(settledObligationRef)
            addSignatories(aliceKey)
            addSignatories(bobKey)
            addCommand(ObligationContract.Close())
        }

        assertFailsWith(transaction, ObligationContract.Close.CONTRACT_RULE_INPUTS)
    }

    @Test
    fun `On obligation closing, zero obligation states must be created`(){
        val transaction = buildTransaction{
            addInputState(settledObligationRef)
            addOutputState(obligationState)
            addSignatories(aliceKey)
            addSignatories(bobKey)
            addCommand(ObligationContract.Close())
        }

        assertFailsWith(transaction, ObligationContract.Close.CONTRACT_RULE_OUTPUTS)
    }

    @Test
    fun `On obligation closing, the amount of the consumed obligation must be zero`(){
        val transaction = buildTransaction{
            addInputState(obligationRef)
            addSignatories(aliceKey)
            addSignatories(bobKey)
            addCommand(ObligationContract.Close())
        }

        assertFailsWith(transaction, ObligationContract.Close.CONTRACT_RULE_AMOUNT)
    }

    @Test
    fun `On obligation closing, the debtor and creditor must sign the transaction (debtor Missing)`(){
        val transaction = buildTransaction{
            addInputState(settledObligationRef)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Close())
        }

        assertFailsWith(transaction, ObligationContract.Close.CONTRACT_RULE_SIGNATORIES)
    }
    @Test
    fun `On obligation closing, the debtor and creditor must sign the transaction (creditor Missing)`(){
        val transaction = buildTransaction{
            addInputState(settledObligationRef)
            addSignatories(bobKey)
            addCommand(ObligationContract.Close())
        }

        assertFailsWith(transaction, ObligationContract.Close.CONTRACT_RULE_SIGNATORIES)
    }
}