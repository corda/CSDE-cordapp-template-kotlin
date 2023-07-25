package com.r3.developers.apples.contracts

import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Test

class BasketOfApplesContractPackBasketCommandTest : ApplesContractTest() {

    @Test
    fun happyPath() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.PackBasket())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertVerifies(transaction)
    }

    @Test
    fun outputContractStateSizeNotOne() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.PackBasket())
            addOutputState(outputBasketOfApplesState)
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "This transaction should only output one BasketOfApples state")
    }

    @Test
    fun blankDescription() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.PackBasket())
            addOutputState(
                BasketOfApples(
                    "",
                    outputBasketOfApplesStateFarm,
                    outputBasketOfApplesStateOwner,
                    outputBasketOfApplesStateWeight,
                    outputBasketOfApplesStateParticipants
                )
            )
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "The output BasketOfApples state should have clear description of Apple product")
    }

    @Test
    fun basketWeightIsZero() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.PackBasket())
            addOutputState(
                BasketOfApples(
                    outputBasketOfApplesStateDescription,
                    outputBasketOfApplesStateFarm,
                    outputBasketOfApplesStateOwner,
                    0,
                    outputBasketOfApplesStateParticipants
                )
            )
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "The output BasketOfApples state should have non zero weight")
    }

    @Test
    fun basketWeightIsNegative() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.PackBasket())
            addOutputState(
                BasketOfApples(
                    outputBasketOfApplesStateDescription,
                    outputBasketOfApplesStateFarm,
                    outputBasketOfApplesStateOwner,
                    -1,
                    outputBasketOfApplesStateParticipants
                )
            )
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "The output BasketOfApples state should have non zero weight")
    }

    @Test
    fun missingCommand() {
        val transaction = buildTransaction {
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "List is empty.")
    }

    @Test
    fun unknownCommand() {
        class MyDummyCommand : Command

        val transaction = buildTransaction {
            addCommand(MyDummyCommand())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(
            transaction,
            "Incorrect type of BasketOfApples commands: com.r3.developers.apples.contracts." +
                    "BasketOfApplesContractPackBasketCommandTest\$unknownCommand\$MyDummyCommand"
        )
    }
}