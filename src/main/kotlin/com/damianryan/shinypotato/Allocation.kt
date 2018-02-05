package com.damianryan.shinypotato

data class Allocation(val id: String,
                      val quantity: Int,
                      val trades: List<Trade>,
                      val endAccount: String,
                      val positionAccount: String) {

    override fun toString() = "${id}: ${quantity} to ${endAccount}/${positionAccount} (${tradeIds})"

    private val tradeIds
        get() = trades.map { it.id }.joinToString(",")
}