package com.r3.examples.tests

import com.r3.examples.ConcatFlow
import com.r3.examples.ConcatInputMessage
import net.corda.testutils.CordaSim
import net.corda.testutils.HoldingIdentity
import net.corda.testutils.tools.CordaFlowChecker
import net.corda.testutils.tools.RPCRequestDataWrapper
import net.corda.testutils.tools.ResponderMock
import net.corda.v5.base.types.MemberX500Name
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ConcatFlowTest {

    private val eric = MemberX500Name.parse("CN=Eric Wimp, OU=Bedroom, O=Acacia Road, L=Nutty Town, C=GB")

    // This is superfluous, the corda.upload(...) statement in the following test runs CordaFlowChecker().check(...)
    @Test
    fun `ConcatFlow should be declared correctly`() {
        assertDoesNotThrow { CordaFlowChecker().check(ConcatFlow::class.java) }
    }

    @Test
    fun `ConcatFlow should concatenate the correct string to the input message`() {
        val corda = CordaSim()
        val node = corda.createVirtualNode(HoldingIdentity(eric), ConcatFlow::class.java)
        val messageText = "Suffix here->"
        // This does not work:
        /*
        val dataWrapper: RPCRequestDataWrapper = RPCRequestDataWrapper("r1",
            "${com.r3.examples.ConcatFlow::class.java}",
            "{\"inText\" : \"${messageText}\" }"
        )*/
        val dataWrapper: RPCRequestDataWrapper =
            RPCRequestDataWrapper.fromData("r1",
                ConcatFlow::class.java,
            ConcatInputMessage(messageText))
        println("dataWrapper=${dataWrapper}")
        val response1 = node.callFlow(dataWrapper)

        println("response=$response1")
        assertThat(response1, `is`("{\"outText\":\"${messageText}${ConcatFlow.CONCAT_TEXT}\"}"))
    }
}

