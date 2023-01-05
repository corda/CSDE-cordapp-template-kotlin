package net.corda.libs.virtualnode.types

data class HoldingIdentity(val x500Name: String, val groupId: String, val shortHash: String, val fullHash: String)
