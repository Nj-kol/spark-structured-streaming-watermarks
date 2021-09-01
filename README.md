# Watermarks Demo : Spark Structured Streaming 

Contains several demos of Spark Structured Streaming written in Scala. The code is mostly a translation of wonderful demos in PySpark by Amit Ranjan which you can find [here](https://www.youtube.com/playlist?list=PLHJp-gMPHvp_YDqkmQqPF2M1M7C1-otdC) adapted for a local environment instead of the cloud

Programs -

* Demonstrate Watermarks with Kafka
* Join two Kafka topics with Watermarks

## Kafka Cluster Setup

See README under the ./kafka folder

**Launch the Cluster**

```bash
cd kafka

# Run in a detached mode
docker-compose up -d
```

**Usage**

* Kafka

```
docker exec -it broker bash
```

* Try ksqlDb

```
docker exec -it ksqldb-cli ksql http://ksqldb-server:8088
```

**Teardown the Cluster**

```bash
cd kafka

docker-compose down
```
