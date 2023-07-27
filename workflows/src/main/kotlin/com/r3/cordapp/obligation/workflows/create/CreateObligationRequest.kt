package com.r3.cordapp.obligation.workflows.create

import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal

internal data class CreateObligationRequest(
    val creditor: MemberX500Name,
    val amount: BigDecimal,
    val symbol: String,
    val notary: MemberX500Name?,
    val observers: List<MemberX500Name>
)