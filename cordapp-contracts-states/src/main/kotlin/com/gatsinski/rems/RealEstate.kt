package com.gatsinski.rems

import net.corda.core.contracts.CommandAndState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.OwnableState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********
data class RealEstate(
    override val linearId: UniqueIdentifier = UniqueIdentifier(),
    override val owner: Party,
    val value: Int,
    val tenant: Party? = null,
    val address: String
    //val BankAccount: BankAccount
) : LinearState, OwnableState {
    override val participants: List<Party> get() = listOfNotNull(owner, tenant)
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        TODO("Not yet implemented")
    }
}