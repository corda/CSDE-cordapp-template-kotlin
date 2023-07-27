package com.r3.cordapp.obligation.workflows.create

import com.r3.cordapp.obligation.contract.Obligation
import com.r3.cordapp.obligation.contract.ObligationContract
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.membership.NotaryInfo

class CreateObligationFlow(
    private val obligation: Obligation,
    private val notaryInfo: NotaryInfo,
    private val sessions: List<FlowSession>
) : SubFlow<UtxoSignedTransaction> {

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(): UtxoSignedTransaction {
        val transaction = utxoLedgerService.createTransactionBuilder()
            .addOutputState(obligation)
            .addSignatories(obligation.debtor)
            .addCommand(ObligationContract.Create())
            .setNotary(notaryInfo.name)
            .toSignedTransaction()

        return utxoLedgerService.finalize(transaction, sessions).transaction
    }
}