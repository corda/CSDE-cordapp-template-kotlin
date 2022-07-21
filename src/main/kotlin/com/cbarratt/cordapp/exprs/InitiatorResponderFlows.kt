package com.cbarratt.cordapp.exprs


import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.InitiatingFlow
import net.corda.v5.application.flows.InitiatedBy
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.application.flows.ResponderFlow
import net.corda.v5.application.flows.getRequestBodyAs
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.receive
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger


// TODO use a separate object for interflow comms
@CordaSerializable
data class InputMessage(
    val msg: String?,
    val recipient: String

)

data class OutputMessage(
    val outThing1: String
)

@InitiatingFlow("ProtocolX1")
class Foof : RPCStartableFlow {

    private companion object {
        val CONCAT_TEXT = "Foof"
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {
        val args = requestBody.getRequestBodyAs<InputMessage>(jsonMarshallingService)
        log.info("Started Foof ${args}")
        val recipient = MemberX500Name.parse(args.recipient)

        // Create FlowSession object
        // extract an X500 identity from args.
        // initiate the other flow

        log.info("Send to initiator flow  on ${args.recipient}")
        val session = flowMessaging.initiateFlow(recipient)
        session.send(args)
         

        return jsonMarshallingService.format(OutputMessage("${args.msg ?: ""}${Foof.CONCAT_TEXT}"))
    }
}


@InitiatedBy("ProtocolX1")
class Foof2 : ResponderFlow {

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    private companion object {
        val log = contextLogger()
    }

    @Suspendable
    override fun call(session: FlowSession) {
        val msg = session.receive<InputMessage>()
        log.info("Rececived: ${msg}")

    }
}

