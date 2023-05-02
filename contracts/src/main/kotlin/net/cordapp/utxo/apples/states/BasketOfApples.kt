package net.cordapp.utxo.apples.states

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import net.corda.v5.membership.MemberInfo
import net.cordapp.utxo.apples.contracts.BasketOfApplesContract
import java.security.PublicKey

//description - The brand or type of apple. Use type String.
//farm - The origin of the apples. Use type Party.
//owner - The person exchanging the basket of apples for the voucher (Farmer Bob). Use type Party.
//weight - The weight of the basket of apples. Use type int.

@BelongsToContract(BasketOfApplesContract::class)
@CordaSerializable
class BasketOfApples(
    val description: String,
    val farm: MemberInfo,
    val owner: MemberInfo,
    val weight: Int,
    private val participants: List<PublicKey>
) : ContractState {

    fun changeOwner(buyer: MemberInfo): BasketOfApples {
        val participants = listOf(farm.ledgerKeys.first(), buyer.ledgerKeys.first())
        return BasketOfApples(description, farm, buyer, weight, participants)
    }

    override fun getParticipants(): MutableList<PublicKey> {
        TODO("Not yet implemented")
    }
}