package com.r3.hellocorda.tests

import com.r3.hellocorda.InitiatorMsg
import com.r3.hellocorda.MessageReturner
import com.r3.hellocorda.MessageSender
import com.r3.hellocorda.ResponderMsg
import com.r3.hellocorda.StartRPCFlowArgs
import net.corda.testutils.CordaMock
import net.corda.testutils.tools.CordaFlowChecker
import net.corda.testutils.tools.RPCRequestDataMock
import net.corda.testutils.tools.ResponderMock
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class MessageSenderTests {
    private val nodeA = MemberX500Name.parse("CN=Node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeB = MemberX500Name.parse("CN=Node B, OU=Test Dept, O=R3, L=London, C=GB")

    // Not strictly necessary as corda.upload(...) will carry out the same checks as
    // CordaFlowChecker().check(...)
    @Test
    fun `Should pass if PassAMessageFlow declared correctly`() {
        val flowClass = MessageSender::class.java
        assertDoesNotThrow { CordaFlowChecker().check(flowClass) }
    }

    // Unit Test
    @Test
    fun `Should get message back with responder prefix `() {
        // Create an instance of the mock cluster
        val corda = CordaMock()
        val responderMock = ResponderMock<InitiatorMsg, ResponderMsg>()

        // Create virtual node
        corda.upload(nodeA, MessageSender::class.java)

        val message = "here's my message"
        responderMock.whenever(InitiatorMsg(message), listOf(ResponderMsg("Responder returned: $message")))
        corda.upload(nodeB, "pass-a-message-protocol", responderMock)

        // Invoke (start) a flow on NodeA and collect flow response
        val response = corda.invoke(
            nodeA,
            RPCRequestDataMock.fromData(
                "r1",
                MessageSender::class.java,
                StartRPCFlowArgs(
                    nodeB.toString(),
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
        val corda = CordaMock()

        // Liz is this the correct way?
        // Create virtual node
        corda.upload(nodeA, MessageSender::class.java)
        corda.upload(nodeB, MessageReturner::class.java)

        val message = "here's my message"

        // Invoke (start) a flow on NodeA and collect flow response
        val response = corda.invoke(
            nodeA,
            RPCRequestDataMock.fromData(
                "r1",
                MessageSender::class.java,
                StartRPCFlowArgs(
                    nodeB.toString(),
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
