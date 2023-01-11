package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.service.FlowService
import com.r3.developers.csdetemplate.utxo.service.VNodeService
import net.corda.libs.virtualnode.types.HoldingIdentity
import org.junit.jupiter.api.Assertions.*
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
}