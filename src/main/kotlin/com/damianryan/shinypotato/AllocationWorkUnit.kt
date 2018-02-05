package com.damianryan.shinypotato

data class AllocationWorkUnit(val id: String,
                              val allocations: List<Allocation>) {

    val quantity: Int
        get() = allocations.map(Allocation::quantity).sum()

    fun tradeAllocations() = trades.map { TradeAllocation(it, allocationsForTrade(it)) }

    private val trades: Set<Trade>
        get() = allocations.map(Allocation::trades).flatten().toSet()

    private fun allocationsForTrade(trade: Trade): List<Allocation> {
        return allocations.filter { it.trades.contains(trade) }
    }
}