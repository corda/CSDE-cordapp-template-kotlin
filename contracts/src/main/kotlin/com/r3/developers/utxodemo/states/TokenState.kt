package com.r3.developers.utxodemo.states

import com.r3.developers.utxodemo.contracts.TokenContract
import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.Party
import net.corda.v5.ledger.utxo.BelongsToContract
import net.corda.v5.ledger.utxo.ContractState
import java.lang.reflect.Member
import java.security.PublicKey


@CordaSerializable
@BelongsToContract(TokenContract::class)
class TokenState(
    val issuer: Party,
    val owner: Party,
    val amount: Int,
    val info: String // a placeholder for token meta data
    , override val participants: List<PublicKey> = listOf(issuer.owningKey, owner.owningKey)
) : ContractState