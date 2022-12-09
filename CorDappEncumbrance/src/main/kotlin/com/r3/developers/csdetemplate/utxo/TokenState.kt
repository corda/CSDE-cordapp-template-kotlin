package com.r3.developers.csdetemplate.utxo

import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey

@BelongsToContract(TokenContract::class)
data class TokenState(val issuer: Party, val owner: Party, val amount: Int) : ContractState {

    override val participants: List<PublicKey>
        get() = listOf(issuer.owningKey, owner.owningKey)
}
