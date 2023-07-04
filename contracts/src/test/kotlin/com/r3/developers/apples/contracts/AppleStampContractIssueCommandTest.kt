package com.r3.developers.apples.contracts

import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.apples.states.AppleStamp
import net.corda.v5.ledger.utxo.Command
import org.junit.jupiter.api.Test

class AppleStampContractIssueCommandTest : ApplesContractTest() {

    @Test
    fun happyPath() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.Issue())
            addOutputState(outputAppleStampState)
            addSignatories(outputAppleStampStateParticipants)
        }
        assertVerifies(transaction)
    }

    @Test
    fun outputContractStateSizeNotOne() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.Issue())
            addOutputState(outputAppleStampState)
            addOutputState(outputAppleStampState)
            addSignatories(outputAppleStampStateParticipants)
        }
        assertFailsWith(
            transaction,
            "This transaction should only have one AppleStamp state as output"
        )
    }

    @Test
    fun blankStampDescription() {
        val transaction = buildTransaction {
            addCommand(AppleCommands.Issue())
            addOutputState(
                AppleStamp(
                    outputAppleStampStateId,
                    "",
                    outputAppleStampStateIssuer,
                    outputAppleStampStateHolder,
                    outputAppleStampStateParticipants
                )
            )
            addSignatories(outputAppleStampStateParticipants)
        }
        assertFailsWith(
            transaction,
            "The output AppleStamp state should have clear description of the type of redeemable goods"
        )
    }

    @Test
    fun missingCommand() {
        val transaction = buildTransaction {
            addOutputState(outputAppleStampState)
            addSignatories(outputAppleStampStateParticipants)
        }
        assertFailsWith(transaction, "List is empty.")
    }

    @Test
    fun unknownCommand() {
        class MyDummyCommand : Command;

        val transaction = buildTransaction {
            addCommand(MyDummyCommand())
            addOutputState(outputAppleStampState)
            addSignatories(outputAppleStampStateParticipants)
        }
        assertFailsWith(
            transaction,
            "Incorrect type of AppleStamp commands: com.r3.developers.apples.contracts." +
                    "AppleStampContractIssueCommandTest\$unknownCommand\$MyDummyCommand"
        )
    }
}