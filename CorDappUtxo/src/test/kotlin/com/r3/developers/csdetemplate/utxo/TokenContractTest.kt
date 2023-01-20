package com.r3.developers.csdetemplate.utxo

import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.given
import org.mockito.kotlin.mock

class TokenContractTest {

    @Test
    fun givenTxWithNoCommands_whenVerify_then() {
        //given
        val tokenContract = TokenContract()
        val tx = mock<UtxoLedgerTransaction>()
        given(tx.commands).willReturn(emptyList())

        //when
        val ex = assertThrows<IllegalArgumentException> { tokenContract.verify(tx) }

        //then
        assertEquals("Commands must be one, but are 0!", ex.message)
    }
}