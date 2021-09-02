package com.njkol.spark.structured.streaming.kafka

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
 * Demonstrate Kafka Source Watermarking
 *
 * @author Nilanjan Sarkar
 */
class KafkaWaterMarking(spark: SparkSession){

  private val kafkaServer = "10.159.25.58:29092"
  private val topicName = "lines"

  def runJob() {

    val linesSchema = StructType(Seq(
      StructField("TS", LongType, true),
      StructField("CONTENT", StringType, true)))

    val linesStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", kafkaServer)
      .option("subscribe", "lines")
      .option("startingOffsets", "earliest")
      .load()

    val lines = linesStream.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .select(from_json(col("value"), linesSchema).alias("lines"))
      .selectExpr("lines.content AS content", "CAST(lines.ts AS TIMESTAMP) AS timestamp")

    // Split the lines into words
    val words = lines.select(
      // explode turns each item in an array into a separate row
      explode(split(lines("content"), " ")).alias("word"),
      lines("timestamp"))
       // Define Watermark with 5 second lateness
      .withWatermark("timestamp", "5 seconds")

    words.printSchema()

    // Generate running word count
    val windowedCounts = words.groupBy(
      window(words("timestamp"), "10 seconds", "5 seconds"), // Sliding Window of window length of 10 sec and slide interval of 5 sec
      words("word")).
      count()

    windowedCounts.printSchema()

    // Define a local checkpoint
    val checkpointDir = "/Users/nilanjan1.sarkar/sparkstreaming/checkpoint"
    
    val query = windowedCounts
      .writeStream
      .outputMode("append")
      .option("truncate", "false")
      .option("checkpointLocation", checkpointDir)
      .format("console")
      .queryName("kafkawordcount")
      .start()

    query.awaitTermination()
  }
}