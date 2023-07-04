package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.lang.Exception
import java.lang.IllegalArgumentException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@InitiatingFlow(protocol = "create-and-issue-apple-stamp")
class CreateAndIssueAppleStampFlow : ClientStartableFlow {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var notaryLookup: NotaryLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    private data class CreateAndIssueAppleStampRequest(
        val stampDescription: String,
        val holder: MemberX500Name
    )

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            CreateAndIssueAppleStampRequest::class.java)
        val stampDescription = request.stampDescription
        val holderName = request.holder

        val notaryInfo = notaryLookup.notaryServices.single()

        val issuer = memberLookup.myInfo().ledgerKeys.first()
        val holder = memberLookup.lookup(holderName)
            ?.let {it.ledgerKeys.first()}
            ?: throw IllegalArgumentException("The holder $holderName does not exist within the network")

        val newStamp = AppleStamp(
            id = UUID.randomUUID(),
            stampDesc = stampDescription,
            issuer = issuer,
            holder = holder,
            participants = listOf(issuer,holder)
        )

        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(newStamp)
            .addCommand(AppleCommands.Issue())
            .setTimeWindowUntil(Instant.now().plus(1,ChronoUnit.DAYS))
            .addSignatories(listOf(issuer,holder))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(holderName)
        return try{
            utxoLedgerService.finalize(transaction, listOf(session))
            newStamp.id.toString()
        }catch (e : Exception){
            "Flow failed, message: ${e.message}"
        }
    }

}

