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
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.*
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator
import java.time.Instant
import java.time.temporal.ChronoUnit

@CordaSerializable
data class TokenMoveRequest(val maxIndex: Int = 1, val owner: MemberX500Name)

/*
As "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB":
{
  "clientRequestId": "moveAll_1",
  "flowClassName": "com.r3.developers.csdetemplate.utxo.MoveAllTokenFlow",
  "requestData": {
    "owner": "CN=Charlie, OU=Test Dept, O=R3, L=London, C=GB",
    "maxIndex": 2
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

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("\n--- [MoveAllTokenFlow] Starting...")

        val notaryInfo = notaryLookup.notaryServices.first()
        // val notaryKey = notaryInfo.publicKey
        // TODO CORE-6173 use proper notary key
        val notaryKey = memberLookup.lookup().first {
            it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
        }.ledgerKeys.first()
        val notaryParty = Party(notaryInfo.name, notaryKey)

        val request = requestBody.getRequestBodyAs<TokenMoveRequest>(jsonMarshallingService)
        val ownerMember = memberLookup.lookup(request.owner) ?: throw IllegalArgumentException("Owner not found!")
        val ownerParty = Party(ownerMember.name, ownerMember.sessionInitiationKey)

        // issuer == old/current owner
        val issuerMember = memberLookup.myInfo()
        val issuerParty = Party(issuerMember.name, issuerMember.sessionInitiationKey)

//        try {
//            val emko = utxoLedgerService.findUnconsumedStatesByType(ContractState::class.java)
//            log.info("\n--- [MoveAllTokenFlow] >>> ${emko.size}")
//        } catch (e:Exception) {
//            log.info("\n--- [MoveAllTokenFlow] !!! $e")
//        }

        /*
        // emko:issue#5
            utxoLedgerService.findLedgerTransaction(inputTxHash) ?: throw IllegalArgumentException("Token not found!")
            => java.lang.ClassCastException: class org.apache.avro.generic.GenericData$Array cannot be cast to class net.corda.v5.crypto.DigitalSignature$WithKey (org.apache.avro.generic.GenericData$Array is in unnamed module of loader org.apache.felix.framework.BundleWiringImpl$BundleClassLoader @4e387da1; net.corda.v5.crypto.DigitalSignature$WithKey is in unnamed module of loader org.apache.felix.framework.BundleWiringImpl$BundleClassLoader @527ba75a)
	at net.corda.flow.application.crypto.SigningServiceImpl.sign(SigningServiceImpl.kt:32) ~[?:?]
         */

        val allUnusedTokens = utxoLedgerService.findUnconsumedStatesByType(TokenState::class.java)
        val myUnusedTokens = allUnusedTokens.filter { it.state.contractState.owner.name.equals(issuerMember.name) }

        myUnusedTokens.forEachIndexed { i, it ->
            log.info("\n--- [MoveAllTokenFlow] InputState.$i with index ${it.ref.index} and with Encumbrance.name ${it.state.encumbrance ?: "n/a"}")
        }
        val inputStateAndRefs = myUnusedTokens.filter { it.ref.index < request.maxIndex }
        val inputStateRefs = inputStateAndRefs.map { it.ref }
        val outputTokenStates = inputStateAndRefs
            .map { it.state.contractState }
            .map { TokenState(it.issuer, ownerParty, it.amount) }

        log.info("\n--- [MoveAllTokenFlow] 1")
        val utxoTxBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(notaryParty)
            // a time windows is mandatory
            // emko:issue#3
            // !!! => .setTimeWindowBetween(Instant.MIN, Instant.MAX) => java.lang.ArithmeticException: long overflow
            .setTimeWindowBetween(Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS))
//            .addInputStates(inputStateRefs)
//            .addReferenceInputStates(inputStateRefs)
            .addOutputStates(outputTokenStates)
            .addCommand(MoveAll())
            .addSignatories(listOf(issuerParty.owningKey))

        log.info("\n--- [MoveAllTokenFlow] 2")
        @Suppress("DEPRECATION")
        val signedTx = utxoTxBuilder.toSignedTransaction(issuerParty.owningKey)
        log.info("\n--- [MoveAllTokenFlow] 3")
        val sessions = listOf(flowMessaging.initiateFlow(ownerParty.name))
        log.info("\n--- [MoveAllTokenFlow] 4")
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
        /*
            emko:issue#2
            somewhere in between I have
            => com.r3.corda.notary.plugin.common.NotaryException: Unable to notarise transaction SHA-256D:D106256B648EB997C14C0E24BB7A7F4904800C7EB1CA56CABCD19C2128DD9CD9 : NotaryErrorGeneralImpl(errorText=Error while processing request from client., cause=net.corda.v5.base.exceptions.CordaRuntimeException: java.lang.IllegalStateException: Error while verifying request signature. Cause: net.corda.v5.crypto.exceptions.CryptoSignatureException: Signature Verification failed!)
	at com.r3.corda.notary.plugin.nonvalidating.client.NonValidatingNotaryClientFlowImpl.call(NonValidatingNotaryClientFlowImpl.kt:89) ~[?:?]
         */
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
