package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.REQUIRE_SINGLE_COMMAND
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UNKNOWN_COMMAND
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Create
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.crypto.SecureHash
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.*

class ChatContractCreateCommandTest : ContractTest() {

    private val outputChatStateChatName = "aliceChatName"
    private val outputChatStateChatMessage = "aliceChatMessage"
    internal val outputChatState = ChatState(
        UUID.randomUUID(),
        outputChatStateChatName,
        aliceName,
        outputChatStateChatMessage,
        listOf(aliceKey, bobKey)
    )

    @Test
    fun happyPath() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        assertVerifies(transaction)
    }

    @Test
    fun addAttachmentsNotSupported() {
        val secureHash: SecureHash = object : SecureHash {
            override fun getAlgorithm(): String {
                return null.toString()
            }

            override fun toHexString(): String {
                return null.toString();
            }
        }
        val exception: Exception = Assertions.assertThrows(
            UnsupportedOperationException::class.java
        ) {
            val transaction = ledgerService
                .createTransactionBuilder()
                .addAttachment(secureHash)
                .addOutputState(outputChatState)
                .addCommand(Create())
                .addSignatories(outputChatState.participants)
                .toSignedTransaction()
        }
        val expectedMessage = "This method is not implemented for the mock ledger"
        val actualMessage = exception.message
        Assertions.assertTrue(actualMessage!!.contains(expectedMessage))
    }


    @Test
    fun missingCommand() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addSignatories(outputChatState.participants)
        }
        assertFailsWith(transaction, REQUIRE_SINGLE_COMMAND)
    }

    @Test
    fun shouldNotAcceptUnknownCommand() {
        class MyDummyCommand : Command

        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(MyDummyCommand())
            addSignatories(outputChatState.participants)
        }

        assertFailsWith(transaction, UNKNOWN_COMMAND)
    }

    @Test
    fun outputStateCannotHaveZeroParticipants() {
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            emptyList()
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun outputStateCannotHaveOneParticipant() {
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            listOf(aliceKey)
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun outputStateCannotHaveThreeParticipants() {
        val state = ChatState(
            UUID.randomUUID(),
            "myChatName",
            aliceName,
            "myChatMessage",
            listOf(aliceKey, bobKey, charlieKey)
        )
        val transaction = buildTransaction {
            addOutputState(state)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS")
    }

    @Test
    fun shouldBeSigned() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }

    @Test
    fun cannotBeSignedByOnlyOneParticipant() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants[0])
        }
        assertFailsWith(transaction, "Failed requirement: $TRANSACTION_SHOULD_BE_SIGNED_BY_ALL_PARTICIPANTS")
    }

    @Test
    fun shouldNotIncludeInputState() {
        happyPath() // generate an existing state to search for
        val existingState = ledgerService.findUnconsumedStatesByType(ChatState::class.java).first() // doesn't matter which as this will fail validation
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES")
    }

    @Test
    fun shouldNotHaveTwoOutputStates() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
            addSignatories(outputChatState.participants)
        }
        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE")
    }
}