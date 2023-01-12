package com.r3.developers.csdetemplate.utxo

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.*
import java.util.stream.Collectors

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

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("\n--- [ListMyTokenFlow] >>>")

        val myInfo = memberLookup.myInfo()
        log.info("\n--- [ListMyTokenFlow] I am ${myInfo.name}")

//        try {
//            log.info("\n--- [ListMyTokenFlow] ? ${ContractState::class.java}")
//            /*
//            !!! =>
//            net.corda.sandbox.SandboxException: Class net.corda.v5.ledger.utxo.ContractState was not found in any sandbox in the sandbox group.
//	at net.corda.sandbox.internal.SandboxGroupImpl.loadClassFromMainBundles(SandboxGroupImpl.kt:70) ~[?:?]
//             */
//            val allUnusedTokens = utxoLedgerService.findUnconsumedStatesByType(ContractState::class.java)
//            log.info("\n--- [ListMyTokenFlow] ALL UnusedTokens are ${allUnusedTokens.size}")
//        } catch (e: Exception) {
//            log.error("emko", e)
//        }

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
//        return resultMessage

        log.info("\n--- [emko] 1")
        val tokens = myUnusedTokens.stream()
            .map { it.state.contractState }
            .map { MyToken(it) }
            .collect(Collectors.toList())
        log.info("\n--- [emko] 2")
        val writeValueAsString = jsonMarshallingService.format(MyTokens(tokens))
        log.info("\n--- [emko] $writeValueAsString")
        log.info("\n--- [emko] 3")
        return writeValueAsString
    }
}
