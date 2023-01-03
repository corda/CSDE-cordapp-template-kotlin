package net.corda.v5.dc

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.security.PublicKey
import java.util.*

@CordaSerializable
@BelongsToContract(TokenDefinitionContract::class)
data class TokenDefinitionState(
    val name: String,
    val symbol: String,
    val decimals: Int,
    val definer: Party,
    val issuer: Party,
    val customPropsMutable: Map<String, String>,
    val customPropsImmutable: Map<String, String>,
    //TOTO: val rules: Set<Rules> ...
    val id: UUID = UUID.randomUUID(),
    val version: Int = 1
) : ContractState {

    override val participants: List<PublicKey>
        get() = listOf(definer.owningKey, issuer.owningKey)
}
