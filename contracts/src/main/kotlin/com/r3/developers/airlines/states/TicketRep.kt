package com.r3.developers.airlines.states

import com.r3.developers.airlines.contracts.TicketContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(TicketContract::class)
class TicketRep (
    val price : Int,
    private val participants : List<PublicKey>
) : ContractState {

    override fun getParticipants(): List<PublicKey>  = participants

}