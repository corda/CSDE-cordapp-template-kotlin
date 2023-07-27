package com.r3.cordapp.obligation.workflows.create

import com.r3.cordapp.obligation.contract.Obligation
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.SubFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction

class CreateObligationFlowResponder(private val flowSession: FlowSession) : SubFlow<UtxoSignedTransaction> {

    private lateinit var memberLookup: MemberLookup

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    override fun call(): UtxoSignedTransaction {
        return utxoLedgerService.receiveFinality(flowSession){
            val output = it.getOutputStates(Obligation::class.java).single()
            val debtor = checkNotNull(memberLookup.lookup(output.debtor)){ "Unknown debtor." }

            check(flowSession.counterparty==debtor.name) {"Session was initiated by incorrect participant"}
        }.transaction

    }
}