package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract.Companion.UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.ledger.utxo.StateAndRef
import org.junit.jupiter.api.Test
import java.util.*

class ChatContractUpdateCommandTest : ContractTest() {

    private fun createInitialChatState(): StateAndRef<ChatState> {
        val transaction = buildTransaction {
            addOutputState(ChatContractCreateCommandTest().outputChatState)
            addCommand(ChatContract.Create())
        }
        transaction.toLedgerTransaction()
        return transaction.outputStateAndRefs.first() as StateAndRef<ChatState>
    }

    @Test
    fun happyPath() {
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertVerifies(transaction)
    }

    @Test
    fun shouldNotHaveNoInputState(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE")
    }

    @Test
    fun shouldNotHaveTwoInputStates(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_INPUT_STATE")
    }

    @Test
    fun shouldNotHaveTwoOutputStates(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState.updateMessage(bobName, "bobResponse")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_SHOULD_HAVE_ONLY_ONE_OUTPUT_STATE")
    }

    @Test
    fun idShouldNotChange(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(id = UUID.randomUUID())
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_ID_SHOULD_NOT_CHANGE")
    }

    @Test
    fun chatNameShouldNotChange(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(chatName = "newName")
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_CHATNAME_SHOULD_NOT_CHANGE")
    }

    @Test
    fun participantsShouldNotChange(){
        val existingState = createInitialChatState()
        val updatedOutputChatState = existingState.state.contractState
            .updateMessage(bobName, "bobResponse")
            .copy(participants = listOf(bobKey, charlieKey))
        val transaction = buildTransaction {
            addInputState(existingState.ref)
            addOutputState(updatedOutputChatState)
            addCommand(ChatContract.Update())
        }
        assertFailsWith(transaction, "Failed requirement: $UPDATE_COMMAND_PARTICIPANTS_SHOULD_NOT_CHANGE")
    }
}