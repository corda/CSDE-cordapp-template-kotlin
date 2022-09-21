package com.r3.experiments

import com.r3.developers.experiments.MLStartFlowArgs
import com.r3.developers.experiments.MemberLookupFlow
import com.r3.developers.experiments.MemberLookupResponderFlow
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class MemberLookupFlowTest {

    private val nodeAX500 = MemberX500Name.parse("CN=node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=node B, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test MemberLookupFlow`(){


        val sim = Simulator()
        val nodeA = sim.createVirtualNode(HoldingIdentity.Companion.create(nodeAX500), MemberLookupFlow::class.java)

        val nodeB = sim.createVirtualNode(HoldingIdentity.Companion.create(nodeBX500), MemberLookupResponderFlow::class.java)

        val mlStartRPCFlowArgs = MLStartFlowArgs(nodeBX500.toString())


        val requestData = RequestData.create(
            "r1",
            MemberLookupFlow::class.java,
            mlStartRPCFlowArgs)

        val response = nodeA.callFlow(requestData)

        assert(response == "CN=node B, OU=Test Dept, O=R3, L=London, C=GB")
    }
}