package com.r3.developers.airlines.contracts

import net.corda.v5.ledger.utxo.Command

interface MoneyCommands : Command {
    class IssueMoney : MoneyCommands
}