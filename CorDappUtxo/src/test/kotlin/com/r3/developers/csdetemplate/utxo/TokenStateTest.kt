package com.r3.developers.csdetemplate.utxo

import net.corda.v5.base.types.MemberX500Name
import net.corda.v5.ledger.common.Party
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.security.PublicKey

class TokenStateTest {

    private val alice = Party(MemberX500Name("Alice", "London", "GB"), Mockito.mock(PublicKey::class.java))
    private val bob = Party(MemberX500Name("Bob", "London", "GB"), Mockito.mock(PublicKey::class.java))

    @Test
    fun tokenStateHasIssuerOwnerAndAmountParamsOfCorrectTypeInConstructor() {
        TokenState(alice, bob, 1)
    }

    @Test
    fun tokenStateHasGettersForIssuerOwnerAndAmount() {
        val tokenState = TokenState(alice, bob, 1)
        assertEquals(alice, tokenState.issuer)
        assertEquals(bob, tokenState.owner)
        assertEquals(1, tokenState.amount)
    }

    @Test
    fun tokenStateHasTwoParticipantsTheIssuerAndTheOwner() {
        val tokenState = TokenState(alice, bob, 1)
        assertEquals(2, tokenState.participants.size)
        assertTrue(tokenState.participants.contains(alice.owningKey))
        assertTrue(tokenState.participants.contains(bob.owningKey))
    }
}