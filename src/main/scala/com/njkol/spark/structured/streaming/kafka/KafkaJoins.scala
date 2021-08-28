package com.njkol.spark.structured.streaming.kafka

import org.apache.spark.sql.SparkSession

/**
 * Demonstrate Kafka Source Watermarking
 *
 * @author Nilanjan Sarkar
 */
class KafkaJoins {
  
  private val kafkaServer = "localhost:9092"
  private val topicName = "demo"
  
  def runJob(spark: SparkSession) {

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("subscribe", topicName)
      .option("startingOffsets", "latest")
      .load()
  }
}