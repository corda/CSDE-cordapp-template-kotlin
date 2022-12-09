package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.Issue
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.*
import java.time.Instant

@CordaSerializable
data class TokenIssueRequest(val amount: Int, val owner: MemberX500Name)

/*
{
  "clientRequestId": "issue_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.TokenIssueFlow",
  "requestData": {
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "amount": 10
  }
}
*/

@InitiatingFlow("utxo-token-issue-flow-protocol")
class TokenIssueFlow : RPCStartableFlow {

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
        log.info("TokenIssueFlow starting...")

//        val notaryInfo =
//            notaryLookup.notaryServices.firstOrNull() ?: throw IllegalArgumentException("Notary not available!")
//        val notaryParty = Party(notaryInfo.name, notaryInfo.publicKey)

        val request = requestBody.getRequestBodyAs<TokenIssueRequest>(jsonMarshallingService)
        val ownerMember = memberLookup.lookup(request.owner) ?: throw IllegalArgumentException("Owner not found!")
        val ownerParty = Party(ownerMember.name, ownerMember.sessionInitiationKey)

        val issuerMember = memberLookup.myInfo()
        val issuerParty = Party(issuerMember.name, issuerMember.sessionInitiationKey)

        val tokenState = TokenState(issuerParty, ownerParty, request.amount)

        val utxoTxBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(issuerParty) // notary is not working as of now
            .setTimeWindowBetween(Instant.MIN, Instant.MAX) // a time windows is mandatory
            .addOutputState(tokenState)
            .addCommand(Issue())
            .addSignatories(listOf(issuerParty.owningKey))
        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(issuerParty.owningKey)
        val finalizedTx = utxoLedgerService.finalize(signedTx, emptyList())
        log.info("Finalized Tx is: $finalizedTx")
        finalizedTx.outputStateAndRefs.map { it.state.contractState }.forEach { log.info(it.toString()) }

        val resultMessage = finalizedTx.id.toString()
        log.info("Response: $resultMessage")
        return resultMessage
    }
}
