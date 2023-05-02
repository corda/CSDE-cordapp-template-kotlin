package net.cordapp.utxo.apples.states

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.membership.MemberInfo
import net.cordapp.utxo.apples.contracts.AppleStampContract
import java.security.PublicKey
import java.util.UUID


//The stamp identifier (id)
//The stamp description (stampDesc)
//The issuer of the stamp (issuer)
//The current owner of the stamp (holder)

@BelongsToContract(AppleStampContract::class)
@CordaSerializable
class AppleStamp(
    val id: UUID,
    val stampDesc: String,
    val issuer: MemberInfo,
    val holder: MemberInfo,
    private val participants: List<PublicKey>
) : ContractState {
    override fun getParticipants(): MutableList<PublicKey> {
        return mutableListOf(issuer.ledgerKeys.first(), holder.ledgerKeys.first())
    }
}