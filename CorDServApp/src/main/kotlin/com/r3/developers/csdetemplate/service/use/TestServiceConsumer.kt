package com.r3.developers.csdetemplate.service.use

import aQute.bnd.annotation.Resolution
import aQute.bnd.annotation.spi.ServiceConsumer
import com.r3.developers.csdetemplate.service.api.TestService
import java.util.*

class TestServiceConsumer {

    @ServiceConsumer(
        value = TestService::class,
        effective = "resolve",
        resolution = Resolution.MANDATORY
    )
    companion object {
        /**
         * Gets the [TestService] via jvm [ServiceLoader]
         */
        private val testService: TestService by lazy {
            // This has to be lazy initialized or a function rather than a value due to initialization order.
            ServiceLoader.load(TestService::class.java, this.javaClass.classLoader).toList().firstOrNull()
                ?: throw NullPointerException("Could not get ${TestService::class.java}")
        }

        fun getAllImpls(): List<TestService> {
            return ServiceLoader.load(TestService::class.java, this.javaClass.classLoader).toList()
        }

        fun testMe(s: String): String {
            return testService.testMe(s)
        }
    }
}
