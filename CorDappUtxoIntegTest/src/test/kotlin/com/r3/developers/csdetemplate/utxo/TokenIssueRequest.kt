package com.r3.developers.csdetemplate.utxo

import net.corda.v5.base.types.MemberX500Name

data class TokenIssueRequest(
    val amount: Int,
    val times: Int = 1,
    val owner: MemberX500Name,
    val withEncumbrance: Boolean = false
)