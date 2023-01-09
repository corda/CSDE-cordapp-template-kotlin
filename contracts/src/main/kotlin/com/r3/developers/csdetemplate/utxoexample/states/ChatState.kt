package com.r3.developers.csdetemplate.utxoexample.states

import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(ChatContract::class)
data class ChatState(
    val id : UUID = UUID.randomUUID(),
    val chatName: String,
    val messages: List<String> = listOf(),
    override val participants: List<PublicKey>) : ContractState {

    fun updateMessage(message: String) = copy(messages = messages + message)
}

// todo: consider recording just the next message and doing back chain resolution for the history
// GetChatFlow could take the number of historic messages to retrieve

//todo: simplify to one flow which creates if no id or updates if id given (error if id given that's not found

// todo: need to record who made the update to the message