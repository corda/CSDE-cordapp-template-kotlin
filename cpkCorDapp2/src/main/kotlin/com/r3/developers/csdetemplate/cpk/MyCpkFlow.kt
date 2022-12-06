package com.r3.developers.csdetemplate.cpk

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger

// MyCpkFlow is an initiating flow, it's corresponding responder flow is called MyCpkFlowResponder (defined below)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatingFlow(protocol = "my-cpk-flow")
// MyCpkFlow should inherit from RPCStartableFlow, which tells Corda it can be started via an RPC call
class MyCpkFlow : RPCStartableFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // Corda has a set of injectable services which are injected into the flow at runtime.
    // Flows declare them with @CordaInjectable, then the flows have access to their services.

    // JsonMarshallingService provides a Service for manipulating json
    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    // FlowMessaging provides a service for establishing flow sessions between Virtual Nodes and
    // sending and receiving payloads between them
    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    // When a flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services
    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        // Useful logging to follow what's happening in the console or logs
        log.info("MCpkF: MyCpkFlow.call() called")

        // Show the requestBody in the logs - this can be used to help establish the format for starting a flow on corda
        log.info("MCpkF: requestBody: ${requestBody.getRequestBody()}")

        // Deserialize the Json requestBody into the MycpkFlowStartArgs class using the JsonSerialisation Service
        val flowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, MyCpkFlowStartArgs::class.java)

        // Obtain the MemberX500Name of counterparty
        val otherMember = flowArgs.otherMember

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create the message payload using the MessageClass we defined.
        val cpkMessage = CpkMessage(otherMember, "Hello from $ourIdentity.")

        // Log the message to be sent.
        log.info("MCpkF: message.message: ${cpkMessage.message}")

        // Start a flow session with the otherMember using the FlowMessaging service
        // The otherMember's Virtual Node will run the corresponding MyCpkFlowResponder responder flow
        val session = flowMessaging.initiateFlow(otherMember)

        // Send the Payload using the send method on the session to the MyCpkFlowResponder Responder flow
        session.send(cpkMessage)

        // Receive a response from the Responder flow
        val response = session.receive(CpkMessage::class.java)

        // The return value of a RPCStartableFlow must always be a String, this string will be passed
        // back as the REST RPC response when the status of the flow is queried on Corda, or as the return
        // value from the flow when testing using the Simulator
        return response.message
    }
}

// MyCpkFlowResponder is a responder flow, it's corresponding initiating flow is called MyCpkFlow (defined above)
// to link the two sides of the flow together they need to have the same protocol.
@InitiatedBy(protocol = "my-cpk-flow")
// Responder flows must inherit from ResponderFlow
class MyCpkFlowResponder : ResponderFlow {

    // It is useful to be able to log messages from the flows for debugging.
    private companion object {
        val log = contextLogger()
    }

    // MemberLookup provides a service for looking up information about members of the Virtual Network which
    // this CorDapp is operating in.
    @CordaInject
    lateinit var memberLookup: MemberLookup

    // Responder flows are invoked when an initiating flow makes a call via a session set up with the Virtual
    // node hosting the Responder flow. When a responder flow is invoked it's call() method is called.
    // call() methods must be marked as @Suspendable, this allows Corda to pause mid-execution to wait
    // for a response from the other flows and services/
    // The Call method has the flow session passed in as a parameter by Corda so the session is available to
    // responder flow code, you don't need to inject the FlowMessaging service.
    @Suspendable
    override fun call(session: FlowSession) {

        // Useful logging to follow what's happening in the console or logs
        log.info("MCpkF: MyCpkResponderFlow.call() called")

        // Receive the payload and deserialize it into a Message class
        val receivedCpkMessage = session.receive(CpkMessage::class.java)

        // Log the message as a proxy for performing some useful operation on it.
        log.info("MCpkF: Message received from ${receivedCpkMessage.sender}: ${receivedCpkMessage.message} ")

        // Get our identity from the MemberLookup service.
        val ourIdentity = memberLookup.myInfo().name

        // Create a response to greet the sender
        val response = CpkMessage(
            ourIdentity,
            "Hello ${session.counterparty.commonName}, best wishes from ${ourIdentity.commonName}"
        )

        // Log the response to be sent.
        log.info("MCpkF: response.message: ${response.message}")

        // Send the response via the send method on the flow session
        session.send(response)
    }
}
/*
RequestBody for triggering the flow via http-rpc:
{
    "clientRequestId": "r2b",
    "flowClassName": "com.r3.developers.csdetemplate.cpk.MyCpkFlow",
    "requestData": {
        "otherMember":"CN=Emma, OU=Test Dept, O=R3, L=London, C=GB"
        }
}
 */