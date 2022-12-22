package com.r3.developers.csdetemplate.utxo

import net.corda.v5.application.flows.*
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.*

/*
As "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB":
{
  "clientRequestId": "listMy_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.ListMyTokenFlow",
  "requestData": {
  }
}
*/

@InitiatingFlow("utxo-listMy-token-flow-protocol")
class ListMyTokenFlow : RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("\n--- [ListMyTokenFlow] >>>")

        val myInfo = memberLookup.myInfo()

        val allUnusedTokens = utxoLedgerService.findUnconsumedStatesByType(TokenState::class.java)
        log.info("\n--- [ListMyTokenFlow] ALL UnusedTokens are ${allUnusedTokens.size}")

        val myUnusedTokens = allUnusedTokens.filter { it.state.contractState.owner.name == myInfo.name }
        log.info("\n--- [ListMyTokenFlow] MY UnusedTokens are ${myUnusedTokens.size}")
        myUnusedTokens.forEachIndexed { i, it ->
            log.info("\n--- [ListMyTokenFlow] $i with ref ${it.ref} and with state ${it.state}")
        }

        val resultMessage = "${myUnusedTokens.size}"
//        log.info("\n--- [ListMyTokenFlow] Finalized Tx Id is $resultMessage")
        log.info("\n--- [ListMyTokenFlow] <<<")
        return resultMessage
    }
}
