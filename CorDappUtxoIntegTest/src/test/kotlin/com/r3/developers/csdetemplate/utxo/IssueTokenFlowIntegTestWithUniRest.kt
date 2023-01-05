package com.r3.developers.csdetemplate.utxo

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kong.unirest.Unirest
import net.corda.libs.virtualnode.types.VirtualNodes
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.net.HttpURLConnection

internal class IssueTokenFlowIntegTestWithUniRest {

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

        val cnNodes =
            vNodes.virtualNodes.associate { MemberX500Name.parse(it.holdingIdentity.x500Name).commonName to it.holdingIdentity.shortHash }
        assertEquals(vNodes.virtualNodes.size, cnNodes.size)
    }
}