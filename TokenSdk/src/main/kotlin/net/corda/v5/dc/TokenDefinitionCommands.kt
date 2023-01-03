package net.corda.v5.dc

import net.corda.v5.ledger.utxo.Command

interface TokenDefinitionCommands : Command {
    class Create : TokenDefinitionCommands
    class Update : TokenDefinitionCommands
    class Destroy : TokenDefinitionCommands
}