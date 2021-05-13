package com.gatsinski.rems.contracts

import com.gatsinski.rems.RealEstate
import com.gatsinski.rems.RealEstateContract
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class RegisterCommandTests {
    private val ledgerServices = MockServices(listOf("com.gatsinski.rems"))
    private val owner = TestIdentity(CordaX500Name("John Doe", "City", "BG"))
    private val tenant = TestIdentity(CordaX500Name("Richard Roe", "Town", "BG"))
    private val participants = listOf(owner.publicKey)

    private val realEstate = RealEstate(
        value = 2000000,
        owner = owner.party,
        address = "City, Test Residential Quarter, Building 1, Entrance A, №1"
    )

    private val realEstateWithTenant = realEstate.copy(tenant = tenant.party)

    @Test
    fun `Register command should verify successfully`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Register())
                output(RealEstateContract.PROGRAM_ID, realEstate)
                verifies()
            }
        }
    }

    @Test
    fun `No input states should be consumed when registering a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Register())
                input(RealEstateContract.PROGRAM_ID, DummyState())
                output(RealEstateContract.PROGRAM_ID, DummyState())
                failsWith("No input states should be consumed when registering a real estate")
            }
        }
    }

    @Test
    fun `A single output should be produced when registering a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Register())
                output(RealEstateContract.PROGRAM_ID, DummyState())
                output(RealEstateContract.PROGRAM_ID, DummyState())
                failsWith(
                    "A single output state should be produced when registering a real estate"
                )
            }
        }
    }

    @Test
    fun `There should be no tenant when registering a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(participants, RealEstateContract.Commands.Register())
                output(RealEstateContract.PROGRAM_ID, realEstateWithTenant)
                failsWith("There should be no tenant when registering a real estate")
            }
        }
    }

    @Test
    fun `The owner should sign the transaction when registering a real estate`() {
        ledgerServices.ledger {
            transaction {
                command(listOf(tenant.publicKey), RealEstateContract.Commands.Register())
                output(RealEstateContract.PROGRAM_ID, realEstate)
                failsWith(
                    "The owner should sign the transaction when registering a real estate"
                )
            }
        }
    }
}
