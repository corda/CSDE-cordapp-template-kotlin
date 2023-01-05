package com.r3.developers.csdetemplate.utxoexample.contracts

import net.corda.v5.ledger.utxo.Command
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.ContractVerificationException
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction

class ChatContract: Contract {

    class Create: Command
    class Fail: Command

    override fun verify(transaction: UtxoLedgerTransaction) {
//        val command = transaction.commands.singleOrNull() ?: throw Exception("Require a single command ")
//        when(command) {
//            is Create -> { assert(true) }
//            is Fail -> { assert(false) }
//        }
    }
}
// todo: check only 2 participants