package com.r3.developers.experiments

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Embeddable
import javax.persistence.Embedded
import javax.persistence.Entity
import javax.persistence.Id


/*
requestBody
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.experiments.JsonMarshallingFlow",
    "requestData": { "responder":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB" }
}

 */

@InitiatingFlow(protocol = "json-marshalling-flow")
class JsonMarshallingFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var persistenceService: PersistenceService

//    @CordaInject
//    lateinit var serializationService: SerializationService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("MB: EmbeddedPersistenceFlow.call() started.")

        val jmStartFlowArgs = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            JMStartFlowArgs::class.java)

        val responder = MemberX500Name.parse(jmStartFlowArgs.responder)

        val session = flowMessaging.initiateFlow(responder)

        //todo: remove dependency on Parent (in PersistenceFlow

        val comp = Comp("test message")
        val parent = Parent(UUID.randomUUID(),"parent_prop", comp )



        val parentJson = jsonMarshallingService.format(parent)
        log.debug("MB: parentJson: $parentJson")


        val localParent = jsonMarshallingService.parse(parentJson, Parent::class.java)
        log.info("MB: localParent: $localParent")
        log.info("MB: localParent.comp.comp_prop: ${localParent.comp.comp_prop}")



        val returnedParentJson = session.sendAndReceive(String::class.java, parentJson)
        val returnedParent = jsonMarshallingService.parse(returnedParentJson, Parent::class.java)


        log.info("MB: returnedParent: $returnedParent")
        log.info("MB: returnedParent.comp.comp_prop: ${returnedParent.comp.comp_prop}")

        return returnedParent.comp.comp_prop


    }
}

@InitiatedBy(protocol = "json-marshalling-flow")
class JsonMarshallingFlowResponder(): ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(session: FlowSession) {
        log.info("MB: EmbeddedPersistenceFlowResponder.call() started.")

        val receivedParentJson = session.receive(String::class.java)
        val receivedParent = jsonMarshallingService.parse(receivedParentJson, Parent::class.java)

        log.info("MB: receivedParent: $receivedParent")
        log.info("MB: receivedParent.comp.comp_prop: ${receivedParent.comp.comp_prop}")

        val sentParentJson = jsonMarshallingService.format(receivedParent)

        session.send(sentParentJson)

    }


}

data class JMStartFlowArgs(val responder: String)


