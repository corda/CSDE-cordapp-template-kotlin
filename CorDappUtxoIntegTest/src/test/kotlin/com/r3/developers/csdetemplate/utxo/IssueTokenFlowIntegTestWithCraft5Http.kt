package com.r3.developers.csdetemplate.utxo

import com.r3.developers.csdetemplate.utxo.service.craft5.C5RestService.Companion.setupConnection
import net.corda.craft5.annotations.TestSuite
import net.corda.craft5.http.Http
import net.corda.craft5.http.util.json
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

//TODO: ask: a "/CorDappUtxoIntegTest/junit_report/TEST-junit-jupiter.xml" is created. How to configure it's location?
@TestSuite
internal class IssueTokenFlowIntegTestWithCraft5Http {

    companion object {
        lateinit var http: Http

        @BeforeAll
        @JvmStatic
        fun init() {
            setupConnection(http)
        }
    }

    @Test
    fun getVNodesTest() {
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