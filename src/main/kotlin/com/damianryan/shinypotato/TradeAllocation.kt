package com.damianryan.shinypotato

data class TradeAllocation(val trade: Trade,
                           val allocations: List<Allocation>) {

    val isSplit: Boolean
        get() = allocations.size > 1

    override fun toString() = "${trade.id}: ${allocations.size} allocs: ${allocIds}"

    private val allocIds
        get() = allocations.map { it.id }.joinToString(",")
}