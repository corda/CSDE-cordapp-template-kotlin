package com.r3.hellocorda.tests

import net.corda.testutils.CordaMock
import net.corda.testutils.tools.CordaFlowChecker
import net.corda.testutils.tools.RPCRequest
import net.corda.v5.base.types.MemberX500Name
import com.r3.hellocorda.ConcatFlow
import com.r3.hellocorda.ConcatInputMessage
import com.r3.hellocorda.ConcatOutputMessage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.jetbrains.annotations.TestOnly
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class ConcatFlowTest {

    private val eric = MemberX500Name.parse("CN=Eric Wimp, OU=Bedroom, O=Acacia Road, L=Nutty Town, C=GB")

    // This superflous.
    @Test
    fun `ConcatFlow should be declared correctly`() {
        assertDoesNotThrow { CordaFlowChecker().check(ConcatFlow::class.java) }
    }

    @Test
    fun `ConcatFlow should concatenate the correct string to the input message`() {
        val corda = CordaMock()
        corda.upload(eric, ConcatFlow::class.java)
        val messageText = "Suffix here->"
        // Stongly typed version to recommend.
        val response1 = corda.invoke(eric,
            RPCRequest.fromData("r1",
                ConcatFlow::class.java,
                ConcatInputMessage(messageText)
            )
        )
        /*
        val response2 = corda.invoke(eric,
            RPCRequest.fromData("r1",
                ConcatFlow::class.java,
                ConcatInputMessage(messageText)
            )
        )
         */

        println("response=$response1")
        // assertThat(actual, expected)
        assertThat(response1, `is`("{\"outText\":\"${messageText}${ConcatFlow.CONCAT_TEXT}\"}") )
    }
}
