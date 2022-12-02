package com.r3.developers.csdetemplate.flow

import com.r3.developers.csdetemplate.state.DoorCodeChangeRequest
import com.r3.developers.csdetemplate.state.DoorCodeChangeResult
import com.r3.developers.csdetemplate.state.DoorCodeConsensualState
import net.corda.v5.application.crypto.DigitalSignatureAndMetadata
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.consensual.ConsensualLedgerService
import java.security.PublicKey

/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.csdetemplate.flow.DoorCodeChangeFlow",
    "requestData": {
        "newDoorCode": {
            "code": "bad-pass"
        },
        "participants": [
            "CN=Y, OU=Test Dept, O=R3, L=London, C=GB",
            "CN=Z, OU=Test Dept, O=R3, L=London, C=GB"
        ]
    }
}
 */

/**
 * A flow to ensure that everyone living in a building gets the new door code before it's changed.
 */
@InitiatingFlow("door-code")
class DoorCodeChangeFlow : RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var consensualLedgerService: ConsensualLedgerService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("emko in")
        val changeRequest = requestBody.getRequestBodyAs(jsonMarshallingService, DoorCodeChangeRequest::class.java)
        val participants = changeRequest.participants
        val newDoorCode = changeRequest.newDoorCode

        val doorCodeState = DoorCodeConsensualState(newDoorCode, participants.map { getPublicKey(it) })

        val txBuilder = consensualLedgerService.getTransactionBuilder()
        val signedTransaction = txBuilder
            .withStates(doorCodeState)
            .sign(memberLookup.myInfo().ledgerKeys.first())

        val result = consensualLedgerService.finality(signedTransaction, initiateSessions(participants.toList()))

        val output = DoorCodeChangeResult(newDoorCode, result.signatures.map { getMemberFromSignature(it) }.toSet())

        return jsonMarshallingService.format(output)
    }

    @Suspendable
    private fun getMemberFromSignature(signature: DigitalSignatureAndMetadata) =
        memberLookup.lookup(signature.by)?.name ?: error("Member for consensual signature not found")

    @Suspendable
    private fun initiateSessions(participants: List<MemberX500Name>) =
        participants.filterNot { it == memberLookup.myInfo().name }.map { flowMessaging.initiateFlow(it) }

    @Suspendable
    private fun getPublicKey(member: MemberX500Name): PublicKey {
        val memberInfo = memberLookup.lookup(member) ?: error("Member \"$member\" not found")
        return memberInfo.ledgerKeys.firstOrNull() ?: error("Member \"$member\" does not have any ledger keys")
    }
}

@InitiatedBy("door-code")
class DoorCodeChangeResponderFlow : ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var consensualLedgerService: ConsensualLedgerService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("emko out")
        val finalizedSignedTransaction = consensualLedgerService.receiveFinality(session) {
            //emko: API changes ...
//            val doorCodeState = it.states[0] as DoorCodeConsensualState
            log.info("\"${memberLookup.myInfo().name}\" got the new door code n/a with session $session")
        }
        val requiredSignatories = finalizedSignedTransaction.toLedgerTransaction().requiredSignatories
        val actualSignatories = finalizedSignedTransaction.signatures.map { it.by }
        check(requiredSignatories == actualSignatories)
        log.info("Finished responder flow - $finalizedSignedTransaction")
    }
}
