package com.r3.developers.csdetemplate.utxo.dto

data class TokenIssueRequest(
    val amount: Int,
    val times: Int = 1,
    val owner: String,
    val withEncumbrance: Boolean = false
)