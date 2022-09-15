package com.r3.corda.csdetemplate.myfirstflow

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import org.junit.jupiter.api.Test

class MyFirstFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private val aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB")
    private val bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB")

    @Test
    fun `test that MyFirstFLow returns correct message`() {

        // Instantiate an instance of the Simulator
        val simulator = Simulator()

        // Create Alice's and Bob HoldingIDs
        val aliceHoldingID = HoldingIdentity.Companion.create(aliceX500)
        val bobHoldingID = HoldingIdentity.Companion.create(bobX500)

        // Create Alice and Bob's virtual nodes, including the Class's of the flows which will be registered on each node.
        val aliceVN = simulator.createVirtualNode(aliceHoldingID, MyFirstFlow::class.java)
        val bobVN = simulator.createVirtualNode(bobHoldingID, MyFirstFlowResponder::class.java)

        // Create an instance of the MyFirstFlowStartArgs which contains the request arguments for starting the flow
        val myFirstFlowStartArgs = MyFirstFlowStartArgs(bobX500, "Hello Bob")

        // Create a requestData object
        val requestData = RequestData.create(
            "request no 1",        // A unique reference for the instance of the flow request
            MyFirstFlow::class.java,        // The name of the flow class which is to be started
            myFirstFlowStartArgs            // The object which contains the start arguments of the flow
        )

        // Call the Flow on Alice's virtual node and capture the response from the flow
        val flowResponse = aliceVN.callFlow(requestData)

        // Check that the flow has returned the expected string
        assert(flowResponse == "Hello Alice best wishes from Bob")
    }
}