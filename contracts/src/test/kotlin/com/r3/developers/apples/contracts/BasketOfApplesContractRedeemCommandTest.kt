package com.r3.developers.apples.contracts

import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Test

class BasketOfApplesContractRedeemCommandTest : ApplesContractTest() {
    @Test
    fun happyPath() {
        val inputAppleStampState = createInputStateAppleStamp()
        val inputBasketOfApplesState = createInputStateBasketOfApples()
        val transaction = buildTransaction {
            addInputStates(listOf(inputAppleStampState.ref, inputBasketOfApplesState.ref))
            addCommand(AppleCommands.Redeem())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertVerifies(transaction)
    }

    @Test
    fun inputContractStateSizeNotTwo() {
        val inputAppleStampState = createInputStateAppleStamp()
        val transaction = buildTransaction {
            addInputState(inputAppleStampState.ref)
            addCommand(AppleCommands.Redeem())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "This transaction should consume two states")
    }

    @Test
    fun twoAppleStampStateInputs() {
        val inputAppleStampState = createInputStateAppleStamp()
        val transaction = buildTransaction {
            addInputStates(listOf(inputAppleStampState.ref, inputAppleStampState.ref))
            addCommand(AppleCommands.Redeem())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(
            transaction,
            "This transaction should have exactly one AppleStamp and one BasketOfApples input state"
        )
    }

    @Test
    fun appleStampIssuerDifferentFromBasketFarm() {
        val inputAppleStampState = createInputStateAppleStamp(
            AppleStamp(
                outputAppleStampStateId,
                outputAppleStampStateStampDesc,
                aliceKey,
                outputAppleStampStateHolder,
                outputAppleStampStateParticipants
            )
        )
        val inputBasketOfApplesState = createInputStateBasketOfApples()
        val transaction = buildTransaction {
            addInputStates(listOf(inputAppleStampState.ref, inputBasketOfApplesState.ref))
            addCommand(AppleCommands.Redeem())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(
            transaction,
            "The issuer of the Apple stamp should be the producing farm of this basket of apple"
        )
    }

    @Test
    fun basketWeightIsZero() {
        val inputAppleStampState = createInputStateAppleStamp()
        val inputBasketOfApplesState = createInputStateBasketOfApples(
            BasketOfApples(
                outputBasketOfApplesStateDescription,
                outputBasketOfApplesStateFarm,
                outputBasketOfApplesStateOwner,
                0,
                outputBasketOfApplesStateParticipants
            )
        )
        val transaction = buildTransaction {
            addInputStates(listOf(inputAppleStampState.ref, inputBasketOfApplesState.ref))
            addCommand(AppleCommands.Redeem())
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(transaction, "The basket of apple has to weigh more than 0")
    }

    @Test
    fun missingCommand() {
        val transaction = buildTransaction {
            addOutputState(outputBasketOfApplesState)
            addSignatories(outputBasketOfApplesStateParticipants)
        }
        assertFailsWith(
            transaction,
            "List is empty."
        )
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
                    "BasketOfApplesContractRedeemCommandTest\$unknownCommand\$MyDummyCommand"
        )
    }
}