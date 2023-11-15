package com.r3.developers.testapp.states

import com.r3.developers.testapp.contracts.GenericStateContract
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(GenericStateContract::class)
class GenericState(val issuer: PublicKey, val owner: PublicKey) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return listOf(issuer)
    }
}