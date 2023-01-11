package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.types.MemberX500Name

@Suspendable
fun checkForBannedWords(str: String): Boolean {
    val bannedWords = listOf("banana", "apple", "pear")
    return bannedWords.any { str.contains(it) }
}

@Suspendable
fun checkMessageFromMatchesCounterparty(state: ChatState, otherMember: MemberX500Name): Boolean = state.messageFrom == otherMember