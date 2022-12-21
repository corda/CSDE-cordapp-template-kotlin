package com.r3.developers.utxodemo.contracts

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class TestContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
    }
}

class TestCommand : Command