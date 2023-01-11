package com.r3.developers.csdetemplate.utxo.service

import com.r3.developers.csdetemplate.utxo.MyToken
import com.r3.developers.csdetemplate.utxo.MyTokens
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
            println("issueToken > $issueTokenFlow")
            //TODO: depending on the flow response design, we can build an object out of it

            return issueTokenFlow;
        }

        fun listMyTokens(
            holdingIdentityShortHash: String
        ): List<MyToken> {
            var listMyTokensFlow: FlowStatusResponse =
                FlowService.startFlow(holdingIdentityShortHash, ListMyTokenFlow, "")
            listMyTokensFlow = FlowService.waitForFlowCompletion(listMyTokensFlow)

            //TODO: can have any "default" actions/checks here
            println("listMyTokens > $listMyTokensFlow")
            //TODO: depending on the flow response design, we can build an object out of it

            val myTokens = FlowService.mapper.readValue(listMyTokensFlow.flowResult, MyTokens::class.java)

            return myTokens.myTokens;
        }
    }
}
