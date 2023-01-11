package com.r3.developers.csdetemplate.utxo.service

import com.r3.developers.csdetemplate.utxo.TokenIssueRequest
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponse

class TokenFlowService {

    companion object {
        private const val IssueTokenFlowName = "com.r3.developers.csdetemplate.utxo.IssueTokenFlow"
        private const val ListMyTokenFlow = "com.r3.developers.csdetemplate.utxo.ListMyTokenFlow"

        fun issueToken(
            holdingIdentityShortHash: String,
            ownerX500Name: String,
            amount: Int,
            times: Int = 1,
            withEncumbrance: Boolean = false
        ): FlowStatusResponse {
            val tokenIssueRequest = TokenIssueRequest(amount, times, ownerX500Name, withEncumbrance)
            val issueTokenFlow: FlowStatusResponse =
                FlowService.startFlow(holdingIdentityShortHash, IssueTokenFlowName, tokenIssueRequest)

            //TODO: can have any "default" actions/checks here
            println(issueTokenFlow)
            //TODO: depending on the flow response design, we can build an object out of it

            return issueTokenFlow;
        }

        fun listMyTokens(
            holdingIdentityShortHash: String
        ): FlowStatusResponse {
            val listMyTokensFlow: FlowStatusResponse =
                FlowService.startFlow(holdingIdentityShortHash, ListMyTokenFlow, null)

            //TODO: can have any "default" actions/checks here
            println(listMyTokensFlow)
            //TODO: depending on the flow response design, we can build an object out of it

            return listMyTokensFlow;
        }
    }
}
