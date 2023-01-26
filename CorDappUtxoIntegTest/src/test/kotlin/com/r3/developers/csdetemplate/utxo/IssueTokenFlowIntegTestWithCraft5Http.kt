package com.r3.developers.csdetemplate.utxo

import net.corda.craft5.annotations.TestSuite
import net.corda.craft5.http.Headers
import net.corda.craft5.http.Http
import net.corda.craft5.http.util.json
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection
import java.net.URI

//TODO: ask: a "/CorDappUtxoIntegTest/junit_report/TEST-junit-jupiter.xml" is created. How to configure it's location?
@TestSuite
internal class IssueTokenFlowIntegTestWithCraft5Http {

    @Test
    fun getVNodesTest(http: Http) {
        //TODO: ask: how can this be done in a more general/static/@BeforeAll manner
        http.baseUri = URI("https://localhost:8888/api/v1/")
        http.baseHeaders = mapOf(
            Headers.basicAuthorization("admin", "admin")
        )

        http.get("virtualnode")
        assertEquals(HttpURLConnection.HTTP_OK, http.response().status())

        val responseBody = http.response().body()
        assertNotNull(responseBody)

        //TODO: ask: for means to customize the Mapper
        val vNodes = http.response().json<VirtualNodes>()
        assertEquals(5, vNodes.virtualNodes.size)

        val cnNodes =
            vNodes.virtualNodes.associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName to it.holdingIdentity.shortHash }
        assertEquals(vNodes.virtualNodes.size, cnNodes.size)
    }
}