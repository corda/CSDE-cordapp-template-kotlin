package com.r3.testutils.example

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.unwrap
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger

@InitiatingFlow("roll-call")
class RollCallFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        println("${RollCallFlow::class.java.simpleName}.call() called")

        val rollCall = requestBody.getRequestBodyAs(jsonMarshallingService, RollCallInitiationRequest::class.java)


        println("${RollCallFlow::class.java.simpleName} initiating roll call")
        val sessionsAndRecipients = rollCall.recipientsX500.map {
            Pair(flowMessaging.initiateFlow(MemberX500Name.parse(it)), it)
        }

        println("${RollCallFlow::class.java.simpleName} initiated roll call")
        val responses = sessionsAndRecipients.map {
            println("${this.javaClass.simpleName} sending and waiting for receipt")
            it.first.sendAndReceive(
                RollCallResponse::class.java,
                RollCallRequest(it.second)
            )
        }.map { r -> r.unwrap { it } }

        return responses.joinToString(System.lineSeparator()) { it.response }
    }
}

@InitiatedBy("roll-call")
class RollCallResponderFlow: ResponderFlow {

    @CordaInject
    lateinit var flowEngine: FlowEngine

    private companion object {
        val log = contextLogger()
    }

    @Suspendable
    override fun call(session: FlowSession) {
        println("${this.javaClass} waiting to receive call")

        session.receive(RollCallRequest::class.java)

        session.send(RollCallResponse("${flowEngine.virtualNodeName}: Here!"))
        println("${this.javaClass} responded: \"Here!\"")
    }
}

@CordaSerializable
data class RollCallInitiationRequest(val recipientsX500: List<String>)
data class RollCallRequest(val recipientX500: String)
data class RollCallResponse(val response: String)