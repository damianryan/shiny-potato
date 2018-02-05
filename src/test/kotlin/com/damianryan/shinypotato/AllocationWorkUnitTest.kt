package com.damianryan.shinypotato

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime

class AllocationWorkUnitTest {

    @Test
    fun expectedTradeAllocations() {
        val trade1 = Trade("T1", Side.BUY, 5, "CAD", BigDecimal.TEN, ZonedDateTime.now())
        val trade2 = Trade("T2", Side.BUY, 6, "CAD", BigDecimal.TEN, ZonedDateTime.now())
        val trade3 = Trade("T3", Side.BUY, 7, "CAD", BigDecimal.TEN, ZonedDateTime.now())
        val allocation1 = Allocation("A1", 3, listOf(trade1), "ABC123", "H")
        val allocation2 = Allocation("A2", 8, listOf(trade1, trade2), "DEF234", "H")
        val allocation3 = Allocation("A3", 7, listOf(trade3), "GHI345", "H")
        val workUnit = AllocationWorkUnit("AWU1", listOf(allocation1, allocation2, allocation3))

        assertEquals(18, workUnit.quantity, "unexpected work unit quantity")

        val tradeAllocations = workUnit.tradeAllocations

        assertEquals(3, tradeAllocations.size, "unexpected number of allocated trades")

        val trade1Allocations = tradeAllocations.filter { it.trade == trade1 }.map {it.allocations}.flatten()

        assertEquals(2, trade1Allocations.size)
        assertTrue(trade1Allocations.contains(allocation1))
        assertTrue(trade1Allocations.contains(allocation2))

        val trade2Allocations = tradeAllocations.filter { it.trade == trade2 }.map {it.allocations}.flatten()

        assertEquals(1, trade2Allocations.size)
        assertTrue(trade2Allocations.contains(allocation2))

        val trade3Allocations = tradeAllocations.filter { it.trade == trade3 }.map {it.allocations}.flatten()

        assertEquals(1, trade3Allocations.size)
        assertTrue(trade3Allocations.contains(allocation3))
    }
}