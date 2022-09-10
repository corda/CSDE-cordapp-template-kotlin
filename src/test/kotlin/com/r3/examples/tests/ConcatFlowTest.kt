package com.r3.examples.tests
/*
import com.r3.examples.ConcatFlow
import com.r3.examples.ConcatInputMessage
import net.corda.simulator.HoldingIdentity
import net.corda.simulator.RequestData
import net.corda.simulator.Simulator
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

class ConcatFlowTest {

    private val eric = MemberX500Name.parse("CN=Eric Wimp, OU=Bedroom, O=Acacia Road, L=Nutty Town, C=GB")

    @Test
    fun `ConcatFlow should concatenate the correct string to the input message`() {
        val corda = Simulator()
        val node = corda.createVirtualNode(HoldingIdentity.create(eric), ConcatFlow::class.java)
        val messageText = "Suffix here->"

        val dataWrapper: RequestData =
            RequestData.create("r1",
                ConcatFlow::class.java,
            ConcatInputMessage(messageText))
        println("dataWrapper=${dataWrapper}")
        val response1 = node.callFlow(dataWrapper)

        println("response=$response1")
        assertThat(response1, `is`("{\"outText\":\"${messageText}${ConcatFlow.CONCAT_TEXT}\"}"))
    }
}

*/