package com.r3.experiments

import com.r3.developers.experiments.PersistenceFlow
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class PersistenceFlowTest {

    private val nodeAX500 = MemberX500Name.parse("CN=node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=node B, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test embeddedPersistenceFlow`(){


        val sim = Simulator()
        val nodeA = sim.createVirtualNode(HoldingIdentity.Companion.create(nodeAX500), PersistenceFlow::class.java)

        val requestData = RequestData.create(
            "r1",
            PersistenceFlow::class.java,
            "")

        val response = nodeA.callFlow( requestData)

        assert(response == "comp_prop")
    }
}