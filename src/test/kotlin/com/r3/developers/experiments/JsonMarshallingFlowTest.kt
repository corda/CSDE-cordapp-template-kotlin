package com.r3.experiments


import com.r3.developers.experiments.JMStartFlowArgs
import com.r3.developers.experiments.JsonMarshallingFlow
import com.r3.developers.experiments.JsonMarshallingFlowResponder
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class JsonMarshallingFlowTest {

    private val nodeAX500 = MemberX500Name.parse("CN=node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=node B, OU=Test Dept, O=R3, L=London, C=GB")

    //todo: sort out this test so it doesn't reference persistence

    @Test
    fun `test JsonMarshallingFlow`(){


        val sim = Simulator()
        val nodeA = sim.createVirtualNode(HoldingIdentity.Companion.create(nodeAX500), JsonMarshallingFlow::class.java)

        sim.createVirtualNode(HoldingIdentity.Companion.create(nodeBX500), JsonMarshallingFlowResponder::class.java)

        val jmStartRPCFlowArgs = JMStartFlowArgs(nodeBX500.toString())

        val requestData = RequestData.create(
            "r1",
            JsonMarshallingFlow::class.java, // change this class
            jmStartRPCFlowArgs)

        val response = nodeA.callFlow( requestData)

        assert(response == "test message")
    }
}