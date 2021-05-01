package com.gatsinski.rems

import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

// *****************
// * Contract Code *
// *****************
class RealEstateContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.gatsinski.rems.RealEstateContract"
    }

    interface Commands : CommandData {
        class Register : TypeOnlyCommandData(), Commands
        class Sell : TypeOnlyCommandData(), Commands
        class SellWithLoan : TypeOnlyCommandData(), Commands
        class Rent : TypeOnlyCommandData(), Commands
        class TerminateRent : TypeOnlyCommandData(), Commands
    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value) {
            is Commands.Register -> requireThat {
                "No input states should be consumed when registering a real estate" using tx.inputStates.isEmpty()
                "A single output state should be produced when registering a real estate" using
                        (tx.outputStates.size == 1)
                val outputState = tx.outputStates.single() as RealEstate
                "There should be no tenant when registering a real estate" using (outputState.tenant == null)
                "The owner should sign the transaction when registering a real estate" using
                        (command.signers.toSet() == outputState.participants.map { it.owningKey }.toSet())
            }
            is Commands.Sell -> requireThat {
                "A single input state should be consumed when buying a real estate" using (tx.inputStates.size == 1)
                "A single output state should be produced when buying a real estate" using (tx.outputStates.size == 1)
                val inputState = tx.inputStates.single() as RealEstate
                val outputState = tx.outputStates.single() as RealEstate
                "The owner should change when buying a real estate" using (inputState.owner != outputState.owner)
                "Only the owner should change when buying a real estate" using
                        (inputState == outputState.copy(owner = inputState.owner))
                "The tenant and the buyer should be different when buying a real estate" using
                        (inputState.tenant != outputState.owner)
                val inputStateSigners = inputState.participants.map { it.owningKey }.toSet()
                val outputStateSigners = outputState.participants.map { it.owningKey }.toSet()
                "All affected parties should sign the transaction when a real estate is being bought" using
                        (command.signers.toSet() == inputStateSigners union outputStateSigners)
            }
            is Commands.SellWithLoan -> requireThat {
                "A single input state should be consumed when buying a real estate" using (tx.inputStates.size == 1)
                "A single output state should be produced when buying a real estate" using (tx.outputStates.size == 1)
                val inputState = tx.inputStates.single() as RealEstate
                val outputState = tx.outputStates.single() as RealEstate

                //Bank business to give loan to owner
                val outputBankState = tx.outputStates.single() as Bank
                "Owner in the bank is the same who is interested buying a real estate" using (outputBankState.buyer == outputState.owner)
                "Owner money is superior than 5000" using (outputBankState.buyerMoney > 5000)
                "Owner has professional stability" using (outputBankState.isWorking)
                "Owner has to be at least 25 years old" using (outputBankState.buyerAge > 25)

                "The owner should change when buying a real estate" using (inputState.owner != outputState.owner)
                "Only the owner should change when buying a real estate" using
                        (inputState == outputState.copy(owner = inputState.owner))
                "The tenant and the buyer should be different when buying a real estate" using
                        (inputState.tenant != outputState.owner)
                val inputStateSigners = inputState.participants.map { it.owningKey }.toSet()
                val outputStateSigners = outputState.participants.map { it.owningKey }.toSet()
                "All affected parties should sign the transaction when a real estate is being bought" using
                        (command.signers.toSet() == inputStateSigners union outputStateSigners)
            }
            is Commands.Rent -> requireThat {
                "A single input state should be consumed when renting a real estate" using (tx.inputStates.size == 1)
                "A single output state should be produced when renting a real estate" using (tx.outputStates.size == 1)
                val inputState = tx.inputStates.single() as RealEstate
                val outputState = tx.outputStates.single() as RealEstate
                "There should be no previous tenant when renting a real estate" using (inputState.tenant == null)
                "The tenant should change when renting a real estate" using (outputState.tenant is Party)
                "Only the tenant should change when renting a real estate" using
                        (inputState == outputState.copy(tenant = inputState.tenant))
                "The owner and the tenant should be different when renting a real estate" using
                        (outputState.owner != outputState.tenant)
                val inputStateSigners = inputState.participants.map { it.owningKey }.toSet()
                val outputStateSigners = outputState.participants.map { it.owningKey }.toSet()
                "Both owner and tenant should sign the transaction when renting a real estate" using
                        (command.signers.toSet() == inputStateSigners union outputStateSigners)
            }
            is Commands.TerminateRent -> requireThat {
                "A single input state should be consumed when terminating a real estate rent" using
                        (tx.inputStates.size == 1)
                "A single output state should be produced when terminating a real estate rent" using
                        (tx.outputStates.size == 1)
                val inputState = tx.inputStates.single() as RealEstate
                val outputState = tx.outputStates.single() as RealEstate
                "There should be a tenant before terminating a real estate rent" using (inputState.tenant is Party)
                "The tenant should be removed when terminating a real estate rent" using
                        (outputState.tenant == null)
                "Only the tenant should change when terminating a real estate rent" using
                        (inputState == outputState.copy(tenant = inputState.tenant))
                val inputStateSigners = inputState.participants.map { it.owningKey }.toSet()
                val outputStateSigners = outputState.participants.map { it.owningKey }.toSet()
                "Both owner and tenant should sign the transaction when terminating a real estate rent" using
                        (command.signers.toSet() == inputStateSigners union outputStateSigners)
            }
            else -> throw IllegalArgumentException("Invalid command")
        }
    }
}

// *********
// * State *
// *********
data class RealEstate(
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    val owner: Party,
    val tenant: Party? = null,
    val address: String
) : LinearState {
    override val participants: List<Party> get() = listOfNotNull(owner, tenant)
}

// *********
// * Bank *
// *********
data class Bank(
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
//    val bankName: Party,
//    val bankMoney: Int,
    val buyer: Party,
    val buyerMoney: Int,
    val isWorking: Boolean,
    val buyerAge: Int,
    val loan: Int? = null
) : LinearState {
    override val participants: List<Party> get() = listOf(buyer)
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.gatsinski.rems.BankContract"
    }
}

