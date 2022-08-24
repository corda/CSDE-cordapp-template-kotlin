package com.r3.messagebounce

import com.r3.examples.ConcatFlow
import com.r3.messagebounce.InitiatorMsg
import com.r3.messagebounce.MessageReturner
import com.r3.messagebounce.MessageSender
import com.r3.messagebounce.ResponderMsg
import com.r3.messagebounce.StartRPCFlowArgs
import net.corda.testutils.CordaSim
import net.corda.testutils.HoldingIdentity
import net.corda.testutils.tools.CordaFlowChecker
import net.corda.testutils.tools.RPCRequestDataWrapper
import net.corda.testutils.tools.ResponderMock
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class MessageSenderTests {
    private val nodeAX500 = MemberX500Name.parse("CN=Node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=Node B, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `Should pass if PassAMessageFlow declared correctly`() {
        val flowClass = MessageSender::class.java
        assertDoesNotThrow { CordaFlowChecker().check(flowClass) }
    }

    // Unit Test
    @Test
    fun `Should get message back with responder prefix `() {
        // Create an instance of the mock cluster
        val corda = CordaSim()
        val responderMock = ResponderMock<InitiatorMsg, ResponderMsg>()

        val message = "here's my message"
        responderMock.whenever(InitiatorMsg(message), listOf(ResponderMsg("Responder returned: $message")))
        val nodeA = corda.createVirtualNode(HoldingIdentity(nodeAX500),
            MessageSender::class.java
        )
        corda.createVirtualNode(
            HoldingIdentity(nodeBX500),
            "pass-a-message-protocol",
            responderMock
        )

        // Invoke (start) a flow on NodeA and collect flow response
        val response = nodeA.callFlow(
            RPCRequestDataWrapper.fromData(
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


    // Integration Test
    @Test
    fun `Should return the correct message using actual responder flow`() {
        val corda = CordaSim()

        // Create virtual nodes
        val nodeA = corda.createVirtualNode(HoldingIdentity(nodeAX500),
            MessageSender::class.java
        )
        corda.createVirtualNode(
            HoldingIdentity(nodeBX500),
            MessageReturner::class.java
        )

        val message = "here's my message"

        // Invoke (start) a flow on NodeA and collect flow response
        val response = nodeA.callFlow(
            RPCRequestDataWrapper.fromData(
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

class MessageReturnerTests {
    @Test
    fun `Return Message should Defined Correctly`() {
        assertDoesNotThrow { CordaFlowChecker().check(MessageReturner::class.java) }
    }
}

