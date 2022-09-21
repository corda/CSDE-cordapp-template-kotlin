package com.r3.developers.experiments

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import java.util.UUID
import javax.persistence.*

/*
requestBody:
{
    "clientRequestId": "r1",
    "flowClassName": "com.r3.developers.experiments.PersistenceFlow",
    "requestData": { "responder":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB" }
}
*/
//@InitiatingFlow(protocol = "persistence-flow")
class PersistenceFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("MB: EmbeddedPersistenceFlow.call() started.")

        val comp = Comp("comp_prop")
        val parent = Parent("parent_prop", comp )

        persistenceService.persist(parent)
        val parentFromDB = persistenceService.findAll(Parent::class.java).execute().first()

        return parentFromDB.comp.comp_prop

    }
}


//// todo: remove unnecessary responder when annotation bug fixed in Simulator - https://r3-cev.atlassian.net/browse/CORE-6827
//@InitiatedBy(protocol = "persistence-flow")
//class PersistenceResponderFlow(): ResponderFlow {
//
//    override fun call(session: FlowSession) {
//
//    }
//}



data class EPFStartFlowArgs(val responder: String)
//
//// todo need to add a MemberX500
//@CordaSerializable
//@Entity
//class Parent(
//    @Id
//    val id: UUID,
//    @Column
//    val parent_prop: String,
//    @OneToOne(cascade = [CascadeType.ALL]) // cascade tells the database to save comp as well
//    val comp: Comp
//){
//    constructor(parent_prop: String, comp: Comp): this(id = UUID.randomUUID(), parent_prop = parent_prop, comp = comp)
//}
//
//@CordaSerializable
//@Entity
//class Comp(
//    @Id
//    val id: UUID,
//    @Column
//    var comp_prop: String
//){
//    constructor(comp_prop: String): this(id = UUID.randomUUID(), comp_prop)
//}

class DummyArgs()