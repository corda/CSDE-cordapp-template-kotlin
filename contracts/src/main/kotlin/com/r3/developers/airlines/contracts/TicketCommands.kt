package com.r3.developers.airlines.contracts

import net.corda.v5.ledger.utxo.Command

interface TicketCommands : Command{
    class IssueMoney : TicketCommands
    class Spend : TicketCommands
    class IssueTicket : TicketCommands
    class Transact : TicketCommands
}