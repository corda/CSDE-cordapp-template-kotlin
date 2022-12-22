package com.r3.developers.csdetemplate.utxo

import net.corda.v5.application.membership.MemberLookup
import net.corda.v5.ledger.common.NotaryLookup
import net.corda.v5.ledger.common.Party

fun getNotaryParty(notaryLookup: NotaryLookup, memberLookup: MemberLookup): Party {
    val notaryInfo = notaryLookup.notaryServices.single()

    // val notaryKey = notaryInfo.publicKey
    // TODO CORE-6173 use proper notary key
    val notaryKey = memberLookup.lookup().first {
        it.memberProvidedContext["corda.notary.service.name"] == notaryInfo.name.toString()
    }.ledgerKeys.first()

    return Party(notaryInfo.name, notaryKey)
}
