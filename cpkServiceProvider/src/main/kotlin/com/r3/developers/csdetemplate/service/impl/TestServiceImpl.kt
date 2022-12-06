package com.r3.developers.csdetemplate.service.impl

import aQute.bnd.annotation.Resolution
import aQute.bnd.annotation.spi.ServiceProvider
import com.r3.developers.csdetemplate.service.api.TestService

@ServiceProvider(
    value = TestService::class,
    register = TestServiceImpl::class,
    effective = "resolve",
    resolution = Resolution.MANDATORY,
    uses = [TestService::class]
)
class TestServiceImpl : TestService {

    override fun testMe(s: String): String {
        val result = "---[testMe {s}]---"
        println(result)
        return result
    }
}
