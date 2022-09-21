package com.r3.developers.experiments

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.application.messaging.receive
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import java.time.Instant
import java.util.*
import javax.persistence.*


@InitiatingFlow(protocol = "test-protocol")
class DummyFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jms: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging


    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("MB: TestFlow.call() called now")

        val dummyFlowArgs: DummyFlowArgs = requestBody.getRequestBodyAs(jms, DummyFlowArgs::class.java)

        val recipient: MemberX500Name = MemberX500Name.parse(dummyFlowArgs.x500Name)

        val session = flowMessaging.initiateFlow(recipient)

        return session.sendAndReceive(String::class.java, "Matt")
    }
}


@CordaSerializable
data class DummyFlowArgs(val x500Name: String)

@InitiatedBy(protocol = "test-protocol")
class DummyResponderFlow: ResponderFlow {

    @Suspendable
    override fun call(session: FlowSession) {

        val message = session.receive<String>()
        session.send("Hello: $message")

    }

}
@InitiatingFlow(protocol = "persistence-protocol")
class DummyPersistenceFlow: RPCStartableFlow {

    @CordaInject
    lateinit var persistenceService: PersistenceService


    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val dogId = UUID.randomUUID()
        val dog = Dog(dogId, "dog", Instant.now(), "none")
        persistenceService.persist(dog)

        val x = persistenceService.findAll(Dog::class.java)

        val results= x.execute()
        println("MB: results: ${results[0]}")
        return results[0].name
    }
}

@CordaSerializable
@Entity
@NamedQueries(
    NamedQuery(name = "Dog.summon", query = "SELECT d FROM Dog d WHERE d.name = :name"),
    NamedQuery(name = "Dog.independent", query = "SELECT d FROM Dog d WHERE d.owner IS NULL"),
    NamedQuery(name = "Dog.summonLike", query = "SELECT d FROM Dog d WHERE d.name LIKE :name ORDER BY d.name"),
    NamedQuery(name = "Dog.all", query = "SELECT d FROM Dog d ORDER BY d.name"),
    NamedQuery(name = "Dog.release", query = "UPDATE Dog SET owner=null")
)
data class Dog(
    @Id
    @Column
    val id: UUID,
    @Column
    val name: String,
    @Column
    val birthdate: Instant,
    @Column
    val owner: String?
) {
    constructor() : this(id = UUID.randomUUID(), name = "", birthdate = Instant.now(), owner = "")
}