package com.gatsinski.rems.contracts

import com.gatsinski.rems.RealEstate
import com.gatsinski.rems.RealEstateContract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class TerminateRentCommandTests {
    private val ledgerServices = MockServices(listOf("com.gatsinski.rems"))
    private val owner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val tenant = TestIdentity(CordaX500Name("Richard Roe", "Town", "BG"))
    private val thirdParty = TestIdentity(CordaX500Name("Jane Roe", "Town", "BG"))
    private val participants = listOf(owner.publicKey, tenant.publicKey)

    private val realEstateWithTenant = RealEstate(
        owner = owner.party,
        value = 1000,
        tenant = tenant.party,
        address = "City, Test Residential Quarter, Building 75, Entrance A, №10"
    )

    private val realEstateWithoutTenant = realEstateWithTenant.copy(tenant = null)


    @Test
    fun `TerminateRent command should complete successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                verifies()
            }
        }
    }

    @Test
    fun `A single input state should be consumed when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "A single input state should be consumed when terminating a real estate rent"
                )
            }
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "A single input state should be consumed when terminating a real estate rent"
                )
            }
        }
    }

    @Test
    fun `A single output state should be produced when renting a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith(
                    "A single output state should be produced when terminating a real estate rent"
                )
            }
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "A single output state should be produced when terminating a real estate rent"
                )
            }
        }
    }

    @Test
    fun `There should be a tenant before terminating a real estate rent`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "There should be a tenant before terminating a real estate rent"
                )
            }
        }
    }

    @Test
    fun `The tenant should be removed when terminating a real estate rent`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith(
                    "The tenant should be removed when terminating a real estate rent"
                )
            }
        }
    }

    @Test
    fun `Only the tenant should change when terminating a real estate rent`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant.copy(owner = thirdParty.party))
                failsWith(
                    "Only the tenant should change when terminating a real estate rent"
                )
            }
            transaction {
                command(participants, RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant.copy(address = "New City, Fake Address"))
                failsWith(
                    "Only the tenant should change when terminating a real estate rent"
                )
            }
        }
    }

    @Test
    fun `Both owner and tenant should sign the transaction when terminating a real estate rent`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(owner.publicKey), RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "Both owner and tenant should sign the transaction when terminating a real estate rent"
                )
            }
            transaction {
                command(listOf(tenant.publicKey), RealEstateContract.Commands.TerminateRent())
                input(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                output(RealEstateContract.PROGRAM_ID, realEstateWithoutTenant)
                failsWith(
                    "Both owner and tenant should sign the transaction when terminating a real estate rent"
                )
            }
        }
    }
}
