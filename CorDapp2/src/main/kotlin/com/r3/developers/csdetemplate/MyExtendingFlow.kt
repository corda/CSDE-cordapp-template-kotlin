package com.r3.developers.csdetemplate

import com.r3.developers.csdetemplate.cpb.CpbMessage
import com.r3.developers.csdetemplate.cpb.MyCpbFlow
import com.r3.developers.csdetemplate.cpb.MyCpbFlowResponder
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger

// MySecondFlow is an initiating flow, it's corresponding responder flow is called MySecondFlowResponder (defined below)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatingFlow(protocol = "my-ex1-flow")
// MySecondFlow should inherit from RPCStartableFlow, which tells Corda it can be started via an RPC call
class MyEx1Flow : MyCpbFlow(), RPCStartableFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // Corda has a set of injectable services which are injected into the flow at runtime.
    // Flows declare them with @CordaInjectable, then the flows have access to their services.

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    override lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging provides a service for establishing flow sessions between Virtual Nodes and
    // sending and receiving payloads between them
    @CordaInject
    override lateinit var flowMessaging: FlowMessaging

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    override lateinit var memberLookup: MemberLookup

    // When a flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services
    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        // Useful logging to follow what's happening in the console or logs
        log.info("MEx1F: MySecondFlow.call() called")

        // Show the requestBody in the logs - this can be used to help establish the format for starting a flow on corda
        log.info("MEx1F: requestBody: ${requestBody.getRequestBody()}")

        return super.call(requestBody)
    }
}

// MySecondFlowResponder is a responder flow, it's corresponding initiating flow is called MySecondFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "my-ex1-flow")
// Responder flows must inherit from ResponderFlow
class MyEx1FlowResponder : MyCpbFlowResponder(), ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    override lateinit var memberLookup: MemberLookup

    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {

        // Useful logging to follow what's happening in the console or logs
        log.info("MEx1F: MySecondResponderFlow.call() called")

        // Receive the payload and deserialize it into a Message class
        val receivedMessage = session.receive(CpbMessage::class.java)

        // Log the message as a proxy for performing some useful operation on it.
        log.info("MEx1F: Message received from ${receivedMessage.sender}: ${receivedMessage.message} ")

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create a response to greet the sender
        val response = Message(
            ourIdentity,
            "Hello ${session.counterparty.commonName}, best wishes from ${ourIdentity.commonName}"
        )

        // Log the response to be sent.
        log.info("MEx1F: response.message: ${response.message}")

        // Send the response via the send method on the flow session
        session.send(response)
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r2e1",
    "flowClassName": "com.r3.developers.csdetemplate.MyEx1Flow",
    "requestData": {
        "otherMember":"CN=Emma, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */