package com.r3.developers.csdetemplate.utxoexample.workflows

import com.r3.developers.csdetemplate.utxoexample.states.ChatState
import net.corda.v5.application.flows.CordaInject
import net.corda.v5.application.flows.RPCRequestData
import net.corda.v5.application.flows.RPCStartableFlow
import net.corda.v5.base.annotations.Suspendable
import net.corda.v5.base.util.contextLogger
import net.corda.v5.ledger.utxo.UtxoLedgerService


class ListChatsFlow : RPCStartableFlow {

    private companion object {
        val log = contextLogger()
    }

    @CordaInject
    lateinit var ledgerService: UtxoLedgerService

    @Suspendable
    override fun call(requestBody: RPCRequestData): String {

        val states = ledgerService.findUnconsumedStatesByType(ChatState::class.java)

        return states.map { "Id: ${it.state.contractState.id} " +
                "last message: ${it.state.contractState.messages.last()}" }.toString()

    }

    // todo: START HERE - return message and Id
    // todo: consider recording just the next message and doing back chain resolution for the history


}