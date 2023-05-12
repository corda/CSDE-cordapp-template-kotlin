package com.r3.developers.apples.contracts

import net.corda.v5.ledger.utxo.Command

// Used to indicate the transaction's intent
interface AppleCommands : Command {
    class Issue : AppleCommands
    class Redeem : AppleCommands
    class PackBasket : AppleCommands
}
