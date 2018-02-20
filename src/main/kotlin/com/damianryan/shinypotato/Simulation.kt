package com.damianryan.shinypotato

import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList

class PEBSimulator : AutoCloseable {

    private val toMarket = LinkedBlockingQueue<PEBMessage>()
    private val fromMarket = LinkedBlockingQueue<PEBMessage>()
    private val allocationExecutor = Executors.newSingleThreadExecutor()

    fun start() {
        allocationExecutor.execute(Allocator(toMarket, fromMarket))
    }

    fun read() = toMarket.take()

    fun write(message: PEBMessage) {
        fromMarket.add(message)
    }

    override fun close() {
        allocationExecutor.shutdownNow()
    }
}

class CCPSimulator : AutoCloseable {

    private val toMember = LinkedBlockingQueue<CCPMessage>()
    private val fromMember = LinkedBlockingQueue<CCPMessage>()
    private val newTradeExecutor = Executors.newSingleThreadScheduledExecutor()
    private val responseExecutor = Executors.newSingleThreadExecutor()

    fun start() {
        newTradeExecutor.scheduleAtFixedRate(CCPTradeGenerator(toMember), 0, 100, TimeUnit.MILLISECONDS)
        responseExecutor.execute(CCPResponseGenerator(toMember, fromMember))
    }

    fun read() = toMember.take()

    fun write(message: CCPMessage) {
        fromMember.add(message)
    }

    override fun close() {
        newTradeExecutor.shutdownNow()
        responseExecutor.shutdownNow()
    }

    companion object {
        val tradeIds = AtomicLong(100_000L)
    }
}

class CCPTradeGenerator(val toMember: LinkedBlockingQueue<CCPMessage>) : Runnable {

    private val random = Random()
    private val symbols = listOf("OIL", "GAS", "SUN", "HAM", "EGG")
    private val prices = listOf(100L, 250L, 55L, 10L, 5L)

    override fun run() {
        val quantity = random.nextInt(100) + 1
        val instrumentIndex = random.nextInt(prices.size)
        val basePrice = prices[instrumentIndex]
        val priceDelta = random.nextInt(5)
        val minus = random.nextBoolean()
        val buy = random.nextBoolean()
        val price = basePrice + (if (minus) -priceDelta else priceDelta)
        val trade = CCPMessage(
                id = "${CCPSimulator.tradeIds.getAndIncrement()}",
                side = if (buy) Side.BUY else Side.SELL,
                quantity = quantity,
                symbol = symbols[instrumentIndex],
                price = BigDecimal.valueOf(price))
        LOGGER.info("generated new trade {}", trade);
        toMember.add(trade);
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(CCPTradeGenerator::class.java)
    }
}

class CCPResponseGenerator(val toMember: LinkedBlockingQueue<CCPMessage>,
                           val fromMember: LinkedBlockingQueue<CCPMessage>) : Runnable {

    private val random = Random()

    override fun run() {
        while (true) {
            val request = fromMember.take()
            val splits = request.splits
            LOGGER
                .info(
                        "CCP received {}-way allocation request on trade {}",
                        if (null == splits) 1 else splits.size,
                        request.id)
            val shouldNack = 13 == random.nextInt(100)
            if (shouldNack) {
                val nack = request.copy(type = CCPMessageType.NACK)
                toMember.add(nack)
                LOGGER.info("CCP generated nack {}", nack)
            } else {
                val ack = request.copy(type = CCPMessageType.ACK)
                toMember.add(ack)
                LOGGER.info("CCP generated ack {}", ack)
                if (1 != splits?.size) {
                    splits?.forEach {
                        val split = request.copy(
                                id = "${CCPSimulator.tradeIds.getAndIncrement()}",
                                type = CCPMessageType.TRADE,
                                origId = request.id,
                                quantity = it,
                                splits = null)
                        LOGGER.info("CCP generated child trade {}", split)
                        toMember.add(split)
                    }
                }
            }
        }
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(CCPResponseGenerator::class.java)
    }
}

class Allocator(val toMarket: LinkedBlockingQueue<PEBMessage>,
                val fromMarket: LinkedBlockingQueue<PEBMessage>) : Runnable {

    private val random = Random()

    override fun run() {
        while (true) {
            val tradeOrAck = fromMarket.take()
            LOGGER.info("allocator received {} message for trade {}", tradeOrAck.type, tradeOrAck.id)
            if (tradeOrAck.type == PEBMessageType.TRADE && null == tradeOrAck.origId) allocate(tradeOrAck)
        }
    }

    private fun allocate(trade: PEBMessage) {
        val quantity = trade.quantity
        when (quantity) {
            1 -> {
                val oneToOne = trade.copy(type = PEBMessageType.ALLOCATION)
                toMarket.add(oneToOne)
                LOGGER.info("allocator generated 1:1 allocation {}", oneToOne)
            }
            2 -> split(trade, 2)
            else -> split(trade, random.nextInt(Math.min(4, quantity)) + 1)
        }
    }

    private fun split(trade: PEBMessage, splits: Int) {
        val quantities = ArrayList<Int>()
        val clip = trade.quantity / splits
        var left = trade.quantity
        while (quantities.size < splits) {
            quantities.add(clip)
            left -= clip
        }
        if (left > 0) {
            quantities.removeAt(0)
            quantities.add(left + clip)
        }
        LOGGER.info("allocator generated {}-way allocation on trade {}", quantities.size, trade.id)
        val split = trade.copy(type = PEBMessageType.ALLOCATION, allocs = quantities)
        toMarket.add(split)
        LOGGER.info("allocator generated split allocation {}", split)
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(Allocator::class.java)
    }
}