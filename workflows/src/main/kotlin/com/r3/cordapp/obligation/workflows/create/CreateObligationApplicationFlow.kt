package com.r3.cordapp.obligation.workflows.create

import com.r3.cordapp.obligation.contract.Obligation
import net.corda.v5.application.flows.ClientRequestBody
import net.corda.v5.application.flows.ClientStartableFlow
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.FlowEngine
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.utxo.transaction.UtxoSignedTransaction
import net.corda.v5.membership.NotaryInfo

internal object CreateObligationApplicationFlow {

    private const val FLOW_PROTOCOL = "create_obligation"

    @InitiatingFlow(protocol = FLOW_PROTOCOL)
    class Initiator : ClientStartableFlow{

        @CordaInject
        private lateinit var jsonMarshallingService : JsonMarshallingService

        @CordaInject
        private lateinit var flowEngine: FlowEngine

        @CordaInject
        private lateinit var memberLookup: MemberLookup

        @CordaInject
        private lateinit var notaryLookup: NotaryLookup

        @CordaInject
        private lateinit var flowMessaging: FlowMessaging


        @Suspendable
        override fun call(requestBody: ClientRequestBody): String {
            val request = requestBody.getRequestBodyAs(jsonMarshallingService, CreateObligationRequest::class.java)

            val obligation = request.getObligation()
            val notaryInfo = request.getNotaryInfo()
            val sessions = request.getFlowSessions()

            val transaction = flowEngine.subFlow(CreateObligationFlow(obligation, notaryInfo, sessions))
            return jsonMarshallingService.format(transaction.toTransactionResponse())
        }

        @Suspendable
        private fun CreateObligationRequest.getObligation() : Obligation{
            val debtor = memberLookup.myInfo().ledgerKeys.first()
            val creditor = checkNotNull(memberLookup.lookup(creditor)?.ledgerKeys?.first()) {"Unknown Creditor"}

            return Obligation(debtor, creditor, amount, symbol)
        }

        @Suspendable
        private fun CreateObligationRequest.getNotaryInfo() : NotaryInfo{

            return notary?.let { notaryLookup.lookup(it) } ?: notaryLookup.notaryServices.first()

        }

        @Suspendable
        private fun CreateObligationRequest.getFlowSessions() : List<FlowSession>{
            return (observers+creditor).map { flowMessaging.initiateFlow(it) }
        }

        @Suspendable
        private fun UtxoSignedTransaction.toTransactionResponse() : CreateObligationResponse{
            val obligation = toLedgerTransaction().getOutputStates(Obligation::class.java).single()
            val debtor = checkNotNull(memberLookup.lookup(obligation.debtor)?.name) {"Unknown debtor"}
            val creditor = checkNotNull(memberLookup.lookup(obligation.creditor)?.name) {"Unknown creditor"}

            return CreateObligationResponse(debtor, creditor, obligation.amount, obligation.symbol, obligation.id)
        }
    }

    @InitiatedBy(protocol = FLOW_PROTOCOL)
    class Responder : ResponderFlow{

        @CordaInject
        private lateinit var flowEngine: FlowEngine

        @Suspendable
        override fun call(session: FlowSession) {
            flowEngine.subFlow(CreateObligationFlowResponder(session))
        }

    }
}