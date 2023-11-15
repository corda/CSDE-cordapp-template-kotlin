package com.r3.developers.testapp

import net.corda.v5.base.annotations.CordaSerializable
import java.util.UUID
import javax.persistence.*

@CordaSerializable
@Entity
data class DraftTx(
    @Id
    @Column
    val id: UUID,

    @Column
    val tx: ByteArray?
) {
    constructor() : this(UUID.randomUUID(), null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DraftTx

        if (id != other.id) return false
        if (!tx.contentEquals(other.tx)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tx.contentHashCode()
        return result
    }
}