package com.r3.testutils.example

import net.corda.testutils.CordaMock
import net.corda.testutils.tools.RPCRequest
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class RollCallFlowTest {


    private val teacher = MemberX500Name.parse("CN=Ben Stein, OU=Economics, O=Glenbrook North High School, L=Chicago, C=US")
    private val students = listOf("Albers", "Anderson", "Anheiser", "Busch", "Bueller"). map {
        "CN=$it, OU=Economics, O=Glenbrook North High School, L=Chicago, C=US"
    }

    @Test
    fun `should get roll call from multiple recipients`() {
        // Given a RollCallFlow that's been uploaded to Corda
        val corda = CordaMock()
        corda.upload(teacher, RollCallFlow::class.java)

        // and one recipient with the responder flow
        students.forEach { corda.upload(MemberX500Name.parse(it), RollCallResponderFlow::class.java) }

        // When we invoke it in Corda
        val response = corda.invoke(teacher, RPCRequest.fromData(
            "r1",
            RollCallFlow::class.java,
            RollCallInitiationRequest(students)
        ))

        // Then we should get the response back
        assertThat(response, `is`(students.joinToString(System.lineSeparator()) { "$it: Here!" }))
    }
}