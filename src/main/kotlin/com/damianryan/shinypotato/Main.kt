package com.damianryan.shinypotato

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.DescribeStreamResult
import com.amazonaws.services.kinesis.model.ResourceNotFoundException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.time.ZonedDateTime

fun main(args: Array<String>) {

    // need to set credentials
    System.setProperty("aws.accessKeyId", "AKIAIWQRJSVCUDGGOFSQ")
    System.setProperty("aws.secretKey", "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")
    val config = ClientConfiguration();
    val userAgent = StringBuilder(ClientConfiguration.DEFAULT_USER_AGENT)
    userAgent.append(" ").append("shiny-potato/1.0-SNAPSHOT")
    config.userAgentPrefix = userAgent.toString()

    val credentialsProvider = DefaultAWSCredentialsProviderChain()

    val kinesis = AmazonKinesisClientBuilder.standard().withCredentials(credentialsProvider).withClientConfiguration(config).withRegion("us-east-1").build()

    try {
        val result = kinesis.describeStream("premises_to_aws_stream")
        if (!isActive(result)) {
            println("stream is not active")
            return
        } else {
            println("stream is present and active")
        }
    } catch (x: ResourceNotFoundException) {
        println("unable to find stream")
        return
    }

    val mapper = jacksonObjectMapper()
    mapper.findAndRegisterModules()
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    mapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

    val trade = Trade("T1", Side.BUY, 10, "FOO", BigDecimal.ONE, ZonedDateTime.now())
    val allocation = Allocation("A1", 10, listOf(trade), "ABC123", "CLIENT")
    val workUnit = AllocationWorkUnit("WU1", listOf(allocation))

    val json = mapper.writeValueAsString(workUnit)

    println("json = ${json}")

//    kinesis.putRecord("premises_to_aws_stream", ByteBuffer.wrap(json.toByteArray(StandardCharsets.UTF_8)), "T1")

    // TODO get record

}

fun isActive(describeStreamResult: DescribeStreamResult): Boolean = "ACTIVE" == describeStreamResult.streamDescription.streamStatus
