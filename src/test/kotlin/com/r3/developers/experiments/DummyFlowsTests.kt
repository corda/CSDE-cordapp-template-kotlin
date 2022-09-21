package com.r3.experiments


import com.r3.developers.experiments.DummyFlow
import com.r3.developers.experiments.DummyFlowArgs
import com.r3.developers.experiments.DummyPersistenceFlow
import com.r3.developers.experiments.DummyResponderFlow
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator


import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class DummyFlowTests {

    private val nodeAX500 = MemberX500Name.parse("CN=Node A, OU=Test Dept, O=R3, L=London, C=GB")
    private val nodeBX500 = MemberX500Name.parse("CN=Node B, OU=Test Dept, O=R3, L=London, C=GB")

//    @Test
//    fun `dummy test`(){
//
//        val flowClass = DummyFlow::class.java
//        assertDoesNotThrow { CordaFlowChecker().check(flowClass) }
//
//    }

    @Test
    fun `DummyFlow test`(){

        val cordaSim = Simulator()
        // Can we have a version that just takes the MemberX500Name
        val nodeA = cordaSim.createVirtualNode(HoldingIdentity.create(nodeAX500), DummyFlow::class.java )

        val nodeB = cordaSim.createVirtualNode(HoldingIdentity.create(nodeBX500), DummyResponderFlow::class.java)


        // what happens when there are no args to pass, still wants a class
        val response = nodeA.callFlow(
            RequestData.create(
                "r1",
                DummyFlow::class.java,
                DummyFlowArgs(nodeBX500.toString()) // currently a bug that means can't pass MemberX500Name classes into flow
            )
        )
        assert(response == "Hello: Matt")
    }

    @Test
    fun `test DummyPersistenceFlow`(){

        val cordaSim = Simulator()

        val nodeA = cordaSim.createVirtualNode(HoldingIdentity.create(nodeAX500), DummyPersistenceFlow::class.java )

        val response = nodeA.callFlow(
            RequestData.create(
                "r1",
                DummyPersistenceFlow::class.java,
                ""
            )
        )
        assert(response=="dog")
    }
}