package com.r3.developers.csdetemplate

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class MySecondFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test that MySecondFLow returns correct message`() {

        // Instantiate an instance of the Simulator
        val simulator = Simulator()

        // Create Alice's and Bob HoldingIDs
        val aliceHoldingID = HoldingIdentity.Companion.create(aliceX500)
        val bobHoldingID = HoldingIdentity.Companion.create(bobX500)

        // Create Alice and Bob's virtual nodes, including the Class's of the flows which will be registered on each node.
        // We don't assign Bob's virtual node to a val because we don't need it for this particular test.
        val aliceVN = simulator.createVirtualNode(aliceHoldingID, MySecondFlow::class.java)
        simulator.createVirtualNode(bobHoldingID, MySecondFlowResponder::class.java)

        // Create an instance of the MySecondFlowStartArgs which contains the request arguments for starting the flow
        val mySecondFlowStartArgs = MySecondFlowStartArgs(bobX500)

        // Create a requestData object
        val requestData = RequestData.create(
            "request no 1",        // A unique reference for the instance of the flow request
            MySecondFlow::class.java,        // The name of the flow class which is to be started
            mySecondFlowStartArgs            // The object which contains the start arguments of the flow
        )

        // Call the Flow on Alice's virtual node and capture the response from the flow
        val flowResponse = aliceVN.callFlow(requestData)

        // Check that the flow has returned the expected string
        assert(flowResponse == "Hello Alice, best wishes from Bob")
    }
}