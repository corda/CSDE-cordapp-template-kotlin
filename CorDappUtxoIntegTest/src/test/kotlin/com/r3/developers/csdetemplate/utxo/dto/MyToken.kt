package com.r3.developers.csdetemplate.utxo.dto

import net.corda.v5.base.types.MemberX500Name
import java.util.*

data class MyToken(val issuer: MemberX500Name, val owner: MemberX500Name, val amount: Int, val id: UUID)