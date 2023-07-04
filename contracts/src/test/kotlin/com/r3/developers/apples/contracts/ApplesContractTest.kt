package com.r3.developers.apples.contracts

import com.r3.corda.ledger.utxo.testing.ContractTest
import com.r3.corda.ledger.utxo.testing.buildTransaction
import com.r3.developers.apples.states.AppleStamp
import com.r3.developers.apples.states.BasketOfApples
import net.corda.v5.ledger.utxo.StateAndRef
import java.security.PublicKey
import java.util.*

@Suppress("UnnecessaryAbstractClass", "UtilityClassWithPublicConstructor")
abstract class ApplesContractTest : ContractTest() {

    protected val outputAppleStampStateId: UUID = UUID.randomUUID()
    protected val outputAppleStampStateStampDesc: String = "Can be exchanged for a single basket of apples"
    protected val outputAppleStampStateIssuer: PublicKey = bobKey
    protected val outputAppleStampStateHolder: PublicKey = daveKey
    protected val outputAppleStampStateParticipants: List<PublicKey> = listOf(bobKey, daveKey)
    protected val outputAppleStampState: AppleStamp = AppleStamp(
        outputAppleStampStateId,
        outputAppleStampStateStampDesc,
        outputAppleStampStateIssuer,
        outputAppleStampStateHolder,
        outputAppleStampStateParticipants
    )

    protected val outputBasketOfApplesStateDescription: String = "Golden delicious apples, picked on 11th May 2023"
    protected val outputBasketOfApplesStateFarm: PublicKey = bobKey
    protected val outputBasketOfApplesStateOwner: PublicKey = bobKey
    protected val outputBasketOfApplesStateWeight: Int = 214
    protected val outputBasketOfApplesStateParticipants: List<PublicKey> = listOf(bobKey)
    internal val outputBasketOfApplesState: BasketOfApples = BasketOfApples(
        outputBasketOfApplesStateDescription,
        outputBasketOfApplesStateFarm,
        outputBasketOfApplesStateOwner,
        outputBasketOfApplesStateWeight,
        outputBasketOfApplesStateParticipants
    )

    @Suppress("UNCHECKED_CAST")
    protected fun createInputStateAppleStamp(outputState: AppleStamp = outputAppleStampState): StateAndRef<AppleStamp> {
        val transaction = buildTransaction {
            addOutputState(outputState)
            addCommand(AppleCommands.Issue())
            addSignatories(outputState.participants)
        }
        transaction.toLedgerTransaction()
        return transaction.outputStateAndRefs.first() as StateAndRef<AppleStamp>
    }

    @Suppress("UNCHECKED_CAST")
    protected fun createInputStateBasketOfApples(outputState: BasketOfApples = outputBasketOfApplesState): StateAndRef<BasketOfApples> {
        val transaction = buildTransaction {
            addOutputState(outputState)
            addCommand(AppleCommands.PackBasket())
            addSignatories(outputState.participants)
        }
        transaction.toLedgerTransaction()
        return transaction.outputStateAndRefs.first() as StateAndRef<BasketOfApples>
    }
}