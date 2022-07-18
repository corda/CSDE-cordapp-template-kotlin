package com.cbarratt.cordapp.exprs
//package com.mattb.cordapp.helloworld

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
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


        val myMessageDTO = requestBody.getRequestBodyAs(jsonMarshallingService, MyMessageDTO::class.java)
        val recipient = MemberX500Name.parse(myMessageDTO.recipientX500)

        log.info("MB: myMessageDTO in Initiator: $myMessageDTO")

        val session = flowMessaging.initiateFlow(recipient)
        //        session.sendAndReceive(MyMessageDTO::class.java, payload )
        session.send(myMessageDTO)
        return "MB: recipient: $recipient"
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

        val myMessageDTO = session.receive(MyMessageDTO::class.java).unwrap { it }
        log.info("MB: myMessageDTO in Responder: $myMessageDTO")
    }
}
@CordaSerializable
data class MyMessageDTO(val recipientX500: String, val message: String)