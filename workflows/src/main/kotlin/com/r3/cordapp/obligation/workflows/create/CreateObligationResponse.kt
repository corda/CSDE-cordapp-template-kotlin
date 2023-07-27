package com.r3.cordapp.obligation.workflows.create

import net.corda.v5.base.types.MemberX500Name
import java.math.BigDecimal
import java.util.UUID

internal data class CreateObligationResponse(
    val debtor: MemberX500Name,
    val creditor: MemberX500Name,
    val amount: BigDecimal,
    val symbol: String,
    val id: UUID
)