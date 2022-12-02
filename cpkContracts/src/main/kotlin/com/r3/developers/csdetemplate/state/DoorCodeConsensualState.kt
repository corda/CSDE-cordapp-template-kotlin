package com.r3.developers.csdetemplate.state

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import org.slf4j.Logger
import java.security.PublicKey

@CordaSerializable
class DoorCodeConsensualState(val code: DoorCode, override val participants: List<PublicKey>) : ConsensualState {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log: Logger = contextLogger()
    }

    override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {
        log.info("Verifying ledgerTransaction ${ledgerTransaction.id} for states ${ledgerTransaction.states} ...")
    }
}