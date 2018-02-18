package com.damianryan.shinypotato

import java.math.BigDecimal


data class CCPMessage(val id: String,
                      val origId: String? = null,
                      val type: CCPMessageType = CCPMessageType.TRADE,
                      val side: Side,
                      val quantity: Int,
                      val symbol: String,
                      val price: BigDecimal = BigDecimal.ONE,
                      val splits: List<Int>? = null) {
}