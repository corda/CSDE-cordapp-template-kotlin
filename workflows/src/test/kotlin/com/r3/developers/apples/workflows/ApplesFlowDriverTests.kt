package com.r3.developers.apples.workflows

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.r3.developers.MemberX500NameDeserializer
import com.r3.developers.MemberX500NameSerializer
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverNodes
import net.corda.testing.driver.runFlow
import net.corda.v5.base.types.MemberX500Name
import net.corda.virtualnode.VirtualNodeInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.fail
import org.slf4j.LoggerFactory
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApplesFlowDriverTests {

    private val logger = LoggerFactory.getLogger(ApplesFlowDriverTests::class.java)
    private val alice = MemberX500Name.parse("CN=Alice, OU=Application, O=R3, L=London, C=GB")
    private val bob = MemberX500Name.parse("CN=Bob, OU=Application, O=R3, L=London, C=GB")
    private val notary = MemberX500Name.parse("CN=Notary, OU=Application, O=R3, L=London, C=GB")
    private val jsonMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())

        val module = SimpleModule().apply {
            addSerializer(MemberX500Name::class.java, MemberX500NameSerializer)
            addDeserializer(MemberX500Name::class.java, MemberX500NameDeserializer)
        }
        registerModule(module)
    }

    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val driver = DriverNodes(alice, bob).withNotary(notary, 1).forAllTests()

    private lateinit var vNodes: Map<MemberX500Name, VirtualNodeInfo>

    @BeforeAll
    fun setup() {
        vNodes = driver.let { dsl ->
            dsl.startNodes(setOf(alice, bob))
            dsl.nodesFor("workflows")
        }
        assertThat(vNodes).withFailMessage("Failed to populate vNodes").isNotEmpty()
    }

    @Test
    fun `test that CreateAndIssueAppleStampFlow returns correct message`() {
        val stampId = createAndIssueAppleStamp("Stamp # 0001", bob, alice)
        logger.info("result: {}", stampId)
        assertThat(stampId).isInstanceOf(UUID::class.java)
    }

    @Test
    fun `test that PackageApplesFlow is successful`() {
        packageApples("Basket of apples # 0001", 100, alice)
        // flow has no return value to assert, if no exceptions are thrown test is successful
    }

    @Test
    fun `test that RedeemApplesFlow is successful`() {
        val stampId = createAndIssueAppleStamp("Stamp # 0002", bob, alice)!!
        packageApples("Basket of apples # 0002", 350, alice)
        val redeemApplesFlowArgs = RedeemApplesFlow.RedeemApplesRequest(bob, stampId)
        driver.run { dsl ->
            dsl.runFlow<RedeemApplesFlow>(vNodes[alice] ?: fail("Missing vNode for Alice")) {
                jsonMapper.writeValueAsString(redeemApplesFlowArgs)
            }
        }
        // flow has no return value to assert, if no exceptions are thrown test is successful
    }

    private fun packageApples(description: String, weight: Int, packer: MemberX500Name) {
        val packageApplesFlowArgs = PackageApplesFlow.PackApplesRequest(description, weight)
        driver.run { dsl: DriverDSL ->
            dsl.runFlow<PackageApplesFlow>(vNodes[packer] ?: fail(String.format("Missing vNode {}", jsonMapper.writeValueAsString(packer)))) {
                jsonMapper.writeValueAsString(packageApplesFlowArgs)
            }
        }
    }

    private fun createAndIssueAppleStamp(description: String, member: MemberX500Name, issuer: MemberX500Name): UUID? {
        val createAndIssueFlowArgs = CreateAndIssueAppleStampFlow.CreateAndIssueAppleStampRequest(description, member)
        val result = driver.let<String> { dsl ->
            dsl.runFlow<CreateAndIssueAppleStampFlow>(
                vNodes[issuer] ?: fail(String.format("Missing vNode {}", jsonMapper.writeValueAsString(issuer)))
            ) {
                jsonMapper.writeValueAsString(createAndIssueFlowArgs)
            }
        }
        return UUID.fromString(result)
    }
}