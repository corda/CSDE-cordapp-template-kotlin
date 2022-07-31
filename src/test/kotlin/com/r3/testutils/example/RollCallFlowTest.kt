package com.r3.testutils.example
/*

// QUARENTEENED - Test failing wait for Liz K to resolve.

import net.corda.testutils.CordaMock
import net.corda.testutils.tools.RPCRequest
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class RollCallFlowTest {

    private val teacher = MemberX500Name.parse("CN=Ben Stein, OU=Economics, O=Glenbrook North High School, L=Chicago, C=US")
    private val students = listOf("Albers", "Anderson", "Anheiser", "Busch", "Bueller").map {
        "CN=$it, OU=Economics, O=Glenbrook North High School, L=Chicago, C=US"
    }

    @Test
    fun `should get roll call from multiple recipients`() {
        // Given a RollCallFlow that's been uploaded to Corda
        val corda = CordaMock()
        corda.upload(teacher, RollCallFlow::class.java)

        // and recipients with the responder flow
        students.forEach { corda.upload(MemberX500Name.parse(it), RollCallResponderFlow::class.java) }

        // and a flow to invoke when someone is absent (they return an empty string)
        // Note: We don't actually need to do the upload, because it's constructed inside the main flow -
        // initialization, checking etc. will have to happen when it's passed to the engine.
        // corda.upload(teacher, AbsenceSubFlow::class.java)

        // and a response (which we do need, but it's exactly the same; Bueller continues to take a day off)
        students.forEach { corda.upload(MemberX500Name.parse(it), AbsenceCallResponderFlow::class.java) }

        // When we invoke it in Corda
        val response = corda.invoke(
            teacher,
            RPCRequest.fromData(
                "r1",
                RollCallFlow::class.java,
                RollCallInitiationRequest(students)
            )
        )

        // Then we should get the response back
        assertThat(
            response,
            `is`(
                """
            BEN STEIN: Albers?
            ALBERS: Here!
            BEN STEIN: Anderson?
            ANDERSON: Here!
            BEN STEIN: Anheiser?
            ANHEISER: Here!
            BEN STEIN: Busch?
            BUSCH: Here!
            BEN STEIN: Bueller?
            BEN STEIN: Bueller?
            BEN STEIN: Bueller?
                """.trimIndent().replace("\n", System.lineSeparator())
            )
        )
    }
}
*/