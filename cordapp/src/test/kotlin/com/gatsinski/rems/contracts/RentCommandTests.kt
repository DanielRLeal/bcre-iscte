package com.gatsinski.rems.contracts

import com.gatsinski.rems.RealEstate
import com.gatsinski.rems.RealEstateContract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class RentCommandTests {
    private val ledgerServices = MockServices(listOf("com.gatsinski.rems"))
    private val owner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val tenant = TestIdentity(CordaX500Name("Richard Roe", "Town", "BG"))
    private val thirdParty = TestIdentity(CordaX500Name("Jane Roe", "Town", "BG"))
    private val participants = listOf(owner.publicKey, tenant.publicKey)

    private val realEstateWithoutTenant = RealEstate(
        owner = owner.party,
        value = 1000,
        address = "City, Test Residential Quarter, Building 75, Entrance A, №10"
    )

    private val realEstateWithTenant = realEstateWithoutTenant.copy(tenant = tenant.party)

    @Test
    fun `Rent command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                verifies()
            }
        }
    }

    @Test
    fun `A single input state should be consumed when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith("A single input state should be consumed when renting a real estate")
            }
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith("A single input state should be consumed when renting a real estate")
            }
        }
    }

    @Test
    fun `A single output state should be produced when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "A single output state should be produced when renting a real estate"
                )
            }
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith(
                    "A single output state should be produced when renting a real estate"
                )
            }
        }
    }

    @Test
    fun `There should be no previous tenant when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant.copy(tenant = thirdParty.party))
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith("There should be no previous tenant when renting a real estate")
            }
        }
    }

    @Test
    fun `The tenant should change when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith("The tenant should change when renting a real estate")
            }
        }
    }

    @Test
    fun `Only the tenant should change when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant.copy(owner = thirdParty.party))
                failsWith("Only the tenant should change when renting a real estate")
            }
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant.copy(address = "New City, Fake Address"))
                failsWith("Only the tenant should change when renting a real estate")
            }
        }
    }

    @Test
    fun `The owner and the tenant should be different when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant.copy(tenant = owner.party))
                failsWith(
                    "The owner and the tenant should be different when renting a real estate"
                )
            }
        }
    }

    @Test
    fun `Both owner and tenant should sign the transaction when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(owner.publicKey), RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith(
                    "Both owner and tenant should sign the transaction when renting a real estate"
                )
            }
            transaction {
                command(listOf(tenant.publicKey), RealEstateContract.Commands.Rent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith(
                    "Both owner and tenant should sign the transaction when renting a real estate"
                )
            }
        }
    }
}
