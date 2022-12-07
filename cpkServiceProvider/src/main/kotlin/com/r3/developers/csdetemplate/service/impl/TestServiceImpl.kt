package com.r3.developers.csdetemplate.service.impl

import aQute.bnd.annotation.spi.ServiceProvider
import com.r3.developers.csdetemplate.service.api.TestService

@ServiceProvider(value = TestService::class)
class TestServiceImpl : TestService {

    override fun testMe(s: String): String {
        val result = "---[testMe $s]---"
        println(result)
        return result
    }
}
