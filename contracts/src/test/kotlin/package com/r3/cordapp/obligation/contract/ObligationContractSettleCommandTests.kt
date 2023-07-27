package com.r3.cordapp.obligation.contract

import com.r3.corda.ledger.utxo.testing.buildTransaction
import org.junit.jupiter.api.Test
import java.util.*

class ObligationContractSettleCommandTests : ObligationContractTest(){
    @Test
    fun `On obligation settling, the transaction should verify successfully`(){
        val transaction = buildTransaction{
            addInputState(obligationRef)
            addOutputState(obligationState) {settle(100.00.toBigDecimal())}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertVerifies(transaction)
    }

    @Test
    fun `On obligation contract executing, a single command of type ObligationContractCommand must be present in the transaction (Settle command)`(){
        val transaction = buildTransaction{
            addInputState(obligationRef)
            addOutputState(obligationState) {settle(100.00.toBigDecimal())}
            addSignatories(aliceKey)
        }
        assertFailsWith(transaction, ObligationContract.CONTRACT_RULE_COMMANDS)
    }

    @Test
    fun `On obligation settling, only one obligation state must be consumed`(){
        val transaction = buildTransaction{
            addInputState(obligationRef)
            addInputState(obligationRef)
            addOutputState(obligationState) {settle(100.00.toBigDecimal())}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_INPUTS)
    }

    @Test
    fun `On obligation settling, only one obligation state must be created`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { settle(100.00.toBigDecimal()) }
            addOutputState(obligationState) { settle(100.00.toBigDecimal()) }
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_OUTPUTS)
    }

    @Test
    fun `On obligation settling, the debtor, creditor, symbol and id must not change (debtor changed)`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { copy(debtor = charlieKey) }
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_CHANGES)
    }

    @Test
    fun `On obligation settling, the debtor, creditor, symbol and id must not change (symbol changed)`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { copy(symbol = "GBP") }
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_CHANGES)
    }

    @Test
    fun `On obligation settling, the debtor, creditor, symbol and id must not change (id changed)`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { copy(id = UUID.randomUUID()) }
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_CHANGES)
    }

    @Test
    fun `On obligation settling, the amount of the consumed obligation must be greater than the amount of the created obligation`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_AMOUNT)
    }

    @Test
    fun `On obligation settling, the amount of the created obligation must be greater than, or equal to zero`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { settle(200.00.toBigDecimal()) }
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_AMOUNT_NON_NEGATIVE)
    }



    @Test
    fun `On obligation settling, the debtor must sign the transaction`() {
        val transaction = buildTransaction {
            addInputState(obligationRef)
            addOutputState(obligationState) { settle(100.00.toBigDecimal()) }
            addCommand(ObligationContract.Settle())
        }
        assertFailsWith(transaction, ObligationContract.Settle.CONTRACT_RULE_SIGNATORIES)
    }
}