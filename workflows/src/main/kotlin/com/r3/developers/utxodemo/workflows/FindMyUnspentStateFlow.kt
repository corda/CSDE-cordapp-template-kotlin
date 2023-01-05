package com.r3.developers.utxodemo.workflows

import com.r3.developers.utxodemo.states.TokenState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.UtxoLedgerService

@InitiatingFlow(protocol = "find-unspent-flow")
class FindMyUnspentStateFlow : RPCStartableFlow {
    companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val header = "[${FindMyUnspentStateFlow::class.simpleName}]"
        log.info("$header Start ${FindMyUnspentStateFlow::class.simpleName} flow")

        val myInfo = memberLookup.myInfo()

        log.info("$header Retrieve my unspent states")
        val allUnspentStates = utxoLedgerService.findUnconsumedStatesByType(TokenState::class.java)
        val myStateAndRefs = allUnspentStates.filter { it.state.contractState.owner.name == myInfo.name }
        val myStateRefs = myStateAndRefs.map { it.ref }

        log.info("$header Compiling the result")
        var result = ""
        myStateRefs.forEach {
           result += "${it.index}: ${it.transactionHash}\n"
        }

        return "$header $result"
    }
}