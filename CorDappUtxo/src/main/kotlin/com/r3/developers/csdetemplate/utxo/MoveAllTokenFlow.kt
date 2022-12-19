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
data class TokenMoveRequest(val input: String, val maxIndex: Int = 1, val owner: MemberX500Name)

/*
As "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB":
{
  "clientRequestId": "moveAll_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.MoveAllTokenFlow",
  "requestData": {
    "owner": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
    "input": "SHA-256D:5645A8DFD7089C5C8A65B675F815C34C30E160387733A4F53DC3EBA91605530E",
    "maxIndex": 3
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
        log.info("\n--- [MoveAllTokenFlow] Starting...")

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
            utxoLedgerService.findSignedTransaction(inputTxHash) ?: throw IllegalArgumentException("Token not found!")

        inputTx.outputStateAndRefs.forEachIndexed { i, it ->
            log.info("\n--- [MoveAllTokenFlow] InputState.$i with index ${it.ref.index} and with Encumbrance.name ${it.state.encumbrance ?: "n/a"}")
        }
        val inputStateAndRefs = inputTx.outputStateAndRefs.filter { it.ref.index < request.maxIndex }
        val inputStateRefs = inputStateAndRefs.map { it.ref }
        val outputTokenStates = inputStateAndRefs
            .map { it.state.contractState as TokenState }
            .map { TokenState(it.issuer, ownerParty, it.amount) }

        val utxoTxBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(issuerParty) // notary is not working as of now
            .setTimeWindowBetween(Instant.MIN, Instant.MAX) // a time windows is mandatory
            .addInputStates(inputStateRefs)
//            .addReferenceInputStates(inputStateRefs)
            .addOutputStates(outputTokenStates)
            .addCommand(MoveAll())
            .addSignatories(listOf(issuerParty.owningKey))

        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(issuerParty.owningKey)
        val sessions = listOf(flowMessaging.initiateFlow(ownerParty.name))
        val finalizedTx = utxoLedgerService.finalize(signedTx, sessions)
        log.info("\n--- [MoveAllTokenFlow] Finalized Tx is $finalizedTx")
        finalizedTx.outputStateAndRefs.map { it.state.contractState }.forEachIndexed { i, it ->
            log.info("\n--- [MoveAllTokenFlow] OutputState.$i $it")
        }

        val resultMessage = finalizedTx.id.toString()
        log.info("\n--- [MoveAllTokenFlow] Finalized Tx Id is $resultMessage")
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
        log.info("\n--- [MoveAllTokenRespFlow] Starting...")
        val finalizedTx = utxoLedgerService.receiveFinality(session, this)
        val resultMessage = finalizedTx.id.toString()
        log.info("\n--- [MoveAllTokenRespFlow] Finalized Tx Id is $resultMessage")
    }

    @Suspendable
    override fun checkTransaction(ledgerTransaction: UtxoLedgerTransaction) {
        log.info("\n--- [MoveAllTokenRespFlow] UtxoLedger Tx is ${ledgerTransaction.id}")
        ledgerTransaction.outputContractStates.forEachIndexed { i, it ->
            log.info("\n--- [MoveAllTokenRespFlow] OutputState.$i $it")
        }
    }
}
