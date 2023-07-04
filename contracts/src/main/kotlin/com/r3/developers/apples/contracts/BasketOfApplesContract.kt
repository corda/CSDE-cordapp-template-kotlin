package com.r3.developers.apples.contracts

import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.Contract
import net.corda.v5.ledger.utxo.transaction.UtxoLedgerTransaction
import java.lang.IllegalArgumentException

class BasketOfApplesContract : Contract {
    override fun verify(transaction: UtxoLedgerTransaction) {
        when (val command = transaction.commands.first()){
            is AppleCommands.PackBasket -> {
                val output = transaction.getOutputStates(BasketOfApples::class.java).first()
                require(transaction.outputContractStates.size == 1){
                    "This transaction should only output one BasketOfApples state"
                }
                require(output.description.isNotBlank()){
                    "The output BasketOfApples state should have clear description of Apple product"
                }
                require(output.weight > 0){
                    "The output BasketOfApples weight should have non zero weight"
                }
            }
            is AppleCommands.Redeem -> {
                require(transaction.inputContractStates.size == 2){
                    "This transaction should consume two states"
                }
                val stampInputs = transaction.getInputStates(AppleStamp::class.java)
                val basketInputs = transaction.getInputStates(BasketOfApples::class.java)
                require(stampInputs.isNotEmpty() && basketInputs.isNotEmpty()){
                    "This transaction should have exactly one AppleStamp and one BasketOfApples input state"
                }
                require(stampInputs.single().issuer == basketInputs.single().farm){
                    "The issuer of the Apple Stamp should be the producing farm of this basket of apples"
                }
                require(basketInputs.single().weight > 0){
                    "The basket of apple has to weigh more than 0"
                }
            }
            else ->{
                throw IllegalArgumentException("Incorrect type of BasketOfApples commands: ${command::class.java.name}")
            }
        }
    }
}