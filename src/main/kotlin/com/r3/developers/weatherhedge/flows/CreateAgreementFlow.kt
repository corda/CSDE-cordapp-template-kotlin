package com.r3.developers.weatherhedge.flows

import com.r3.developers.weatherhedge.flows.Agreement
import com.r3.developers.weatherhedge.flows.EvaluableExpression
import com.r3.developers.weatherhedge.flows.Payout
import net.corda.v5.application.flows.*
import net.corda.v5.application.marshalling.JsonMarshallingService
import net.corda.v5.application.persistence.PersistenceService
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.base.util.contextLogger


@InitiatingFlow(protocol = "create-protocol")
class CreateAgreementFlow: RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var jsonMarshallingService: JsonMarshallingService

    @CordaInject
    lateinit var persistenceService: PersistenceService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val rpcArgs: CreateAgreementFlowArgs = requestBody.getRequestBodyAs(
            jsonMarshallingService,
            CreateAgreementFlowArgs::class.java)

        val leadParticipant = MemberX500Name.parse(rpcArgs.leadParticipant)
        val secondParticipant = MemberX500Name.parse(rpcArgs.secondParticipant)
        val oracle = MemberX500Name.parse(rpcArgs.oracle)
        val escrow = MemberX500Name.parse(rpcArgs.escrow)

        val agreement =  Agreement(
            leadParticipant,
            secondParticipant,
            oracle,
            escrow,
            rpcArgs.expression,
            rpcArgs.payout
        )

        val fact = Fact(
            "Rain fall in London",
            20,
            "mm",
            "2022-09-28")
        val verifiableFact =  VerifiableFact(fact)

        val agreementWithFact = Agreement(agreement, verifiableFact)

        log.info("MB: agreementWithFact.evaluate: ${agreementWithFact.evaluate()}")
        log.info("MB: agreement.evaluate(verifiableFact): ${agreement.evaluate(verifiableFact)}")

//        persistenceService.persist(agreement)

        return "CreateAgreementFlow finished"
    }
}

@CordaSerializable
data class CreateAgreementFlowArgs(
    val leadParticipant: String,
    val secondParticipant: String,
    val oracle: String,
    val escrow: String,
    val expression: EvaluableExpression,
    val payout: Payout
)


/*

requestBody
{
  "clientRequestId": "r1",
  "flowClassName": "com.weatherhedge.flows.CreateAgreementFlow",
  "requestData": {
    "leadParticipant":"CN=Lead participant, OU=Test Dept, O=R3, L=London, C=GB",
    "secondParticipant":"CN=Second participant, OU=Test Dept, O=R3, L=London, C=GB",
    "oracle":"CN=Oracle B, OU=Test Dept, O=R3, L=London, C=GB",
    "escrow":"CN=Escrow, OU=Test Dept, O=R3, L=London, C=GB",
    "expression":{
      "type":"Rain fall in London",
      "value":30,
      "unit":"mm",
      "date":"2022-09-28",
      "comparator":"LESS_THAN"
    },
    "payout":{
      "leadOnTrue":0,
      "leadOnFalse":10,
      "secondOnTrue":100,
      "secondOnFalse":0
    }
  }
}




 */