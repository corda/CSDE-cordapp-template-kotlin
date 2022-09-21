package com.r3.developers.experiments

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.messaging.FlowMessaging
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Signature

/*
requestBody
{
  "clientRequestId": "r1",
  "flowClassName": "com.r3.developers.experiments.SignedClassFlow",
  "requestData": { "responder":"CN=Bob, OU=Test Dept, O=R3, L=London, C=GB" }
}
*/


@InitiatingFlow(protocol = "signed-class-flow")
class SignedClassFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var flowMessaging: FlowMessaging

// todo: START HERE: work out why this doesn't work in corda


    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val scfStartFlowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, SCFStartFlowArgs::class.java)
        log.info("MB: mlStartFlowArgs: $scfStartFlowArgs")

        val memberX500Name = MemberX500Name.parse(scfStartFlowArgs.responder)

        val keyPair = genKeys()

        log.info("MB: private key: ${keyPair.private}")
        log.info("MB: public key: ${keyPair.public}")


        val myClassA = MyClassA()
        val myClassAjson = jsonMarshallingService.format(myClassA)
        val myClassABytes = myClassAjson.toByteArray()

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(keyPair.private)
        signature.update(myClassABytes)
        val digitalSignature = signature.sign()

        val signedClass = SignedClass(myClassAjson, Pair(keyPair.public, digitalSignature))
        val session = flowMessaging.initiateFlow(memberX500Name)

        return session.sendAndReceive(String::class.java, signedClass)
    }

    // Note, quasar can't checkpoint the KeyPairGenerator Class, by hiding it in a private function
    // its released from the stack before the checkpoint (error is a nullPointerException in kryo)
    private fun genKeys(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(1024)
        return  generator.genKeyPair()
    }

}

class SCFStartFlowArgs(val responder: String )

@InitiatedBy(protocol = "signed-class-flow")
class SignedClassResponderFlow: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }
    @Suspendable
    override fun call(session: FlowSession) {

        val received = session.receive(SignedClass::class.java)
        val publicKey = received.signatures.first
        val digitalSignature = received.signatures.second
        val myClassABytes = received.json.toByteArray()

        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(publicKey)
        signature.update(myClassABytes)
        val isCorrect = signature.verify(digitalSignature)

        session.send(isCorrect.toString())
    }
}



class MyClassA(
    val myValA: String =  "myValA",
    val embedded: MyClassB = MyClassB())

class MyClassB(
    val myValB: String = "myValB")

class PersistedClass(
    val myClassA: String,
    val myClassJson: String
)
@CordaSerializable
class SignedClass(
    val json: String,
    val signatures: Pair<PublicKey,ByteArray>){

    fun checkSignature(signature: Signature): Boolean {return  true}
    fun deserialize(){}
}




