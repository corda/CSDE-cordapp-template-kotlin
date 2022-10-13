package com.r3.developers.javathing;

/*
import net.corda.simulator.HoldingIdentity;
import net.corda.simulator.RequestData;
import net.corda.simulator.SimulatedVirtualNode;
import net.corda.simulator.Simulator;
import net.corda.v5.base.types.MemberX500Name;
import org.junit.jupiter.api.Test;

class MyFirstFlowTest {

    // Names picked to match the corda network in config/dev-net.json
    private MemberX500Name aliceX500 = MemberX500Name.parse("CN=Alice, OU=Test Dept, O=R3, L=London, C=GB");
    private MemberX500Name bobX500 = MemberX500Name.parse("CN=Bob, OU=Test Dept, O=R3, L=London, C=GB");

    @Test
    public void test_that_MyFirstFLow_returns_correct_message() {

        // Instantiate an instance of the Simulator
        Simulator simulator = new Simulator();

        // Create Alice's and Bob HoldingIDs
        HoldingIdentity aliceHoldingID = HoldingIdentity.Companion.create(aliceX500);
        HoldingIdentity bobHoldingID = HoldingIdentity.Companion.create(bobX500);

        // Create Alice and Bob's virtual nodes, including the Class's of the flows which will be registered on each node.
        // We don't assign Bob's virtual node to a val because we don't need it for this particular test.
        SimulatedVirtualNode aliceVN = simulator.createVirtualNode(aliceHoldingID, MyFirstFlow.class);
        simulator.createVirtualNode(bobHoldingID, MyFirstFlowResponder.class);

        // Create an instance of the MyFirstFlowStartArgs which contains the request arguments for starting the flow
        MyFirstFlowStartArgs myFirstFlowStartArgs = new MyFirstFlowStartArgs(bobX500);

        // Create a requestData object
        RequestData requestData = RequestData.Companion.create(
                "request no 1",        // A unique reference for the instance of the flow request
                MyFirstFlow.class,        // The name of the flow class which is to be started
                myFirstFlowStartArgs            // The object which contains the start arguments of the flow
        );

        // Call the Flow on Alice's virtual node and capture the response from the flow
        String flowResponse = aliceVN.callFlow(requestData);

        // Check that the flow has returned the expected string
        assert(flowResponse.equals("Hello Alice, best wishes from Bob"));
    }
}
*/
