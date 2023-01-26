package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.TokenContract.Commands.Issue
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.receive
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.*
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator
import java.time.Instant
import java.time.temporal.ChronoUnit

@CordaSerializable
data class TokenIssueRequest(
    val amount: Int,
    val times: Int = 1,
    val owner: MemberX500Name,
    val withEncumbrance: Boolean = false
)

/*
As "CN=Alice, OU=Test Dept, O=R3, L=London, C=GB":
{
  "clientRequestId": "issue_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.IssueTokenFlow",
  "requestData": {
    "owner": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB",
    "amount": 10,
    "times": 2,
    "withEncumbrance": false
  }
}
*/

@InitiatingFlow("utxo-issue-token-flow-protocol")
class IssueTokenFlow : RPCStartableFlow {

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
        log.info("\n--- [IssueTokenFlow] >>>")

        val notaryParty = getNotaryParty(notaryLookup, memberLookup)

        val request = requestBody.getRequestBodyAs<TokenIssueRequest>(jsonMarshallingService)
        val ownerMember = memberLookup.lookup(request.owner) ?: throw IllegalArgumentException("Owner not found!")
        val ownerParty = Party(ownerMember.name, ownerMember.sessionInitiationKey)

        val myInfo = memberLookup.myInfo()
        val meAsAnIssuer = Party(myInfo.name, myInfo.sessionInitiationKey)

        val outputTokenStates = mutableListOf<TokenState>()
        for (i in 1..request.times) {
            outputTokenStates.add(TokenState(meAsAnIssuer, ownerParty, request.amount))
        }

        var utxoTxBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(notaryParty)
            // a time windows is mandatory
            // emko:issue#3
            // !!! => .setTimeWindowBetween(Instant.MIN, Instant.MAX) => java.lang.ArithmeticException: long overflow
            .setTimeWindowBetween(Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS))
            .addCommand(Issue())
            .addSignatories(listOf(meAsAnIssuer.owningKey))

        // emko:issue#6
        // !!! be sure not to add them twice ... maybe some checks in the builder are needed?
        utxoTxBuilder = if (request.withEncumbrance) {
            utxoTxBuilder.addEncumberedOutputStates("all-for-one", outputTokenStates)
        } else {
            utxoTxBuilder.addOutputStates(outputTokenStates)
        }
        utxoTxBuilder.getEncumbranceGroups().forEach { entry ->
            log.info("\n--- [IssueTokenFlow] EncumbranceGroup name ${entry.key} and size ${entry.value.size}")
        }

        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(meAsAnIssuer.owningKey)

        val sessions = listOf(flowMessaging.initiateFlow(ownerParty.name))
        sessions.forEach { log.info("\n--- [IssueTokenFlow] --- ${it.receive<String>()}") }
        val finalizedTx = utxoLedgerService.finalize(signedTx, sessions)
        log.info("\n--- [IssueTokenFlow] Finalized Tx is $finalizedTx")
        finalizedTx.outputStateAndRefs.map { it.state.contractState }.forEachIndexed { i, it ->
            log.info("\n--- [IssueTokenFlow] OutputState.$i $it")
        }

        val resultMessage = finalizedTx.id.toString()
        log.info("\n--- [IssueTokenFlow] Finalized Tx Id is $resultMessage")
        log.info("\n--- [IssueTokenFlow] <<<")
        return resultMessage
    }
}

@InitiatedBy("utxo-issue-token-flow-protocol")
class IssueTokenRespFlow : ResponderFlow, UtxoTransactionValidator {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("\n--- [IssueTokenRespFlow] >>>")
        session.send("emko")
        val finalizedTx = utxoLedgerService.receiveFinality(session, this)
        val resultMessage = finalizedTx.id.toString()
        log.info("\n--- [IssueTokenRespFlow] Finalized Tx Id is $resultMessage")
        log.info("\n--- [IssueTokenRespFlow] <<<")
    }

    @Suspendable
    override fun checkTransaction(ledgerTransaction: UtxoLedgerTransaction) {
        log.info("\n--- [IssueTokenRespFlow] UtxoLedger Tx is ${ledgerTransaction.id}")
        ledgerTransaction.outputContractStates.forEachIndexed { i, it ->
            log.info("\n--- [IssueTokenRespFlow] OutputState.$i $it")
        }
    }
}
