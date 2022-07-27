package com.r3.hellocorda.tests

import net.corda.testutils.tools.CordaFlowChecker
import com.r3.hellocorda.PassAMessageFlow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class InitiatorTests {

    @Test
    fun `Should pass if PassAMessageFlow declared correctly`() {
        val flowClass = PassAMessageFlow::class.java
        assertDoesNotThrow { CordaFlowChecker().check(flowClass) }
    }

}
