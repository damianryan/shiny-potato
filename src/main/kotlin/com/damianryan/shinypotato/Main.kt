package com.damianryan.shinypotato

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.model.DescribeStreamResult
import com.amazonaws.services.kinesis.model.ResourceNotFoundException

fun main(args: Array<String>) {
    val config = ClientConfiguration();
    val userAgent = StringBuilder(ClientConfiguration.DEFAULT_USER_AGENT)
    userAgent.append(" ").append("shinypotato/1.0-SNAPSHOT")
    config.userAgentPrefix = userAgent.toString()

    val credentialsProvider = DefaultAWSCredentialsProviderChain()

    val kinesis = AmazonKinesisClientBuilder.standard().withCredentials(credentialsProvider).withClientConfiguration(config).withRegion("us-east-1").build()

    try {
        if (!isActive(kinesis.describeStream("shinypotato-test-input"))) {
            kinesis.createStream("shinypotato-test-input", 1)
        }
    } catch (x: ResourceNotFoundException) {

    }
}

fun isActive(describeStreamResult: DescribeStreamResult): Boolean = "ACTIVE" == describeStreamResult.streamDescription.streamStatus
