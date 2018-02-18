package com.damianryan.shinypotato

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IRecordProcessorFactory
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.types.InitializationInput
import com.amazonaws.services.kinesis.clientlibrary.types.ProcessRecordsInput
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput
import com.amazonaws.services.kinesis.producer.KinesisProducer
import com.amazonaws.services.kinesis.producer.KinesisProducerConfiguration
import com.amazonaws.services.kinesis.producer.UserRecordFailedException
import com.amazonaws.services.kinesis.producer.UserRecordResult
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.collect.Iterables
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

fun main(args: Array<String>) {
    consume()
//    produce()
}

fun consume() {
    val logger = LoggerFactory.getLogger("consumer")

    val config = clientConfiguration()
}

fun produce() {
    val logger = LoggerFactory.getLogger("producer")

    val producer = KinesisProducer(producerConfiguration())
    val mapper = objectMapper()

    val trade = Trade("T1", Side.BUY, 10, "FOO", BigDecimal.ONE, ZonedDateTime.now())
    val allocation = Allocation("A1", 10, listOf(trade), "ABC123", "CLIENT")
    val workUnit = AllocationWorkUnit("WU1", listOf(allocation))

    val json = mapper.writeValueAsString(workUnit)

    val completed = AtomicLong()
    val callback = ProducerCallback(completed)

    val data = ByteBuffer.wrap(json.toByteArray(StandardCharsets.UTF_8))
    val future = producer.addUserRecord("premises_to_aws_stream", workUnit.id, data)
    Futures.addCallback(future, callback)

    logger.info("waiting for put to finish...")
    producer.flushSync()
    logger.info("all records put")
    producer.destroy()
    logger.info("finished")
}

fun clientConfiguration(): KinesisClientLibConfiguration {
    return KinesisClientLibConfiguration("shiny-potato", "premises_to_aws_stream",
            DefaultAWSCredentialsProviderChain(), "W1")
}

fun producerConfiguration(): KinesisProducerConfiguration {
    return KinesisProducerConfiguration().apply {
        recordMaxBufferedTime = TimeUnit.SECONDS.toMillis(2)
        maxConnections = 2
        requestTimeout = TimeUnit.SECONDS.toMillis(6)
        region = "us-east-1"
        credentialsProvider = DefaultAWSCredentialsProviderChain()
    }
}

fun objectMapper(): ObjectMapper {
    return jacksonObjectMapper().apply {
        findAndRegisterModules()
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    }
}

class ProducerCallback(val completed: AtomicLong): FutureCallback<UserRecordResult> {

    private val logger = LoggerFactory.getLogger(ProducerCallback::class.java)

    override fun onSuccess(result: UserRecordResult?) {
        completed.getAndIncrement()
        logger.info("put succeeded with sequence number {} to shard {}", result?.sequenceNumber, result?.shardId)
    }

    override fun onFailure(t: Throwable?) {
        if (t is UserRecordFailedException) {
            val last = Iterables.getLast(t.result.attempts)
            logger.error("record failed to put - {} : {}", last.errorCode, last.errorMessage)
        }
        logger.error("exception during put", t)
        System.exit(1)
    }
}

class RecordProcessorFactory: IRecordProcessorFactory {
    override fun createProcessor() = RecordProcessor()
}

class RecordProcessor: IRecordProcessor {

    private val logger = LoggerFactory.getLogger(RecordProcessor::class.java)
    private val processorId = processorIds.getAndIncrement()

    override fun shutdown(input: ShutdownInput?) {
        logger.info("record processor {} shutdown; checkpointer {}, reason {}",
                processorId, input?.checkpointer, input?.shutdownReason)
    }

    override fun initialize(input: InitializationInput?) {
        logger.info("record processor {} initialized against {}; extended SN {}, pending checkpoint SN {}",
                processorId, input?.shardId, input?.extendedSequenceNumber, input?.pendingCheckpointSequenceNumber)
    }

    override fun processRecords(processRecordsInput: ProcessRecordsInput?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private val processorIds = AtomicLong(1L)
    }
}