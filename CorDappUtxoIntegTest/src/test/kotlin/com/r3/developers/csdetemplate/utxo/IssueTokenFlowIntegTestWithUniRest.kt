package com.r3.developers.csdetemplate.utxo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.r3.developers.csdetemplate.utxo.dto.TokenIssueRequest
import kong.unirest.Unirest
import net.corda.flow.rpcops.v1.types.request.StartFlowParameters
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponse
import net.corda.flow.rpcops.v1.types.response.FlowStatusResponses
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.util.*
import java.util.concurrent.TimeUnit

internal class IssueTokenFlowIntegTestWithUniRest {

    lateinit var cnNodes: Map<String, String>
    lateinit var x500Nodes: Map<String, String>

    companion object {
        lateinit var mapper: ObjectMapper

        @BeforeAll
        @JvmStatic
        fun init() {
            Unirest.config()
                .verifySsl(false)
                .setDefaultBasicAuth("admin", "admin")

            mapper = jacksonObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    @Test
    fun getVNodesTest() {
        val response = Unirest
            .get("https://localhost:8888/api/v1/virtualnode")
            .asString()

        assertEquals(HttpURLConnection.HTTP_OK, response.status)

        val responseBody = response.body
        assertNotNull(responseBody)

        val vNodes = mapper.readValue(responseBody, VirtualNodes::class.java)
        assertEquals(5, vNodes.virtualNodes.size)

        cnNodes =
            vNodes.virtualNodes.associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName!! to it.holdingIdentity.shortHash }
        assertEquals(vNodes.virtualNodes.size, cnNodes.size)
        x500Nodes =
            vNodes.virtualNodes.associate { it.holdingIdentity.shortHash to it.holdingIdentity.x500Name }
    }

    @Test
    fun postFlowTest() {
        if (!::cnNodes.isInitialized) {
            getVNodesTest()
        }
        val aliceHash = cnNodes["Alice"]!!
        val bobHash = cnNodes["Bob"]!!

        val tokenIssueRequest = TokenIssueRequest(1, 1, x500Nodes[bobHash]!!)
        val startFlowParameters: StartFlowParameters = StartFlowParameters(
            "issue-${UUID.randomUUID()}",
            "com.r3.developers.csdetemplate.utxo.IssueTokenFlow",
            tokenIssueRequest
        )

        println(mapper.writeValueAsString(startFlowParameters))
        val response = Unirest
            .post("https://localhost:8888/api/v1/flow/$aliceHash")
            .body(mapper.writeValueAsString(startFlowParameters))
            .asString()

        assertEquals(HttpURLConnection.HTTP_ACCEPTED, response.status)

        val responseBody = response.body
        assertNotNull(responseBody)

        val flowStarted = mapper.readValue(responseBody, FlowStatusResponse::class.java)
        println(flowStarted)
        assertEquals(aliceHash, flowStarted.holdingIdentityShortHash)
        assertEquals(startFlowParameters.clientRequestId, flowStarted.clientRequestId)

        TimeUnit.SECONDS.sleep(10)

        val aliceFlow = getFlowByClientId(aliceHash, startFlowParameters.clientRequestId)
        println(aliceFlow)
        assertNotNull(aliceFlow)
    }

    @Test
    fun getFlowsTest() {
        if (!::cnNodes.isInitialized) {
            getVNodesTest()
        }
        val aliceHash = cnNodes["Alice"]

        val response = Unirest
            .get("https://localhost:8888/api/v1/flow/$aliceHash")
            .asString()

        assertEquals(HttpURLConnection.HTTP_OK, response.status)

        val responseBody = response.body
        assertNotNull(responseBody)

        val allFlows = mapper.readValue(responseBody, FlowStatusResponses::class.java)
        assertTrue(allFlows.flowStatusResponses.isNotEmpty())
        allFlows.flowStatusResponses.forEach { println(it) }
    }

    private fun getFlowByClientId(hash: String, id: String): FlowStatusResponse {
        val response = Unirest
            .get("https://localhost:8888/api/v1/flow/$hash/$id")
            .asString()

        assertEquals(HttpURLConnection.HTTP_OK, response.status)

        val responseBody = response.body
        assertNotNull(responseBody)

        val flow = mapper.readValue(responseBody, FlowStatusResponse::class.java)
        return flow;
    }
}