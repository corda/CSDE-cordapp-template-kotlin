package com.r3.developers.csdetemplate.flow

import com.r3.developers.csdetemplate.state.DoorCode
import com.r3.developers.csdetemplate.state.DoorCodeConsensualState
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DoorCodeChangeFlowTest {

    @Test
    fun dummyTest() {
        val code = DoorCode("super-duper-secret")
        val state = DoorCodeConsensualState(code, listOf())
        // ...
        assertTrue(true)
    }
}