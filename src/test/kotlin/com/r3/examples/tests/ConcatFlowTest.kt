package com.r3.examples.tests

import com.r3.examples.ConcatFlow
import com.r3.examples.ConcatInputMessage
import net.corda.testutils.FakeCorda
import net.corda.testutils.tools.CordaFlowChecker
import net.corda.testutils.tools.RPCRequestDataMock
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
        val corda = FakeCorda()
        corda.upload(eric, ConcatFlow::class.java)
        val messageText = "Suffix here->"
        // Stongly typed version to recommend.
        val response1 = corda.invoke(
            eric,
            RPCRequestDataMock.fromData(
                "r1",
                ConcatFlow::class.java,
                ConcatInputMessage(messageText)
            )
        )

        println("response=$response1")
        assertThat(response1, `is`("{\"outText\":\"${messageText}${ConcatFlow.CONCAT_TEXT}\"}"))
    }
}

