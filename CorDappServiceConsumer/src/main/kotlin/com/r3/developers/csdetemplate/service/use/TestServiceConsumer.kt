package com.r3.developers.csdetemplate.service.use

import aQute.bnd.annotation.spi.ServiceConsumer
import com.r3.developers.csdetemplate.service.api.TestService
import org.osgi.framework.FrameworkUtil
import java.util.*

class TestServiceConsumer {

    @ServiceConsumer(value = TestService::class)
    companion object {

        fun getAllImpls(viaFrameworkUtils: Boolean): List<TestService> {
            if (!viaFrameworkUtils) {
                return ServiceLoader.load(TestService::class.java, this::class.java.classLoader).toList()
            }

            return FrameworkUtil.getBundle(this::class.java).bundleContext
                .bundles
                .mapNotNull { bundle ->
                    bundle.registeredServices?.let {
                        bundle to it
                    }
                }
                .map { pair ->
                    pair.second.mapNotNull {
                        val classes = it.properties["objectClass"] as Array<String>
                        if (classes.contains(TestService::class.java.name))
                            it
                        else null
                    }
                }
                .flatten()
                .mapNotNull {
                    it.bundle.loadClass(it.properties[".org.apache.aries.spifly.provider.implclass"] as String)
                }
                .map {
                    it.newInstance() as TestService
                }
                .toList()
        }

        /**
         * Gets the [TestService] via jvm [ServiceLoader]
         */
        private val testService: TestService by lazy {
            // This has to be lazy initialized or a function rather than a value due to initialization order.
            getAllImpls(true).firstOrNull() ?: throw NullPointerException("Could not get ${TestService::class.java}")
        }

        fun testMe(s: String): String {
            return testService.testMe(s)
        }
    }
}
