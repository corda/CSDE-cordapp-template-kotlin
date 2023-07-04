package com.r3.developers.airlines.states

import com.r3.developers.airlines.contracts.TicketContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.UUID

@BelongsToContract(TicketContract::class)
class Ticket (
    val id : UUID,
    val issuer: PublicKey,
    val holder : PublicKey,
    val seat : String,
    val departureDate : String,
    val price : Int,
    private val participants : List<PublicKey>
) : ContractState {

    override fun getParticipants(): List<PublicKey> = participants

    fun changeOwner(buyer: PublicKey) : Ticket{
        val participants = listOf(issuer,buyer)
        return Ticket(id,issuer,buyer,seat,departureDate,price,participants)
    }

    fun checkOwner(buyer : PublicKey) : Boolean {
        return (issuer == buyer)
    }

    fun returnPrice():Int{
        return price
    }

}