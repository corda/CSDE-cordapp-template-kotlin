package com.r3.developers.csdetemplate.utxo

import net.corda.v5.application.flows.*
import net.corda.v5.ledger.utxo.*
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.security.PublicKey

class TestContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        // nothing ...
    }
}

@BelongsToContract(TestContract::class)
class TestUtxoState(
    val testField: String, override val participants: List<PublicKey>
) : ContractState

class TestCommand : Command


