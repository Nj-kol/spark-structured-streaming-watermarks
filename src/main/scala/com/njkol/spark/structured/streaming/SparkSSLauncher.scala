package com.njkol.spark.structured.streaming

import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession
import org.apache.spark.SparkConf
import org.apache.spark.sql.SQLContext

import com.njkol.spark.structured.streaming.kafka._

/**
 * @author Nilanjan Sarkar
 *
 * A template project to demonstrate structured streaming
 */
object SparkSSLauncher extends App {

  val spark = SparkSession
    .builder()
    .master("local[*]")
    .appName("Spark Structured Streaming Demo")
    .getOrCreate()
    
  spark.sparkContext.setLogLevel("ERROR")
  spark.conf.set("spark.sql.session.timeZone", "GMT")
   
  // run demo for Watermarking
  val tester = new KafkaWaterMarking(spark)
  tester.runJob()
}