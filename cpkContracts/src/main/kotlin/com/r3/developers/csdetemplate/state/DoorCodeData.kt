package com.r3.developers.csdetemplate.state

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name

@CordaSerializable
data class DoorCode(val code: String)

@CordaSerializable
data class DoorCodeChangeRequest(val newDoorCode: DoorCode, val participants: Set<MemberX500Name>)

@CordaSerializable
data class DoorCodeChangeResult(val newDoorCode: DoorCode, val signedBy: Set<MemberX500Name>)
