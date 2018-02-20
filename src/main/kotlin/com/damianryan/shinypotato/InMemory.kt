package com.damianryan.shinypotato

import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue

fun main(args: Array<String>) {
    val ccp = CCPSimulator()
    val peb = PEBSimulator()
    ccp.start()
    peb.start()

    val input = LinkedBlockingQueue<Any>()
    val output = LinkedBlockingQueue<Any>()
    val ccpReader = Executors.newSingleThreadExecutor()
    val pebReader = Executors.newSingleThreadExecutor()
    val writer = Executors.newSingleThreadExecutor()
    ccpReader.execute({
        while (true) {
            input.add(ccp.read())
        }
    })
    pebReader.execute({
        while (true) {
            input.add(peb.read())
        }
    })
    writer.execute({
        while (true) {
            val message = output.take()
            if (message is PEBMessage) {
                peb.write(message)
            } else if (message is CCPMessage) {
                ccp.write(message)
            }
        }
    })

    val gateway = Executors.newSingleThreadExecutor()
    gateway.execute(InMemoryGateway(input, output))
}

class InMemoryGateway(val input: LinkedBlockingQueue<Any>, val output: LinkedBlockingQueue<Any>) : Runnable {

    override fun run() {
        while (true) {
            val next = input.take()
            when (next) {
                is PEBMessage -> handleAllocation(next)
                is CCPMessage -> handleCCPMessage(next)
                else -> LOGGER.warn("unexpected input message type: {}", next.javaClass)
            }
        }
    }

    private fun handleCCPMessage(next: CCPMessage) {
        LOGGER.info("gateway processing {}", next)
        val response = toPEBMessage(next)
        LOGGER.info("gateway outputting {}", response)
        output.add(response)
    }

    private fun handleAllocation(next: PEBMessage) {
        LOGGER.info("gateway processing {}", next)
        val response = toCCPMessage(next)
        LOGGER.info("gateway outputting {}", response)
        output.add(response)
    }

    companion object {
        val LOGGER = LoggerFactory.getLogger(InMemoryGateway::class.java)
    }
}