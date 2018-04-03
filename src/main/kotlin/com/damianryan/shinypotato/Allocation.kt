package com.damianryan.shinypotato

/**
 * Allocation of all or some quantity of lots of one of more trades to a client end account, corresponding to a CCP
 * position account.
 * @author Damian Ryan
 * @since January 2018
 */
data class Allocation(val id: String,
                      val quantity: Int,
                      val trades: List<Trade>,
                      val endAccount: String,
                      val positionAccount: String) {

    override fun toString() = "${id}: ${quantity} to ${endAccount}/${positionAccount} (${tradeIds})"

    private val tradeIds
        get() = trades.map { it.id }.joinToString(",")
}