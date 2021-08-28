package com.njkol.spark.structured.streaming.kafka

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger

class KafkaTester {

  private val kafkaServer = "localhost:9092"
  private val topicName = "demo"
  
  def runJob(spark: SparkSession) {

    val df = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("subscribe", topicName)
      .option("startingOffsets", "latest")
      .load()

    val transformed = df.selectExpr("CAST(value AS STRING)", "timestamp")

    val query = transformed.writeStream
      .outputMode("append")
      .trigger(Trigger.ProcessingTime("5 seconds"))
      .format("console")
      .start()

    query.awaitTermination()
  }
}