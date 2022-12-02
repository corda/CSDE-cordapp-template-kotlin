package com.r3.developers.csdetemplate.state

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DoorCodeConsensualStateTest {

    @Test
    fun dummyTest() {
        val code = DoorCode("super-duper-secret")
        val state = DoorCodeConsensualState(code, listOf())
        // ...
        assertTrue(true)
    }
}