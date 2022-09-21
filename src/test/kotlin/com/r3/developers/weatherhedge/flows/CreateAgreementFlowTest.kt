package com.r3.developers.weatherhedge.flows

import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.simulator.factories.JsonMarshallingServiceFactory


import net.corda.v5.base.types.MemberX500Name
import org.junit.jupiter.api.Test

class CreateAgreementFlowTest {

    private val leadX500 = MemberX500Name.parse("CN=Lead participant, OU=Test Dept, O=R3, L=London, C=GB")
    private val secondX500 = MemberX500Name.parse("CN=Second participant, OU=Test Dept, O=R3, L=London, C=GB")
    private val oracleX500 = MemberX500Name.parse("CN=Oracle B, OU=Test Dept, O=R3, L=London, C=GB")
    private val escrowX500 = MemberX500Name.parse("CN=Escrow, OU=Test Dept, O=R3, L=London, C=GB")


    @Test
    fun `test CreateAgreementFlow`(){

        val sim = Simulator()
        val nodeA = sim.createVirtualNode(HoldingIdentity.create(leadX500), CreateAgreementFlow::class.java)

        val testJson = "{\"someval\":\"some string\",\"args\":{\"leadParticipant\":\"1\",\"secondParticipant\":\"2\",\"oracle\":\"3\",\"escrow\":\"4\",\"testJson\":\"5\"}}"

        val jms = JsonMarshallingServiceFactory.create()

        val expression = EvaluableExpression(
            "Rain fall in London",
            30,
            "mm",
            "2022-09-28",
            Comparator.LESS_THAN
        )

        val payout = Payout(0, 10, 100, 0)

        val args = CreateAgreementFlowArgs(
            leadX500.toString(),
            secondX500.toString(),
            oracleX500.toString(),
            escrowX500.toString(),
            expression,
            payout)

        val x = jms.format(args)


        val requestData = RequestData.create(
            "r1",
            CreateAgreementFlow::class.java,
            args)

        val response = nodeA.callFlow( requestData)


        assert(response == "CreateAgreementFlow finished")
    }


}