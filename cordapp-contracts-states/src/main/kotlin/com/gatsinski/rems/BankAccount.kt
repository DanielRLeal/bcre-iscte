package com.gatsinski.rems

import net.corda.core.contracts.*
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.AnonymousParty
import net.corda.finance.contracts.CommercialPaper
import java.time.Instant
import java.util.*

// *********
// * Bank *
// *********
data class BankAccount(
    override val owner: AnonymousParty,
    val issuance: PartyAndReference,
    val currency: Amount<Issued<Currency>>,
    val maturityDate: Instant,
    val isWorking: Boolean,
    val Age: Int,
    val accountNumber: String,
    val accountName: String,
    val loan: Int? = null
) : OwnableState {
    override val participants = listOf(owner)

    fun withoutOwner() = copy(owner = AnonymousParty(NullKeys.NullPublicKey))
    override fun withNewOwner(newOwner: AbstractParty): CommandAndState = CommandAndState(CommercialPaper.Commands.Move(), copy(owner = newOwner as AnonymousParty))
}