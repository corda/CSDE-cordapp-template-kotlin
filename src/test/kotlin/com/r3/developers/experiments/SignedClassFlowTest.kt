package com.r3.experiments

import com.r3.developers.experiments.SCFStartFlowArgs
import com.r3.developers.experiments.SignedClassFlow
import com.r3.developers.experiments.SignedClassResponderFlow
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test


/*
requestBody
{
  "clientRequestId": "r2",
  "flowClassName": "com.r3.experiments.SignedClassFlow",
  "requestData": { "responder":"C=GB, L=London, O=Blueberry" }

}

(need to wait a few seconds to complete)
 */


class SignedClassFlowTest {

    private val nodeAX500 = MemberX500Name.parse("CN=node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=node B, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test SignedClassFlow`(){


        val sim = Simulator()
        val nodeA = sim.createVirtualNode(HoldingIdentity.Companion.create(nodeAX500), SignedClassFlow::class.java)

        sim.createVirtualNode(HoldingIdentity.Companion.create(nodeBX500), SignedClassResponderFlow::class.java)

        val scfStartFlowArgs = SCFStartFlowArgs(nodeBX500.toString())

        val requestData = RequestData.create(
            "r1",
            SignedClassFlow::class.java,
            scfStartFlowArgs)

        val response = nodeA.callFlow( requestData)

        assert(response == "true")
    }
}