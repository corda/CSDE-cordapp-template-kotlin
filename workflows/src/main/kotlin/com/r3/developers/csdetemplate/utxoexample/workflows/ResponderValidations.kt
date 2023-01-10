package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.states.ChatState

fun checkForBannedWords(str: String): Boolean {
    val bannedWords = listOf("banana", "apple", "pear")
    return bannedWords.any { str.contains(it) }
}

fun checkMessageFromMatchesKey(state: ChatState): Boolean{
    // todo: add proper check on messageFrom
    return true
}