package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.OUTPUT_STATE_SHOULD_ONLY_HAVE_TWO_PARTICIPANTS
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.REQUIRE_SINGLE_COMMAND
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UNKNOWN_COMMAND
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Test
import java.util.*

class ChatContractCreateCommand : ContractTest() {

    private val outputChatState = ChatState(
        UUID.randomUUID(),
        "myChatName",
        aliceName,
        "myChatMessage",
        listOf(aliceKey, bobKey)
    )

    @Test
    fun happyPath() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
        }

        assertVerifies(transaction)
    }

    @Test
    fun missingCommand() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
        }

        assertFailsWith(transaction, REQUIRE_SINGLE_COMMAND)
    }

    @Test
    fun shouldNotAcceptUnknownCommand() {
        class MyDummyCommand : Command

        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addCommand(MyDummyCommand())
        }

        assertFailsWith(transaction, UNKNOWN_COMMAND)
    }

    @Test
    fun outputStateOnlyCannotHaveZeroParticipants() {
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
    fun outputStateOnlyCannotHaveOneParticipant() {
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
    fun outputStateOnlyCannotHaveThreeParticipants() {
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
    fun shouldNotIncludeInputState() {
        happyPath()
        val existingState = ledgerService.findUnconsumedStatesByType(ChatState::class.java).first()

        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
        }

        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_NO_INPUT_STATES")
    }

    @Test
    fun shouldNotHaveTwoOutputStates() {
        val transaction = buildTransaction {
            addOutputState(outputChatState)
            addOutputState(outputChatState)
            addCommand(ChatContract.Create())
        }

        assertFailsWith(transaction, "Failed requirement: $CREATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE")
    }
}