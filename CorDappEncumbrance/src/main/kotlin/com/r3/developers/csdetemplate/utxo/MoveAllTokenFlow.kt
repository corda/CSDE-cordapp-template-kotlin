package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.MoveAll
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.*
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator
import java.time.Instant

@CordaSerializable
data class TokenMoveRequest(val input: String, val owner: MemberX500Name)

/*
{
  "clientRequestId": "moveAll_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.MoveAllTokenFlow",
  "requestData": {
    "owner": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
    "input": "SHA-256D:FDCA2015F6D46C676508FE84AF48FE630190F775124E8EFA17EF09F69FF67D42"
  }
}
*/

@InitiatingFlow("utxo-token-moveAll-flow-protocol")
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

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("\n[MoveAllTokenFlow] Starting...")

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

        log.info("\n[MoveAllTokenFlow] $issuerParty")
        log.info("\n[MoveAllTokenFlow] ${issuerParty.name}")
        log.info("\n[MoveAllTokenFlow] ${issuerParty.owningKey}")
        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(issuerParty.owningKey)
        val sessions = listOf(flowMessaging.initiateFlow(ownerParty.name))
        val finalizedTx = utxoLedgerService.finalize(signedTx, sessions)
        log.info("\n[MoveAllTokenFlow] Finalized Tx is: $finalizedTx")
        finalizedTx.outputStateAndRefs.map { it.state.contractState }.forEach { log.info("\n[MoveAllTokenFlow] $it") }

        val resultMessage = finalizedTx.id.toString()
        log.info("\n[MoveAllTokenFlow] Finalized Tx Id is: $resultMessage")
        return resultMessage
    }
}

@InitiatedBy("utxo-token-moveAll-flow-protocol")
class MoveAllTokenRespFlow : ResponderFlow, UtxoTransactionValidator {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("\n[MoveAllTokenRespFlow] Starting...")
        val finalizedTx = utxoLedgerService.receiveFinality(session, this)
        val resultMessage = finalizedTx.id.toString()
        log.info("\n[MoveAllTokenRespFlow] Finalized Tx Id is: $resultMessage")
    }

    @Suspendable
    override fun checkTransaction(ledgerTransaction: UtxoLedgerTransaction) {
        log.info("\n[MoveAllTokenRespFlow] UtxoLedger Tx is: ${ledgerTransaction.id}")
        ledgerTransaction.outputContractStates.forEach { log.info("\n[MoveAllTokenRespFlow] $it") }
    }
}
