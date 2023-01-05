package com.r3.developers.utxodemo.workflows


import com.r3.developers.utxodemo.contracts.TokenContract
import com.r3.developers.utxodemo.states.TokenState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit

class TokenMoveFlowArgs(
    val owner: MemberX500Name
)

@InitiatingFlow(protocol = "my-move-flow")
class MoveFlow : RPCStartableFlow {
    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    private lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    private lateinit var utxoLedgerService: UtxoLedgerService

    @CordaInject
    private lateinit var notaryLookup: NotaryLookup

    @CordaInject
    private lateinit var flowMessaging: FlowMessaging

    @CordaInject
    private lateinit var memberLookUp: MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val header = "[${MoveFlow::class.java.simpleName}]"
        log.info("$header MoveFlow starts")

        // Get notary info
        log.info("$header Find a notary")
        val notaryInfo = notaryLookup.notaryServices.single()
//        val notaryMemberName = MemberX500Name("NotaryRep1", "Test Dept", "R3", "GB")
//        val notaryInfo = notaryLookup.lookup(notaryMemberName)
//            ?: throw IllegalArgumentException("Cannot find the requested notary: $notaryMemberName")

        // Get notary key
        log.info("$header Get the notary public key")
        val notaryPublicKey = memberLookUp.lookup().first {
            it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
        }.ledgerKeys.first()

        // Get notary party
        val notary = Party(notaryInfo.name, notaryPublicKey)
//        val notary = Party(notaryMemberName, notaryPublicKey) // may be the same as above

        // Get my info
        log.info("$header Get my info and create me Party")
        val myInfo = memberLookUp.myInfo()
        val meParty = Party(myInfo.name, myInfo.sessionInitiationKey)

        // Get unconsumed state belong to me.
        log.info("$header Retrieve all unspent states of type: ${TokenState::class.java}")
        val allUnspentStates = utxoLedgerService.findUnconsumedStatesByType(TokenState::class.java)

        log.info("$header Filter using myInfo.name: ${myInfo.name}")
        val myStateAndRefs = allUnspentStates.filter { it.state.contractState.owner.name == myInfo.name }
        val myStateRefs = myStateAndRefs.map { it.ref }

        log.info("$header Get request argument type of ${TokenMoveFlowArgs::class.java}")
        val requestArgs = requestBody.getRequestBodyAs(jsonMarshallingService, TokenMoveFlowArgs::class.java)

        // Get new owner
        log.info("$header Get new owner party")
        val newOwner = memberLookUp.lookup(requestArgs.owner)
            ?: throw IllegalArgumentException("Cannot find the receiver ${requestArgs.owner}")
        val newOwnerParty = Party(newOwner.name, newOwner.sessionInitiationKey)

        // Build output states
        log.info("$header Generate output states")
        val outputStates = myStateAndRefs
            .map { it.state.contractState }
            .map {
                TokenState(it.issuer, newOwnerParty, it.amount, it.info)
            }

        // Build an utxo builder
        log.info("$header Build UXTO transaction builder")
        val utxoBuilder = utxoLedgerService.getTransactionBuilder()
            .setNotary(notary)
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addCommand(TokenContract.Commands.Transfer())
            .addInputStates(myStateRefs)
            .addOutputStates(outputStates)
            .addSignatories(listOf(meParty.owningKey))

        // Finalise
        //
        log.info("$header Start finalisation")
        @Suppress("DEPRECATION")
        val signedTxn = utxoBuilder.toSignedTransaction(meParty.owningKey)
        val sessions = listOf(flowMessaging.initiateFlow(newOwner.name))
        val finalisedTxn = utxoLedgerService.finalize(signedTxn, sessions)

        finalisedTxn.outputStateAndRefs
            .map { it.state.contractState }
            .forEachIndexed { idx, it -> log.info("Finalised state $idx: $it") }

        return "$header Finalised txn ID: ${finalisedTxn.id}"
    }
}