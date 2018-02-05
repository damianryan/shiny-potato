package com.damianryan.shinypotato

import java.math.BigDecimal
import java.time.ZonedDateTime


data class Trade(val id: String,
                 val side: Side,
                 val quantity: Int,
                 val symbol: String,
                 val price: BigDecimal,
                 val traded: ZonedDateTime) {

    override fun toString(): String = "${id}: ${side} ${quantity} ${symbol} @ ${price} at ${traded}"
}