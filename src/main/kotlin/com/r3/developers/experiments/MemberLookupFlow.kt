package com.r3.developers.experiments

import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger


/*
requestBody
{
  "clientRequestId": "r101",
  "flowClassName": "com.r3.developers.experiments.MemberLookupFlow",
  "requestData":  { "member": "CN=Bob, OU=Test Dept, O=R3, L=London, C=GB" }
 }


 */

@InitiatingFlow(protocol = "member-lookup-flow")
class MemberLookupFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var memberLookup:  MemberLookup

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val mlStartFlowArgs = requestBody.getRequestBodyAs(jsonMarshallingService, MLStartFlowArgs::class.java)
        log.info("MB: mlStartFlowArgs: $mlStartFlowArgs")

        val memberX500Name = MemberX500Name.parse(mlStartFlowArgs.member)

        val member = memberLookup.lookup(memberX500Name)

        return member!!.name.toString()
    }
}

class MLStartFlowArgs(val member: String)

@InitiatedBy(protocol = "member-lookup-flow")
class MemberLookupResponderFlow: ResponderFlow {

    private companion object {
        val log = contextLogger()
    }

    @Suspendable
    override fun call(session: FlowSession) {

        log.info("MB: MemberLookupResponderFlow.call() called")

    }



}