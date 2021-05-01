package com.gatsinski.rems.contracts

import com.gatsinski.rems.Bank
import com.gatsinski.rems.RealEstate
import com.gatsinski.rems.RealEstateContract
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.eclipse.jetty.util.TypeUtil
import org.junit.Test

class SellWithLoanCommandTests {
    private val ledgerServices = MockServices(listOf("com.gatsinski.rems"))
    private val seller = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val buyer = TestIdentity(CordaX500Name("Jane Doe", "City", "BG"))
    private val tenant = TestIdentity(CordaX500Name("Richard Roe", "Town", "BG"))
    private val buyerAndSeller = listOf(seller.publicKey, buyer.publicKey)
    private val buyerSellerAndTenant = listOf(seller.publicKey, buyer.publicKey, tenant.publicKey)

    private val realEstate = RealEstate(
        owner = seller.party,
        address = "City, Test Residential Quarter, Building 1, Entrance A, №1"
    )
    private val realEstateAfterPurchase = realEstate.copy(owner = buyer.party)
    private val realEstateWithTenant = realEstate.copy(tenant = tenant.party)

    private val bankState1 = Bank(
        buyer = buyer.party,
        buyerMoney =  6000,
        isWorking = true,
        buyerAge = 26
    )

    private val bankState2 = Bank(
        buyer = buyer.party,
        buyerMoney =  3000,
        isWorking = true,
        buyerAge = 21
    )

    private val bankState3 = Bank(
        buyer = buyer.party,
        buyerMoney =  8000,
        isWorking = false,
        buyerAge = 28
    )

    @Test
    fun `Buy command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                verifies()
            }
        }
    }

    @Test
    fun `A single input should be consumed when buying a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                failsWith("A single input state should be consumed when buying a real estate")
            }
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                failsWith("A single input state should be consumed when buying a real estate")
            }
        }
    }

    @Test
    fun `A single output should be produced when buying a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                failsWith("A single output state should be produced when buying a real estate")
            }
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                failsWith("A single output state should be produced when buying a real estate")
            }
        }
    }

    @Test
    fun `The owner should change when buying a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstate)
                failsWith("The owner should change when buying a real estate")
            }
        }
    }

    @Test
    fun `Only the owner should change when buying a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(
                    RealEstateContract.PROGRAM_ID,
                    realEstateAfterPurchase.copy(address = "New City, Fake Address")
                )
                failsWith("Only the owner should change when buying a real estate")
            }
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(
                    RealEstateContract.PROGRAM_ID,
                    realEstateAfterPurchase.copy(tenant = tenant.party)
                )
                failsWith("Only the owner should change when buying a real estate")
            }
        }
    }

    @Test
    fun `The tenant and the buyer should be different when buying a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant.copy(owner = tenant.party))
                failsWith(
                    "The tenant and the buyer should be different when buying a real estate"
                )
            }
        }
    }

    @Test
    fun `Both buyer and seller should sign the transaction when a real estate without tenant is being bought`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(buyer.publicKey), RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                failsWith(
                    "All affected parties should sign the transaction when a real estate is being bought"
                )
            }
            transaction {
                command(listOf(buyer.publicKey), RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstate)
                output(RealEstateContract.PROGRAM_ID, realEstateAfterPurchase)
                failsWith(
                    "All affected parties should sign the transaction when a real estate is being bought"
                )
            }
        }
    }

    @Test
    fun `Buyer, seller and tenant should all sign the transaction when a real estate with tenant is being bought`() {
        ledgerServices.ledger {
            transaction {
                command(buyerAndSeller, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant.copy(owner = buyer.party))
                failsWith(
                    "All affected parties should sign the transaction when a real estate is being bought"
                )
            }
            transaction {
                command(buyerSellerAndTenant, RealEstateContract.Commands.SellWithLoan())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant.copy(owner = buyer.party))
                verifies()
            }
        }
    }
}
