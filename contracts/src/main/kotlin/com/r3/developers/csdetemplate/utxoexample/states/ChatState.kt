package com.r3.developers.csdetemplate.utxoexample.states

import com.r3.developers.csdetemplate.utxoexample.contracts.ChatContract
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@BelongsToContract(ChatContract::class)
data class ChatState(
    val id : UUID = UUID.randomUUID(),
    val chatName: String,
    val messageFrom: MemberX500Name,
    val message: String,
    override val participants: List<PublicKey>) : ContractState {

    fun updateMessage(messageFrom: MemberX500Name, message: String) = copy(messageFrom = messageFrom, message = message)
}

