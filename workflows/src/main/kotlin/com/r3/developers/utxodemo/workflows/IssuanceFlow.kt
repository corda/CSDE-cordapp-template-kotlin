package com.r3.developers.utxodemo.workflows

import com.r3.developers.utxodemo.contracts.TokenContract
import com.r3.developers.utxodemo.states.TokenState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.ledger.utxo.transaction.UtxoTransactionValidator
import net.corda.v5.ledger.utxo.transaction.getInputStates
import net.corda.v5.ledger.utxo.transaction.getOutputStates
import java.time.Instant
import java.time.temporal.ChronoUnit

class TokenIssuanceFlowStartArgs(
    val amount: Int,
    val owner: MemberX500Name
)


@InitiatingFlow(protocol = "my-issuance-flow")
class IssuanceFlow : RPCStartableFlow {
    private companion object {
        val log = contextLogger()
    }

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging provides a service for establishing flow sessions between Virtual Nodes and
    // sending and receiving payloads between them
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    // When a flow is invoked its call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services
    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val header = "[${IssuanceFlow::class.simpleName}]"

        log.info("$header Flow starts with requestBody: ${requestBody.getRequestBody()}")

        // Get a notary.
        //
        val notaryInfo = notaryLookup.notaryServices.single() // CORE-6173
//        val notaryInfo = notaryLookup
//            .lookup(MemberX500Name("NotaryRep1", "Test Dept", "R3", "GB"))
//            ?: throw java.lang.IllegalArgumentException("Cannot find the given notary")

        val notaryPubKey = memberLookup.lookup().first {
            it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
        }.ledgerKeys.first()
        val notaryParty = Party(notaryInfo.name, notaryPubKey)

        // Deserialize the Json requestBody.
        //
        log.info("$header Get request arguments")
        val requestArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TokenIssuanceFlowStartArgs::class.java)

        // Get owner party.
        //
        log.info("$header Get owner party")
        val ownerMember = memberLookup.lookup(requestArgs.owner)
            ?: throw IllegalArgumentException("Owner \"${requestArgs.owner}\" not found.")
        val ownerParty = Party(ownerMember.name, ownerMember.sessionInitiationKey)

        // Get issuer party.
        //
        log.info("$header Get issuer party")
        val issuerMember = memberLookup.myInfo()
        val issuerParty = Party(issuerMember.name, issuerMember.sessionInitiationKey)

        // Create an output state
        //
        log.info("$header Create an output state")
        val outputState = TokenState(
            issuer = issuerParty,
            owner = ownerParty,
            amount = requestArgs.amount,
            info = "some token meta data",
            participants = listOf(issuerParty.owningKey, ownerParty.owningKey)
        )

        // Create transaction builder.
        //
        log.info("$header Create a transaction builder")
        val txBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(notaryParty)
            .addOutputState(outputState)
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(issuerParty.owningKey))
            .addCommand(TokenContract.Commands.Issue())

        log.info("$header Run txBuilder.toSignedTransaction()")
        // NOTE: not implemented yet with RC03
//        val signedTxn: UtxoSignedTransaction = txBuilder.toSignedTransaction()
        @Suppress("DEPRECATION")
        val signedTxn: UtxoSignedTransaction = txBuilder.toSignedTransaction(issuerParty.owningKey) // Basically, me

        log.info("$header Initiate flow: ${issuerMember.name}")
        val session = flowMessaging.initiateFlow(issuerMember.name)

        log.info("$header Start finalisation")
        val finalisedTxn = utxoLedgerService.finalize(
            signedTransaction = signedTxn,
            sessions = listOf(session)
        )

        finalisedTxn.outputStateAndRefs.map { it.state.contractState }.forEachIndexed { i, contractState ->
            log.info("$header $i, $contractState")
        }

        // The return value of a RPCStartableFlow must always be a String, this string will be passed
        // back as the REST RPC response when the status of the flow is queried on Corda, or as the return
        // value from the flow when testing using the Simulator
        return "$header txn ID: ${finalisedTxn.id}"
    }
}

// IssuanceFLowResponder is a responder flow, it's corresponding initiating flow called IssuanceFlow (defined above).
// To link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "my-issuance-flow")
// Responder flows must inherit from ResponderFlow
class IssuanceResponderFlow : ResponderFlow, UtxoTransactionValidator {
    private companion object {
        val log = contextLogger()
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked its call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {
        val header = "[${IssuanceResponderFlow::class.simpleName}]"

        // Useful logging to follow what's happening in the console or logs
        log.info("$header IssuanceResponderFlow starts.")

        utxoLedgerService.receiveFinality(session, validator = this)
    }

    override fun checkTransaction(ledgerTransaction: UtxoLedgerTransaction) {
        val outputStates = ledgerTransaction.getOutputStates<TokenState>()
        val inputStates = ledgerTransaction.getInputStates<TokenState>()

        require(outputStates.isNotEmpty()) { "There must be an output state!" }
        require(inputStates.isEmpty()) { "It's issuance, there must be no input state!" }
    }
}