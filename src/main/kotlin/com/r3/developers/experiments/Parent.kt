package com.r3.developers.experiments

import net.corda.v5.base.annotations.CordaSerializable
import java.util.*
import javax.persistence.*


// todo need to add a MemberX500
@CordaSerializable
@Entity
class Parent(
    @Id
    val id: UUID,
    @Column
    val parent_prop: String,
    @OneToOne(cascade = [CascadeType.ALL]) // cascade tells the database to save comp as well
    val comp: Comp
){
    constructor(parent_prop: String, comp: Comp): this(id = UUID.randomUUID(), parent_prop = parent_prop, comp = comp)
}

@CordaSerializable
@Entity
class Comp(
    @Id
    val id: UUID,
    @Column
    var comp_prop: String
){
    constructor(comp_prop: String): this(id = UUID.randomUUID(), comp_prop)
}