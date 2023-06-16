package com.r3.developers.csdetemplate.utxoexample.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.csdetemplate.utxoexample.states.ChatState
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

        assertFailsWith(transaction, "Requires a single command.")
    }
}