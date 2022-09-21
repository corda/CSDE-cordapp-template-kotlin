package com.r3.developers.weatherhedge.flows


import net.corda.v5.base.annotations.CordaSerializable
import net.corda.v5.base.types.MemberX500Name
import java.util.*

class Agreement(
    val leadParticipant: MemberX500Name,
    val secondParticipant: MemberX500Name,
    val oracle: MemberX500Name,
    val escrow: MemberX500Name,

    val expression: EvaluableExpression,
    val payout: Payout,
    val verifiableFact: VerifiableFact? = null,
    val AgreementID: UUID? = UUID.randomUUID()
)
{
    constructor(agreement: Agreement, verifiableFact: VerifiableFact) : this(
        agreement.leadParticipant,
        agreement.secondParticipant,
        agreement.oracle,
        agreement.escrow,
        agreement.expression,
        agreement.payout,
        verifiableFact, // add in verifiable fact
        agreement.AgreementID)

    fun evaluate(): Boolean{
        return verifiableFact?.let { expression.evaluate(it) } ?: throw Exception("No verifiable fact in Agreement")
    }
    fun evaluate(verifiableFact: VerifiableFact): Boolean {
        return expression.evaluate(verifiableFact)
    }

    // todo: Add signature check over verifiable fact

}


// todo: consider this sort of DSL format:
//val expression = { fact: VerifiableFact ->
//    val threshold = 10
//    threshold < fact.value
//}


@CordaSerializable
class EvaluableExpression(
    var type: String,
    var value: Int,
    var unit: String,
    var date: String,
    var comparator: Comparator
) {

    fun evaluate(verifiableFact: VerifiableFact): Boolean{

        val factValue = verifiableFact.fact.value
        val conditionMet =
            when (comparator) {
                Comparator.LESS_THAN -> { factValue < value }
                Comparator.EQUAL_TO -> { factValue == value }
                Comparator.GREATER_THAN -> { factValue > value }
                else -> throw Exception("Bad comparator")
            }

        return verifiableFact.fact.type == type &&
                verifiableFact.fact.unit == unit &&
                verifiableFact.fact.date == date &&
                conditionMet
    }
}

@CordaSerializable
enum class Comparator {
    LESS_THAN,
    EQUAL_TO,
    GREATER_THAN;
}

class Payout(
    val leadOnTrue: Int,
    val leadOnFalse: Int,
    val secondOnTrue: Int,
    val secondOnFalse: Int
)

