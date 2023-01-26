package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.dto.TokenIssueRequest
import com.r3.developers.csdetemplate.utxo.service.unirest.FlowService
import com.r3.developers.csdetemplate.utxo.service.unirest.TokenFlowService
import com.r3.developers.csdetemplate.utxo.service.unirest.VNodeService
import net.corda.libs.virtualnode.types.HoldingIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

internal class IssueTokenFlowIntegTestWithUniRestServices {

    companion object {
        lateinit var vNodes: Map<String, HoldingIdentity>

        @BeforeAll
        @JvmStatic
        fun init() {
            vNodes = VNodeService.listVNodesMap()
        }
    }

    @Test
    fun getVNodesTest() {
        val vNodes = VNodeService.listVNodes()
        assertEquals(5, vNodes.size)
    }

    @Test
    fun postFlowTest() {
        val alice = vNodes["Alice"]!!
        val bob = vNodes["Bob"]!!

        val flowClassName = "com.r3.developers.csdetemplate.utxo.IssueTokenFlow"
        val tokenIssueRequest = TokenIssueRequest(1, 1, bob.x500Name)

        val issueTokenFlow = FlowService.startFlow(alice.shortHash, flowClassName, tokenIssueRequest)
        println(issueTokenFlow)
        assertEquals(alice.shortHash, issueTokenFlow.holdingIdentityShortHash)
        assertNotNull(issueTokenFlow.clientRequestId)

        TimeUnit.SECONDS.sleep(10)//TODO: emko: re-tries

        val aliceFlow = FlowService.getFlow(alice.shortHash, issueTokenFlow.clientRequestId!!)
        assertNotNull(aliceFlow)
        println(aliceFlow)
    }

    @Test
    fun getFlowsTest() {
        val alice = vNodes["Alice"]!!
        val allAliceFlows = FlowService.listFlows(alice.shortHash)
        assertTrue(allAliceFlows.flowStatusResponses.isNotEmpty())
        allAliceFlows.flowStatusResponses.forEach { println(it) }
    }

    @Test
    fun integTest() {
        val alice = vNodes["Alice"]!!
        val bob = vNodes["Bob"]!!

        val initialTokens = TokenFlowService.listMyTokens(bob.shortHash)
        val initialCount = initialTokens.size

        var issueTokenFlow = TokenFlowService.issueToken(alice.shortHash, bob.x500Name, 1, 2)
        println(issueTokenFlow)
        assertEquals(alice.shortHash, issueTokenFlow.holdingIdentityShortHash)
        assertNotNull(issueTokenFlow.clientRequestId)

        issueTokenFlow = FlowService.waitForFlowCompletion(issueTokenFlow)

        val aliceFlow = FlowService.getFlow(alice.shortHash, issueTokenFlow.clientRequestId!!)
        assertNotNull(aliceFlow)
        println(aliceFlow)

        val listMyTokens = TokenFlowService.listMyTokens(bob.shortHash)
        assertEquals(initialCount + 2, listMyTokens.size)
    }
}