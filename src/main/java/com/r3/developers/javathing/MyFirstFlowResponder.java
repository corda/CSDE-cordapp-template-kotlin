package com.r3.developers.javathing;

import net.corda.v5.application.flows.CordaInject;
import net.corda.v5.application.flows.InitiatedBy;
import net.corda.v5.application.flows.ResponderFlow;
import net.corda.v5.application.membership.MemberLookup;
import net.corda.v5.application.messaging.FlowSession;
import net.corda.v5.base.annotations.Suspendable;
import net.corda.v5.base.types.MemberX500Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// MyFirstFlowResponder is a responder flow, it's corresponding initiating flow is called MyFirstFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "another-flow")
// Responder flows must inherit from ResponderFlow
class MyFirstFlowResponder implements ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private final Logger log = LoggerFactory.getLogger(MyFirstFlowResponder.class);

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    MemberLookup memberLookup;

    public MyFirstFlowResponder() {}

    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    @Override
    public void call(FlowSession session) {

        // Useful logging to follow what's happening in the console or logs
        log.info("MFF: MyFirstResponderFlow.call() called");

        // Receive the payload and deserialize it into a Message class
        Message receivedMessage = session.receive(Message.class);

        // Log the message as a proxy for performing some useful operation on it.
        log.info("MFF: Message received from " + receivedMessage.sender + ":" + receivedMessage.message);

        // Get our identity from the MemberLookup service.
        MemberX500Name ourIdentity = memberLookup.myInfo().getName();

        // Create a response to greet the sender
        Message response = new Message(ourIdentity,
                "Hello " + session.getCounterparty().getCommonName() + ", best wishes from " + ourIdentity.getCommonName());

        // Log the response to be sent.
        log.info("MFF: response.message: " + response.message);

        // Send the response via the send method on the flow session
        session.send(response);
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.javathing.MyFirstFlow",
    "requestData": {
        "otherMember":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */