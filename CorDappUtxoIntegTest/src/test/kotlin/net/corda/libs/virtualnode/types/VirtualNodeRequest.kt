package net.corda.libs.virtualnode.types

data class VirtualNodeRequest(
    val x500Name: String,
    val cpiFileChecksum: String,
    val vaultDdlConnection: String?,
    val vaultDmlConnection: String?,
    val cryptoDdlConnection: String?,
    val cryptoDmlConnection: String?,
    val uniquenessDdlConnection: String?,
    val uniquenessDmlConnection: String?
)
