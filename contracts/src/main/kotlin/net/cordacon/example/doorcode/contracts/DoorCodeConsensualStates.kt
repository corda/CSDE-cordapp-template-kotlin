package net.cordacon.example.doorcode.contracts

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.consensual.ConsensualState
import net.corda.v5.ledger.consensual.transaction.ConsensualLedgerTransaction
import java.security.PublicKey

@CordaSerializable
data class DoorCode(val code: String)

@CordaSerializable
class DoorCodeConsensualState(val code: DoorCode, override val participants: List<PublicKey>) : ConsensualState {
    override fun verify(ledgerTransaction: ConsensualLedgerTransaction) {}
}