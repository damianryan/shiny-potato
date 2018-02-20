package com.damianryan.shinypotato

fun toCCPType(type: PEBMessageType): CCPMessageType {
    when(type) {
        PEBMessageType.ALLOCATION -> return CCPMessageType.REQUEST
        else -> throw IllegalArgumentException()
    }
}

fun toPEBType(type: CCPMessageType): PEBMessageType {
    when(type) {
        CCPMessageType.TRADE -> return PEBMessageType.TRADE
        CCPMessageType.ACK -> return PEBMessageType.ACK
        CCPMessageType.NACK -> return PEBMessageType.NACK
        else -> throw IllegalArgumentException()
    }
}

fun toCCPMessage(message: PEBMessage) = CCPMessage(id = message.id,
        origId = message.origId,
        type = toCCPType(message.type),
        side = message.side,
        quantity = message.quantity,
        symbol = message.symbol,
        price = message.price,
        splits = message.allocs)

fun toPEBMessage(message: CCPMessage) = PEBMessage(id = message.id,
        origId = message.origId,
        type = toPEBType(message.type),
        side = message.side,
        quantity = message.quantity,
        symbol = message.symbol,
        price = message.price,
        allocs = message.splits)