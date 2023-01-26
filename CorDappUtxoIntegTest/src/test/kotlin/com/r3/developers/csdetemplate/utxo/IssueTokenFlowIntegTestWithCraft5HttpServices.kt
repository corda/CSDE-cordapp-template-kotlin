package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.dto.TokenIssueRequest
import com.r3.developers.csdetemplate.utxo.service.craft5.FlowService
import com.r3.developers.csdetemplate.utxo.service.craft5.TokenFlowService
import com.r3.developers.csdetemplate.utxo.service.craft5.VNodeService
import net.corda.craft5.annotations.TestSuite
import net.corda.craft5.http.Http
import net.corda.craft5.http.HttpImpl
import net.corda.libs.virtualnode.types.HoldingIdentity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

//TODO: ask: a "/CorDappUtxoIntegTest/junit_report/TEST-junit-jupiter.xml" is created. How to configure it's location?
@TestSuite
internal class IssueTokenFlowIntegTestWithCraft5HttpServices {

//    companion object {
//        lateinit var vNodes: Map<String, HoldingIdentity>
//
//        @BeforeAll
//        @JvmStatic
//        fun init() {
//            vNodes = VNodeService.listVNodesMap(HttpImpl())
//        }
//    }
//TODO: ask: the above is causing the flowing! How can I disable craft5 injection?
    /*
    =>
    java.util.Map<java.lang.String, net.corda.libs.virtualnode.types.HoldingIdentity>
    java.lang.ClassNotFoundException: java.util.Map<java.lang.String, net.corda.libs.virtualnode.types.HoldingIdentity>
        at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:581)
        at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:178)
        at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:522)
        at java.base/java.lang.Class.forName0(Native Method)
        at java.base/java.lang.Class.forName(Class.java:315)
        at net.corda.craft5.injection.GenericInjector.findTargetClass(GenericInjector.kt:173)
        at net.corda.craft5.injection.GenericInjector.canInstantiate(GenericInjector.kt:142)
        at net.corda.craft5.injection.GenericInjector.instantiateFieldsInClass(GenericInjector.kt:96)
        at net.corda.craft5.injection.GenericInjector.beforeAll(GenericInjector.kt:64)
        at org.junit.jupiter.engine.descriptor.ClassBasedTestDescriptor.lambda$invokeBeforeAllCallbacks$10(ClassBasedTestDescriptor.java:381)
        ...
     */

    @Test
    //TODO: ask: what else can we get (be provided with) here?
    fun getVNodesTest(http: Http) {
        val vNodes = VNodeService.listVNodes(http)
        assertEquals(5, vNodes.size)
    }

    @Test
    fun postFlowTest(http: Http) {
        val vNodes: Map<String, HoldingIdentity> = VNodeService.listVNodesMap(HttpImpl())
        val alice = vNodes["Alice"]!!
        val bob = vNodes["Bob"]!!

        val flowClassName = "com.r3.developers.csdetemplate.utxo.IssueTokenFlow"
        val tokenIssueRequest = TokenIssueRequest(1, 1, bob.x500Name)

        val issueTokenFlow = FlowService.startFlow(http, alice.shortHash, flowClassName, tokenIssueRequest)
        println(issueTokenFlow)
        assertEquals(alice.shortHash, issueTokenFlow.holdingIdentityShortHash)
        assertNotNull(issueTokenFlow.clientRequestId)

        TimeUnit.SECONDS.sleep(10)//TODO: emko: re-tries

        val aliceFlow = FlowService.getFlow(http, alice.shortHash, issueTokenFlow.clientRequestId!!)
        assertNotNull(aliceFlow)
        println(aliceFlow)
    }

    @Test
    fun getFlowsTest(http: Http) {
        val vNodes: Map<String, HoldingIdentity> = VNodeService.listVNodesMap(HttpImpl())
        val alice = vNodes["Alice"]!!

        val allAliceFlows = FlowService.listFlows(http, alice.shortHash)
        assertTrue(allAliceFlows.flowStatusResponses.isNotEmpty())
        allAliceFlows.flowStatusResponses.forEach { println(it) }
    }

    @Test
    fun integTest(http: Http) {
        val vNodes: Map<String, HoldingIdentity> = VNodeService.listVNodesMap(HttpImpl())
        val alice = vNodes["Alice"]!!
        val bob = vNodes["Bob"]!!

        val initialTokens = TokenFlowService.listMyTokens(http, bob.shortHash)
        val initialCount = initialTokens.size

        var issueTokenFlow = TokenFlowService.issueToken(http, alice.shortHash, bob.x500Name, 1, 2)
        println(issueTokenFlow)
        assertEquals(alice.shortHash, issueTokenFlow.holdingIdentityShortHash)
        assertNotNull(issueTokenFlow.clientRequestId)

        issueTokenFlow = FlowService.waitForFlowCompletion(http, issueTokenFlow)

        val aliceFlow = FlowService.getFlow(http, alice.shortHash, issueTokenFlow.clientRequestId!!)
        assertNotNull(aliceFlow)
        println(aliceFlow)

        val listMyTokens = TokenFlowService.listMyTokens(http, bob.shortHash)
        assertEquals(initialCount + 2, listMyTokens.size)
    }
}