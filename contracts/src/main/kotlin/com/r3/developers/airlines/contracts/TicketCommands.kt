package com.r3.developers.airlines.contracts

import net.corda.v5.ledger.utxo.Command

interface TicketCommands : Command{
    class IssueTicket : TicketCommands
    class Transact : TicketCommands
}