package com.damianryan.shinypotato

import java.math.BigDecimal

data class PEBMessage(val id: String,
                      val origId: String? = null,
                      val type: PEBMessageType,
                      val side: Side,
                      val quantity: Int,
                      val symbol: String,
                      val price: BigDecimal = BigDecimal.ONE,
                      val allocs: List<Int>? = null) {
}