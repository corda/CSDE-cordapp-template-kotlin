package com.r3.developers.csdetemplate.cpk

import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name

// A class to hold the arguments required to start the flow
class MyCpkFlowStartArgs(val otherMember: MemberX500Name)

// A class which will contain a message, It must be marked with @CordaSerializable for Corda
// to be able to send from one virtual node to another.
@CordaSerializable
class CpkMessage(val sender: MemberX500Name, val message: String)
