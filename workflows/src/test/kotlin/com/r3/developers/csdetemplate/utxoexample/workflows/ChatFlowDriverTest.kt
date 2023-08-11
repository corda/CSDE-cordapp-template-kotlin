package com.r3.developers.csdetemplate.utxoexample.workflows

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.r3.developers.MemberX500NameDeserializer
import com.r3.developers.MemberX500NameSerializer
import net.corda.testing.driver.DriverNodes
import net.corda.testing.driver.EachTestDriver
import net.corda.testing.driver.runFlow
import net.corda.v5.base.types.MemberX500Name
import net.corda.virtualnode.VirtualNodeInfo
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.LoggerFactory
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatFlowDriverTest {

    /**
     * Step 1.
     * Declare member identities needed for the tests
     * As well as any other data you want to share across tests
     */

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val alice = MemberX500Name.parse("CN=Alice, OU=Application, O=R3, L=London, C=GB")
    private val bob = MemberX500Name.parse("CN=Bob, OU=Application, O=R3, L=London, C=GB")
    private val notary = MemberX500Name.parse("CN=Notary, OU=Application, O=R3, L=London, C=GB")
    private val vNodes = mutableMapOf<MemberX500Name, VirtualNodeInfo>()
    private val jsonMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())

        val module = SimpleModule().apply {
            addSerializer(MemberX500Name::class.java, MemberX500NameSerializer)
            addDeserializer(MemberX500Name::class.java, MemberX500NameDeserializer)
        }
        registerModule(module)
    }

    // avoid repeating String literals
    private val noBody = ""
    private val missingAliceVNode = "Missing vNode for Alice"
    private val missingBobVNode = "Missing vNode for Bob"
    private val resultShouldNotBeNull = "result should not be null"
    private val startOfTransactionId = "SHA-256D:"

    /**
     * Step 2.
     * Declare a test driver
     * Choose between an [EachTestDriver] which will create a fresh instance for each test. Use this if you are worried about tests clashing.
     * Or an [AllTestsDriver] which will only be created once, and reused for all tests. Use this when tests can co-exist as the tests will run faster.
     */

    @Suppress("JUnitMalformedDeclaration")
    @RegisterExtension
    private val driver = DriverNodes(alice, bob).withNotary(notary, 1).forAllTests()

    /**
     * Step 3.
     * Start the nodes
     */

    @BeforeAll
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
    fun `test that CreateNewChatFlow returns correct message`() {
        val chartFlowArgs = CreateNewChatFlowArgs("myChatName", "Hello Bob, from Alice", bob.toString())

        val result = driver.let { dsl ->
            // Run the flow, using the initiating flow class, from Alice to Bob
            dsl.runFlow<CreateNewChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                // serialise request body as JSON in a string
                jsonMapper.writeValueAsString(chartFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)

        // Assert the flow response is the expected value
        // example of returned value SHA-256D:4A577FF830E12BCA050F70760E4739192F2C561BCB9FB5CAF3B384ECB2DD1AE3
        assertThat(result).contains(startOfTransactionId)
    }

    @Test
    fun `test that listChatsFlow returns correct values`() {
        // Get the current count before we start
        val listOfChatMessages1 = driver.let { dsl ->
            dsl.runFlow<ListChatsFlow>(vNodes[alice] ?: fail(missingAliceVNode)) { noBody }
        } ?: fail(resultShouldNotBeNull)
        val sizeBeforeSendingMessage = jsonMapper.readValue<List<ChatStateResults>>(listOfChatMessages1).size

        // Send a new message, so there is another chat in list of chats
        `test that CreateNewChatFlow returns correct message`()

        // Get the latest count, and assert it has increased
        val listOfChatMessages2 = driver.let { dsl ->
            dsl.runFlow<ListChatsFlow>(vNodes[alice] ?: fail(missingAliceVNode)) { noBody }
        } ?: fail(resultShouldNotBeNull)
        val listAfterSendingMessage: List<ChatStateResults> = jsonMapper.readValue(listOfChatMessages2)
        val sizeAfterSendingMessage = listAfterSendingMessage.size
        assertThat(sizeAfterSendingMessage).isGreaterThan(sizeBeforeSendingMessage)

        // Assert the response contains all the values
        val firstMessageInList = listAfterSendingMessage.first()
        assertThat(firstMessageInList).hasNoNullFieldsOrProperties()
    }

    @Test
    fun `test that UpdateChatFlow returns correct values`() {
        // Send a message
        `test that CreateNewChatFlow returns correct message`()

        // List the messages and retrieve the id
        val listMessagesResult = driver.let { dsl ->
            dsl.runFlow<ListChatsFlow>(vNodes[alice] ?: fail(missingAliceVNode)) { noBody }
        } ?: fail(resultShouldNotBeNull)
        val messageList: List<ChatStateResults> = jsonMapper.readValue(listMessagesResult)
        val firstMessageId = messageList.last().id

        // Update the message
        val expectedMessage = "Updated message"
        val updateChatFlowArgs = UpdateChatFlowArgs(firstMessageId, expectedMessage)
        val updateMessageResult = driver.let { dsl ->
            dsl.runFlow<UpdateChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(updateChatFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)
        assertThat(updateMessageResult).contains(startOfTransactionId)

        // List the message and validate updated message is present
        val listUpdatedMessagesResult = driver.let { dsl ->
            dsl.runFlow<ListChatsFlow>(vNodes[alice] ?: fail(missingAliceVNode)) { noBody }
        } ?: fail(resultShouldNotBeNull)
        val updatedMessageList: List<ChatStateResults> = jsonMapper.readValue(listUpdatedMessagesResult)
        val updatedMessageValue = updatedMessageList.single {
            it.id == firstMessageId
        }.message
        assertThat(updatedMessageValue).isEqualTo(expectedMessage)
    }

    @Test
    fun `test that getChatFlow returns correct values`() {
        // Alice sends a message to Bob
        `test that CreateNewChatFlow returns correct message`()

        // List the messages and retrieve the id
        val listMessagesResult = driver.let { dsl ->
            dsl.runFlow<ListChatsFlow>(vNodes[alice] ?: fail(missingAliceVNode)) { noBody }
        } ?: fail(resultShouldNotBeNull)
        val messageList: List<ChatStateResults> = jsonMapper.readValue(listMessagesResult)
        val firstMessageId = messageList.last().id

        // Get the latest message
        val getChatFlowArgs = GetChatFlowArgs(firstMessageId, 1)
        val gatheredMessageResult1 = driver.let { dsl ->
            dsl.runFlow<GetChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(getChatFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)
        val listOfMessages1: List<MessageAndSender> = jsonMapper.readValue(gatheredMessageResult1)
        assertThat(listOfMessages1.size).isEqualTo(1)
        val message1Values = listOfMessages1.single()
        assertThat(message1Values.messageFrom).isEqualTo(alice.toString())

        // Alice sends an updated message to Bob
        val secondMessageExpectedValue = "Hello Bob, It's Alice again"
        val aliceUpdatedMessageArgs = UpdateChatFlowArgs(firstMessageId, secondMessageExpectedValue)
        driver.run { dsl ->
            dsl.runFlow<UpdateChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(aliceUpdatedMessageArgs)
            }
        }

        // Get the latest message and assert the message content is updated value
        val gatheredMessageResult2 = driver.let { dsl ->
            dsl.runFlow<GetChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(getChatFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)
        val listOfMessages2: List<MessageAndSender> = jsonMapper.readValue(gatheredMessageResult2)
        assertThat(listOfMessages2.size).isEqualTo(1)
        val message2Values = listOfMessages2.single()
        assertThat(message2Values.messageFrom).isEqualTo(alice.toString())
        assertThat(message2Values.message).isEqualTo(secondMessageExpectedValue)

        // Bob sends an update message to Alice
        val thirdMessageExpectedValue = "Hello Alice, I've been busy. Bob"
        val bobUpdatedMessageArgs = UpdateChatFlowArgs(firstMessageId, thirdMessageExpectedValue)
        driver.run { dsl ->
            dsl.runFlow<UpdateChatFlow>(vNodes[bob] ?: fail(missingBobVNode)) {
                jsonMapper.writeValueAsString(bobUpdatedMessageArgs)
            }
        }

        // Get the latest message and assert the message content is updated value
        val gatheredMessageResult3 = driver.let { dsl ->
            dsl.runFlow<GetChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(getChatFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)
        val listOfMessages3: List<MessageAndSender> = jsonMapper.readValue(gatheredMessageResult3)
        assertThat(listOfMessages3.size).isEqualTo(1)
        val message3Values = listOfMessages3.single()
        assertThat(message3Values.messageFrom).isEqualTo(bob.toString())
        assertThat(message3Values.message).isEqualTo(thirdMessageExpectedValue)

        // Get full back-chain
        val getFullChatFlowArgs = GetChatFlowArgs(firstMessageId, 9999)
        val gatheredAllMessagesResult = driver.let { dsl ->
            dsl.runFlow<GetChatFlow>(vNodes[alice] ?: fail(missingAliceVNode)) {
                jsonMapper.writeValueAsString(getFullChatFlowArgs)
            }
        } ?: fail(resultShouldNotBeNull)
        val listOfAllMessages: List<MessageAndSender> = jsonMapper.readValue(gatheredAllMessagesResult)
        logger.info("listOfAllMessages : {}", listOfAllMessages)
        assertThat(listOfAllMessages.size).isEqualTo(3)
    }
}