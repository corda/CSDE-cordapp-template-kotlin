package net.corda.libs.virtualnode.types

data class VirtualNodeInfo(
    val holdingIdentity: HoldingIdentity,
//    val cpiIdentifier: CpiIdentifier,
//    val vaultDdlConnectionId: String? = null,
//    val vaultDmlConnectionId: String,
//    val cryptoDdlConnectionId: String? = null,
//    val cryptoDmlConnectionId: String,
//    val uniquenessDdlConnectionId: String? = null,
//    val uniquenessDmlConnectionId: String,
//    val hsmConnectionId: String? = null,
    val state: String,
)
