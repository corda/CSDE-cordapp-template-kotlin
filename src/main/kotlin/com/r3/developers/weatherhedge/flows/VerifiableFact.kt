package com.r3.developers.weatherhedge.flows

class VerifiableFact(
    val fact: Fact,
    val signature: String?= null){
}


class Fact(
    val type: String,
    val value: Int,
    val unit: String,
    val date: String
)