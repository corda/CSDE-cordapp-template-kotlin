package com.r3.cordapp.obligation.contract

import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.math.BigDecimal
import java.security.PublicKey
import java.util.UUID

@BelongsToContract(ObligationContract::class)
data class Obligation (
    val debtor : PublicKey,
    val creditor : PublicKey,
    val amount : BigDecimal,
    val symbol : String,
    val id: UUID = UUID.randomUUID()
) : ContractState {
    override fun getParticipants(): List<PublicKey> {
        return listOf(debtor, creditor).distinct()
    }

    fun settle(settledAmount: BigDecimal) : Obligation{
        return copy(amount=amount-settledAmount)
    }

    internal fun immutableEquals(other: Obligation): Boolean{
        return other.debtor==debtor
                && other.creditor == creditor
                && other.symbol == symbol
                && other.id == id
    }
}