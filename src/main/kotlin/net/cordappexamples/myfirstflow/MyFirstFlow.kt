package net.cordappexamples.myfirstflow

import net.corda.v5.application.flows.*
import net.corda.v5.application.messaging.FlowSession
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger


class MyFirstFlowStartArgs(val counterparty: MemberX500Name, val message: String)

@InitiatingFlow(protocol = "my-first-flow")
class MyFirstFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }
@Suspendable
    override fun call(requestBody: RPCRequestData): String {

        log.info("MFF: a Log message")


        return "Hello Alice"
    }


}

@InitiatedBy(protocol = "my-first-flow")
class MyFirstFlowResponder: ResponderFlow {

    @Suspendable
    override fun call(session: FlowSession) {

    }
}