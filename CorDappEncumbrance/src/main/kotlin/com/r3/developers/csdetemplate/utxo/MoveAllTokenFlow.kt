package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.MoveAll
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.*
import java.time.Instant

@CordaSerializable
data class TokenMoveRequest(val input: String, val owner: MemberX500Name)

/*
{
  "clientRequestId": "moveAll_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.MoveAllTokenFlow",
  "requestData": {
    "owner": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
    "input": "SHA-256D:05B4E02CC9EF0AAB00C9652849936409163693D54A9F0DEEDEBD29B42A08CEA1"
  }
}
*/

@InitiatingFlow("utxo-token-move-flow-protocol")
class MoveAllTokenFlow : RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("[MoveAllTokenFlow] Starting...")

//        val notaryInfo =
//            notaryLookup.notaryServices.firstOrNull() ?: throw IllegalArgumentException("Notary not available!")
//        val notaryParty = Party(notaryInfo.name, notaryInfo.publicKey)

        val request = requestBody.getRequestBodyAs<TokenMoveRequest>(jsonMarshallingService)
        val ownerMember = memberLookup.lookup(request.owner) ?: throw IllegalArgumentException("Owner not found!")
        val ownerParty = Party(ownerMember.name, ownerMember.sessionInitiationKey)

        // issuer == old/current owner
        val issuerMember = memberLookup.myInfo()
        val issuerParty = Party(issuerMember.name, issuerMember.sessionInitiationKey)

        val inputTxHash = SecureHash.parse(request.input)
        val inputTx =
            utxoLedgerService.findLedgerTransaction(inputTxHash) ?: throw IllegalArgumentException("Token not found!")

        val inputStateAndRef = inputTx.outputStateAndRefs[0]
        val inputTokenState = inputStateAndRef.state.contractState as TokenState
        val outputTokenState = TokenState(inputTokenState.issuer, ownerParty, inputTokenState.amount)

        val utxoTxBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(issuerParty) // notary is not working as of now
            .setTimeWindowBetween(Instant.MIN, Instant.MAX) // a time windows is mandatory
            .addInputState(inputStateAndRef.ref)
            .addOutputState(outputTokenState)
            .addCommand(MoveAll())
            .addSignatories(listOf(issuerParty.owningKey))

        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(issuerParty.owningKey)
        val finalizedTx = utxoLedgerService.finalize(signedTx, emptyList())
        log.info("[MoveAllTokenFlow] Finalized Tx is: $finalizedTx")
        finalizedTx.outputStateAndRefs.map { it.state.contractState }.forEach { log.info("[MoveAllTokenFlow] $it") }

        val resultMessage = finalizedTx.id.toString()
        log.info("[MoveAllTokenFlow] Finalized Tx Id is: $resultMessage")
        return resultMessage
    }
}
