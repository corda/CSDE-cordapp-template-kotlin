package com.r3.cordapp.obligation.contract

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import net.corda.v5.ledger.utxo.StateAndRef
import net.corda.v5.ledger.utxo.StateRef


abstract class ObligationContractTest : ContractTest() {

    protected val obligationStateAndRef: StateAndRef<Obligation> by lazy {
        val obligation = Obligation(aliceKey, bobKey, 101.00.toBigDecimal(),"USD")
        val transaction = buildTransaction{
            addOutputState(obligation)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        transaction.toLedgerTransaction().getOutputStateAndRefs(Obligation::class.java).single()
    }

    protected val obligationState: Obligation get() = obligationStateAndRef.state.contractState
    protected val obligationRef: StateRef get() = obligationStateAndRef.ref


    protected val settledObligationStateAndRef: StateAndRef<Obligation> by lazy {
        val obligation = Obligation(aliceKey, bobKey, 1.00.toBigDecimal(),"USD")
        val createTransaction = buildTransaction{
            addOutputState(obligation)
            addSignatories(aliceKey)
            addCommand(ObligationContract.Create())
        }

        val createdObligation = createTransaction.toLedgerTransaction().getOutputStateAndRefs(Obligation::class.java).single()

        val settleTransaction = buildTransaction{
            addInputState(createdObligation.ref)
            addOutputState(createdObligation.state.contractState) {settle(1.00.toBigDecimal())}
            addSignatories(aliceKey)
            addCommand(ObligationContract.Settle())
        }

        settleTransaction.toLedgerTransaction().getOutputStateAndRefs(Obligation::class.java).single()


    }
    protected val settledObligationRef: StateRef get() = settledObligationStateAndRef.ref
}