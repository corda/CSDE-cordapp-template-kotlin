package com.r3.hellocorda

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.UntrustworthyData
import net.corda.v5.application.messaging.unwrap
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger

@InitiatingFlow("pass-a-message-protocol")
class PassAMessageFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging


    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        log.info("MB: PassAMessageFlow.call() called")


        val startFlowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, StartFlowArgs::class.java)
        val recipient = MemberX500Name.parse(startFlowArgs.recipientX500)

        log.info("MB: $startFlowArgs")

        val session = flowMessaging.initiateFlow(recipient)
        //        session.sendAndReceive(MyMessageDTO::class.java, payload )
        val vNodeResponse : ResponderMsg =
            session.sendAndReceive(
                ResponderMsg::class.java,
                InitiatorMsg(startFlowArgs.message)
            ).unwrap{ it }

        return jsonMarshallingService.format(vNodeResponse)
    }
}

@InitiatedBy("pass-a-message-protocol")
class PassAMessageResponderFlow: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("MB: PassAMessageResponderFlow.call() called")

        val initiatorData = session.receive(InitiatorMsg::class.java).unwrap { it }
        log.info("MB: in Responder: $initiatorData")
        session.send(ResponderMsg("Answers:${initiatorData.message}"))
    }
}

// These "message" classes need to be serializable.
@CordaSerializable
data class StartFlowArgs(val recipientX500: String, val message: String)

@CordaSerializable
data class InitiatorMsg(val message: String)

@CordaSerializable
data class ResponderMsg(val message: String)