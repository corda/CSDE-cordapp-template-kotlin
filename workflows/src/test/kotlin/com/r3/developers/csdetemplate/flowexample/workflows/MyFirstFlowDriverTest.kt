package com.r3.developers.csdetemplate.flowexample.workflows

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.r3.developers.MemberX500NameDeserializer
import com.r3.developers.MemberX500NameSerializer
import net.corda.testing.driver.DriverNodes
import net.corda.testing.driver.EachTestDriver
import net.corda.testing.driver.runFlow
import net.corda.v5.base.types.MemberX500Name
import net.corda.virtualnode.VirtualNodeInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MyFirstFlowDriverTest {

    /**
     * Step 1.
     * Declare member identities needed for the tests
     * As well as any other data you want to share across tests
     */

    private val alice = MemberX500Name.parse("CN=Alice, OU=Application, O=R3, L=London, C=GB")
    private val bob = MemberX500Name.parse("CN=Bob, OU=Application, O=R3, L=London, C=GB")
    private val vNodes = mutableMapOf<MemberX500Name, VirtualNodeInfo>()
    private val jsonMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())

        val module = SimpleModule().apply {
            addSerializer(MemberX500Name::class.java, MemberX500NameSerializer)
            addDeserializer(MemberX500Name::class.java, MemberX500NameDeserializer)
        }
        registerModule(module)
    }

    /**
     * Step 2.
     * Declare a test driver
     * Choose between an [EachTestDriver] which will create a fresh instance for each test. Use this if you are worried about tests clashing.
     * Or an [AllTestsDriver] which will only be created once, and reused for all tests. Use this when tests can co-exist as the tests will run faster.
     */

    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val driver = DriverNodes(alice, bob).forEachTest()

    /**
     * Step 3.
     * Start the nodes
     */

    @BeforeEach
    fun setup() {
        driver.run { dsl ->
            dsl.startNodes(setOf(alice, bob))
                .filter { it.cpiIdentifier.name == "workflows" }
                .associateByTo(vNodes) { it.holdingIdentity.x500Name }
        }
    }

    /**
     * Step 4.
     * Write some tests.
     * The FlowDriver runs your flows, and returns the output result for you to assert on.
     */

    @Test
    fun `test that MyFirstFlow returns correct message`() {
        val result = driver.let { dsl ->
            // Run the flow, using the initiating flow class, from Alice to Bob
            dsl.runFlow<MyFirstFlow>(vNodes[alice] ?: fail("Missing vNode for Alice")) {
                // serialise request body as JSON in a string
                jsonMapper.writeValueAsString(MyFirstFlowStartArgs(bob))
            }
        } ?: fail("result should not be null")

        // Assert the flow response is the expected message
        assertThat(result).isEqualTo("Hello Alice, best wishes from Bob")
    }
}
