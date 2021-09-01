# Confluent ksqlDB stack

This ksqlDB stack brings up these services:

* ZooKeeper: one instance
* Kafka: one broker
* Schema Registry: enables Avro, Protobuf, and JSON_SR
* ksqlDB Server: one instance
* ksqlDB CLI: one container running /bin/sh

## Bring up the stack

**Customize**

```bash
cd kafka

vim docker-compose.yaml
```

* You need to edit the line `KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://10.259.35.27:29092`. Change the IP address from `10.259.35.27` to the IP of your system
* You can also add your your custom volumes. THhe compose file does not contain any volumes and thus all kafka topics (& all other data) go away once it's shut down

```bash
cd <location_of_compose_file_directory>

sudo /usr/local/bin/docker-compose up -d
```

## Verify Installation

**Login to the ksqlDB CLI**

```bash
sudo docker exec -it ksqldb-cli ksql http://ksqldb-server:8088
```

**Create a Stream**

```sql
CREATE STREAM riderLocations (
profileId VARCHAR, 
latitude DOUBLE, 
longitude DOUBLE)
WITH (
kafka_topic='locations', 
value_format='avro', 
partitions=1);

SHOW TOPICS;

INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('c2309eec', 37.7877, -122.4205);
INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('18f4ea86', 37.3903, -122.0643);
INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('4ab5cbad', 37.3952, -122.0813);
INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('8b6eae59', 37.3944, -122.0813);
INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('4a7c7b41', 37.4049, -122.0822);
INSERT INTO riderLocations (profileId, latitude, longitude) VALUES ('4ddad000', 37.7857, -122.4011);
```

**See the data in the underlying Kafka topic**

```bash
sudo docker exec -it broker bash

kafka-console-consumer \
--bootstrap-server broker:9092 \
--topic locations \
--from-beginning 

c2309eec���Z��B@���x�^�
18f4ea86}гY�B@�H�}�^�
4ab5cbad���镲B@�J�4�^�
8b6eae59m���{�B@�J�4�^�
4a7c7b41@a�ӳB@����B�^�
4ddad000�;Nё�B@=�U���^�
````

The above binary data confirms data written in Avro format

## Tear down the stack

```bash
sudo /usr/local/bin/docker-compose down
```