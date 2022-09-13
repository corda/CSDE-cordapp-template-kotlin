package com.r3.messagebounce


import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class MessageSenderTests {
    private val nodeAX500 = MemberX500Name.parse("CN=Node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=Node B, OU=Test Dept, O=R3, L=London, C=GB")

    // Integration Test
    @Test
    fun `Should return the correct message using actual responder flow`() {
        val corda = Simulator()

        // Create virtual nodes
        val nodeA = corda.createVirtualNode(
            HoldingIdentity.create(nodeAX500),
            MessageSender::class.java
        )
        corda.createVirtualNode(
            HoldingIdentity.create(nodeBX500),
            MessageReturner::class.java
        )

        val message = "here's my message"

        // Invoke (start) a flow on NodeA and collect flow response
        val response = nodeA.callFlow(
            RequestData.create(
                "r1",
                MessageSender::class.java,
                StartRPCFlowArgs(
                    nodeBX500.toString(),
                    message
                )
            )
        )

        // Check the returned data with the expected data (which will be JSON)
        assertThat(response, `is`("{\"message\":\"Responder returned: here's my message\"}"))
    }
}
