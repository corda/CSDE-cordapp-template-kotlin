package com.r3.developers.airlines.states

import com.r3.developers.airlines.contracts.MoneyContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.UUID

@BelongsToContract(MoneyContract::class)
class Money (
    val id : UUID,
    val issuer : PublicKey,
    val holder : PublicKey,
    val value : Int,
    private val participants : List<PublicKey>
) : ContractState {

    override fun getParticipants(): List<PublicKey> = participants

    fun changeOwner(buyer: PublicKey) : Money{
        val participants = listOf(issuer,buyer)
        return Money(id,issuer,buyer,value,participants)
    }

    fun checkOwner(user : PublicKey) : Boolean {
        return (holder == user)
    }

    fun returnValue():Int{
        return value
    }
}