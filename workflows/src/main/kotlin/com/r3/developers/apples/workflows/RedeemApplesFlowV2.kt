package com.r3.developers.apples.workflows

import com.r3.developers.apples.contracts.AppleCommands
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.base.types.MemberX500Name
import java.util.*

import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.UtxoLedgerService
import java.time.Instant
import java.time.temporal.ChronoUnit

@InitiatingFlow(protocol = "redeem-apples-2")
class RedeemApplesFlowV2 : ClientStartableFlow {

    private data class RedeemApplesRequest(val stampId: UUID)

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup: MemberLookup

    @CordaInject
    lateinit var utxoLedgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: ClientRequestBody): String {
        val request = requestBody.getRequestBodyAs(jsonMarshallingService, RedeemApplesRequest::class.java)
        val stampId = request.stampId

        val myKey = memberLookup.myInfo().let { it.ledgerKeys.first() }

        val appleStampStateAndRef = utxoLedgerService.findUnconsumedStatesByType(AppleStamp::class.java)
            .firstOrNull { stateAndRef -> stateAndRef.state.contractState.id == stampId }
            ?: throw IllegalArgumentException("No apple stamp matching the stamp id $stampId")

        val issuerName = memberLookup.lookup(appleStampStateAndRef.state.contractState.issuer)
            ?.let { it.name }
            ?: throw IllegalArgumentException("The issuer does not exist within the network")

        val session = flowMessaging.initiateFlow(issuerName)

        return try {
            val initialResponse = session.sendAndReceive(String::class.java, stampId)

            if ( initialResponse == "Redeem responder flow ok so far, continuing" ) {
                utxoLedgerService.receiveFinality(session) { transaction ->
                    // In this case, we want to confirm that we have received a basket of apples and we
                    // are now the owner
                    val outputStates = transaction.getOutputStates(BasketOfApples::class.java)
                    require(outputStates.size == 1) {
                        "There should be exactly one basket of apples returned by the issuer"
                    }
                    require(outputStates.first().owner == myKey) {
                        "The initiator should be the new owner of the basket of apples"
                    }
                }.toString()
            } else {
                "Flow failed, message: $initialResponse"
            }
        } catch (e: Exception) {
            "Flow failed, message: ${e.message}"
        }
    }
}
