package com.r3.cordapp.obligation.contract

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

class ObligationLedgerEvolutionTests : ContractTest() {

    @Test
    @Order(1)
    fun `Should be able to create an obligation`(){
        val transaction = buildTransaction {
            addOutputState(Obligation(aliceKey, bobKey, 100.00.toBigDecimal(), "USD"))
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        assertVerifies(transaction)
    }

    @Test
    @Order(2)
    fun `Should be able to partially settle an existing obligation`(){

        val obligation = ledgerService.findUnconsumedStatesByType(Obligation::class.java).single()

        val transaction = buildTransaction {
            addInputState(obligation.ref)
            addOutputState(obligation.state.contractState){settle(5.00.toBigDecimal())}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }

        assertVerifies(transaction)
    }

    @Test
    @Order(3)
    fun `Should be able to fully settle an existing obligation`(){

        val obligation = ledgerService.findUnconsumedStatesByType(Obligation::class.java).single()

        val transaction = buildTransaction {
            addInputState(obligation.ref)
            addOutputState(obligation.state.contractState){settle(95.00.toBigDecimal())}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }

        assertVerifies(transaction)
    }

    @Test
    @Order(3)
    fun `Should be able to close fully settled obligation`(){

        val obligation = ledgerService.findUnconsumedStatesByType(Obligation::class.java).single()

        val transaction = buildTransaction {
            addInputState(obligation.ref)
            addSignatories(aliceKey, bobKey)
            addCommand(ObligationContract.Close())
        }

        assertVerifies(transaction)
    }
}