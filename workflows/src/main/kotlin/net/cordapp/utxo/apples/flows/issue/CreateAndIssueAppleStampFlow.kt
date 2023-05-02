package net.cordapp.utxo.apples.flows.issue

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import net.cordapp.utxo.apples.states.AppleStamp
import net.cordapp.utxo.apples.contracts.AppleStampContract
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID


//stampDescription - A String description for the AppleStamp.
//holder - A MemberX500Name for the participant who is issued an AppleStamp.
data class CreateAndIssueAppleStampRequest(
    val stampDescription: String,
    val holder: MemberX500Name
)
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

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs<CreateAndIssueAppleStampRequest>(jsonMarshallingService,CreateAndIssueAppleStampRequest::class.java)
        val stampDescription = request.stampDescription
        val holderName = request.holder

        // Retrieve the notaries public key (this will change)
        val notaryInfo = notaryLookup.notaryServices.single()
        val notary= memberLookup.lookup().single {
            it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
        }.ledgerKeys.first()
        //val notary = Party(notaryInfo.name, notaryKey)

        val issuer = memberLookup.myInfo()
        val holder = memberLookup.lookup(holderName)
            ?: throw IllegalArgumentException("The holder $holderName does not exist within the network")

        // Building the output AppleStamp state
        val newStamp = AppleStamp(
            id = UUID.randomUUID(),
            stampDesc = stampDescription,
            issuer = issuer,
            holder = holder,
            participants = listOf(issuer.ledgerKeys.first(), holder.ledgerKeys.first())
        )

        // Create the transaction
        @Suppress("DEPRECATION")
        val transaction = utxoLedgerService.createTransactionBuilder()
            .setNotary(notaryInfo.name)
            .addOutputState(newStamp)
            .addCommand(AppleStampContract.Commands.Issue())
            .setTimeWindowUntil(Instant.now().plus(1, ChronoUnit.DAYS))
            .addSignatories(listOf(issuer.ledgerKeys.first()))
            .toSignedTransaction()

        val session = flowMessaging.initiateFlow(holderName)

        return try {
            // Send the transaction and state to the counterparty and let them sign it
            // Then notarise and record the transaction in both parties' vaults.
            utxoLedgerService.finalize(transaction, listOf(session))
            newStamp.id.toString()
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}

