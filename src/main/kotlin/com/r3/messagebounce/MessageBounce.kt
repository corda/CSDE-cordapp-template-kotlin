package com.r3.messagebounce

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger


// Request data block for starting a flow, eg usig Swagger UI:
/*
 }
 (
    "clientRequestId": "mbr1",
    "flowClassName": "com.r3.messagebounce.MessageSender",
    "requestData": "{ \"recipientX500\": \"O=Blueberry, L=London, C=GB\", \"message\" : \"Hello from Apricot\" }"
 )
*/

@InitiatingFlow("pass-a-message-protocol")
class MessageSender : RPCStartableFlow {

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

        val startRPCFlowArgs: StartRPCFlowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, StartRPCFlowArgs::class.java)
        val recipient: MemberX500Name = MemberX500Name.parse(startRPCFlowArgs.recipientX500)

        log.info("Args: $startRPCFlowArgs")
        log.info("About to initiate flow to $recipient")
        val session: FlowSession = flowMessaging.initiateFlow(recipient)

        val vNodeResponse: ResponderMsg =
            session.sendAndReceive(
                ResponderMsg::class.java,
                InitiatorMsg(startRPCFlowArgs.message)
            )

        val flowResults = RPCFlowResults(vNodeResponse.message)
        return jsonMarshallingService.format(flowResults)
    }
}

@InitiatedBy("pass-a-message-protocol")
class MessageReturner : ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("MB: ReturnMessage flow started.")

        val initiatorData = session.receive(InitiatorMsg::class.java)
        log.info("MB: in Responder: $initiatorData")
        // session.send(ResponderMsg("Responder returned: ${initiatorData.message}"))
        session.send(ResponderMsg("Responder returned: ${initiatorData.message}"))
    }
}

// These "message" classes need to be serializable.
@CordaSerializable
data class StartRPCFlowArgs(val recipientX500: String, val message: String)

// NOTE: We could just one class for RPCFlowResults, InitiatorMsg & ResponderMsg in this case.
//       More generally a different data type can be used to pass data to a flow to that which
//       is returned by the flow.
@CordaSerializable
data class RPCFlowResults(val message: String)

@CordaSerializable
data class InitiatorMsg(val message: String)

@CordaSerializable
data class ResponderMsg(val message: String)
