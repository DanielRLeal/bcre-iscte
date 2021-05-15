package com.gatsinski.rems

import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.utils.sumCashBy
import org.bouncycastle.asn1.x500.style.RFC4519Style.owner
import java.lang.NumberFormatException
import java.time.Instant
import java.util.*

// *****************
// * Contract Code *
// *****************
class RealEstateContract : Contract {
    companion object {
        const val PROGRAM_ID: ContractClassName = "com.gatsinski.rems.RealEstateContract"
    }

    fun generateIssue(issuance: PartyAndReference, currency: Amount<Issued<Currency>>, maturityDate: Instant,
                      notary: Party, isWorking: Boolean, Age: Int, accountNumber: String, accountName: String, loan:Int):
            TransactionBuilder {
        val state = BankAccount(issuance.party as AnonymousParty, issuance, currency, maturityDate, isWorking, Age, accountNumber, accountName, loan)
        val stateAndContract = StateAndContract(state, PROGRAM_ID)
        return TransactionBuilder(notary = notary).withItems(stateAndContract, Command(Commands.Issue(), issuance.party.owningKey))
    }

    fun generateMove(tx: TransactionBuilder, paper: StateAndRef<BankAccount>, newOwner: AbstractParty) {
        tx.addInputState(paper)
        val outputState = paper.state.data.withNewOwner(newOwner).ownableState
        tx.addOutputState(outputState, PROGRAM_ID)
        tx.addCommand(Command(Commands.Move(), paper.state.data.owner.owningKey))
    }

    @Throws(InsufficientBalanceException::class)
    fun generateRedeem(tx: TransactionBuilder, paper: StateAndRef<BankAccount>, services: ServiceHub) {
        // Add the cash movement using the states in our vault.
        Cash.generateSpend(
            services = services,
            tx = tx,
            amount = paper.state.data.currency.withoutIssuer(),
           // ourIdentity = services.myInfo.singleIdentityAndCert(),
            to = paper.state.data.owner
        )
        tx.addInputState(paper)
        tx.addCommand(Command(Commands.Redeem(), paper.state.data.owner.owningKey))
    }



    interface Commands : CommandData {
        //RealEstateCommands
        class Register : TypeOnlyCommandData(), Commands
        class Sell : TypeOnlyCommandData(), Commands
        class SellWithLoan : TypeOnlyCommandData(), Commands
        class Rent : TypeOnlyCommandData(), Commands
        class TerminateRent : TypeOnlyCommandData(), Commands
        //BankCommands
        class Move : TypeOnlyCommandData(), Commands
        class Redeem : TypeOnlyCommandData(), Commands
        class Issue : TypeOnlyCommandData(), Commands

    }

    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {

        val timeWindow: TimeWindow? = tx.timeWindow
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

                val inputBankState = tx.inputStates.single() as BankAccount
                val outputBankState = tx.outputStates.single() as BankAccount
                "Buyer must have the amount greater than the real estate price"  using (outputBankState.currency.quantity >= inputState.value);


                "All affected parties should sign the transaction when a real estate is being bought" using
                        (command.signers.toSet() == inputStateSigners union outputStateSigners)

                //Isto deve ser uma cena para aplicar nao uma condiçao
              //  "Remove money from bank account " using (outputBankState.buyerMoney == outputBankState.buyerMoney - inputState.value )
            }
            is Commands.SellWithLoan -> requireThat {
                "A single input state should be consumed when buying a real estate" using (tx.inputStates.size == 1)
                "A single output state should be produced when buying a real estate" using (tx.outputStates.size == 1)
                val inputState = tx.inputStates.single() as RealEstate
                val outputState = tx.outputStates.single() as RealEstate

                //Bank business to give loan to owner
                val outputBankState = tx.outputStates.single() as BankAccount
                //"Owner in the bank is the same who is interested buying a real estate" using (outputBankState.owner == outputState.owner)
                "Owner money is superior than 5000" using (outputBankState.currency.quantity > 5000)
                "Owner has professional stability" using (outputBankState.isWorking)
                "Owner has to be at least 25 years old" using (outputBankState.Age > 25)
                //Se isto tudo estiver ok é feito o emprestimo e ele vai verificar novamente o saldo na conta do comprador
                "Buyer must have the amount greater than the real estate price"  using (outputBankState.currency.quantity >= inputState.value);

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
                val outputBankState = tx.outputStates.single() as BankAccount
                "Buyer must have the amount greater than the real estate rent price"  using (outputBankState.currency.quantity >= inputState.value);
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
            is Commands.Move -> {
                val input = tx.inputStates.single() as BankAccount
                requireThat {
                    "the transaction is signed by the owner of the commercial paper" using (input.owner.owningKey in command.signers)
                    "the state is propagated" using (tx.outputStates.size == 1)
                    // Don't need to check anything else, as if outputs.size == 1 then the output is equal to
                    // the input ignoring the owner field due to the grouping.
                }
            }

            is Commands.Redeem -> {
                // Redemption of the paper requires movement of on-ledger cash.
                val input = tx.inputStates.single() as BankAccount
                val received = tx.outputs.map { it.data }.sumCashBy(input.owner)
                val time = timeWindow?.fromTime ?: throw IllegalArgumentException("Redemptions must be timestamped")
                requireThat {
                    "the paper must have matured" using (time >= input.maturityDate)
                    "the received amount equals the face value" using (received == input.currency)
                    "the paper must be destroyed" using tx.outputStates.isEmpty()
                    "the transaction is signed by the owner of the commercial paper" using (input.owner.owningKey in command.signers)
                }
            }

            is Commands.Issue -> {
                val output = tx.outputStates.single() as BankAccount
                val time = timeWindow?.untilTime ?: throw IllegalArgumentException("Issuances must be timestamped")
                requireThat {
                    // Don't allow people to issue commercial paper under other entities identities.
                    "output states are issued by a command signer" using (output.issuance.party.owningKey in command.signers)
                    "output values sum to more than the inputs" using (output.currency.quantity > 0)
                    "the maturity date is not in the past" using (time < output.maturityDate)
                    // Don't allow an existing commercial paper state to be replaced by this issuance.
                    "can't reissue an existing state" using tx.inputStates.isEmpty()
                }
            }
            else -> throw IllegalArgumentException("Invalid command")
        }
    }
}





