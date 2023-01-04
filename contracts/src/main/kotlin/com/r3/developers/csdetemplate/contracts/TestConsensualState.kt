package com.r3.developers.csdetemplate.contracts

import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import java.security.PublicKey

class TestConsensualState(
    val testField: String,
    override val participants: List<PublicKey>
) : ConsensualState {
    override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
}