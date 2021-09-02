# Watermark Demonstration with ksqlDB and Spark Structured Streaming

## Set up

**Open ksqlDB CLI**

```bash
sudo docker exec -it ksqldb-cli ksql http://ksqldb-server:8088
```

**Create a new Stream**

```sql
CREATE STREAM lines (
ts bigint,
content string
)
WITH (
kafka_topic='lines', 
value_format='json', 
partitions=1);

-- Verify if the topic was created
SHOW TOPICS;
```

## Timestamp format

 1581098400 ==> Feb  7, 2020 18:00:00 GMT
 
 1581098420 ==> Feb  7, 2020 18:00:20 GMT
 
 The last three digits are the significant ones
 
# Start Demo

* Edit the file `KafkaWaterMarking.scala` and change the following :
  - your kafka credentials
  - your local checkpoint directory

* Then compile the project and run the file `SparkSSLauncher.scala`

**Open ksqlDB CLI**

```bash
sudo docker exec -it ksqldb-cli ksql http://ksqldb-server:8088
```

**Add first round of messages**

```sql
INSERT INTO lines (ts, content) VALUES (1581098400,'apple orange');
INSERT INTO lines (ts, content) VALUES (1581098403,'melon pear');
INSERT INTO lines (ts, content) VALUES (1581098406,'apple orange');
INSERT INTO lines (ts, content) VALUES (1581098409,'melon pear');
INSERT INTO lines (ts, content) VALUES (1581098412,'apple orange');
```

**Caveat :** Currently it seems that the underlying json produced by the ksqlDB INSERT statements have keys in uppercase, so this needs to be factored in Spark Code while declaring Schema

Sample data -

```json
{"TS":1581098412,"CONTENT":"apple orange"}
```

**Initial Output**

```bash
-------------------------------------------
Batch: 1
-------------------------------------------
+------+----+-----+
|window|word|count|
+------+----+-----+
+------+----+-----+

-------------------------------------------
Batch: 2
-------------------------------------------
+------------------------------------------+------+-----+
|window                                    |word  |count|
+------------------------------------------+------+-----+
|{2020-02-07 17:59:55, 2020-02-07 18:00:05}|melon |1    |
|{2020-02-07 17:59:55, 2020-02-07 18:00:05}|orange|1    |
|{2020-02-07 17:59:55, 2020-02-07 18:00:05}|pear  |1    |
|{2020-02-07 17:59:55, 2020-02-07 18:00:05}|apple |1    |
+------------------------------------------+------+-----+
```

Notice that counts for each word is shown as one, despite the words being present multiple time. 

The following records are not counted as part of the output

```sql
INSERT INTO lines (ts, content) VALUES (1581098406,'apple orange');
INSERT INTO lines (ts, content) VALUES (1581098409,'melon pear');
INSERT INTO lines (ts, content) VALUES (1581098412,'apple orange');
```

If the max ts of message is M and late threshold is L then window till (M - L) is finalized. 

The watermark at this point is at 18:00:07 (1581098412 - 5 sec = 1581098407), so only windows whose end till this point will be considered. 

But, since the slide interval is 5 second the window will only be till 18:00:05 and hence the records 1581098406(18:00:06),1581098409(18:00:09) and 1581098412(18:00:07)
are not considered for computation

* Now if you add 

```sql
INSERT INTO lines (ts, content) VALUES (1581098415,'melon pear');
```

The output becomes -

```bash
-------------------------------------------
Batch: 3
-------------------------------------------
+------+----+-----+
|window|word|count|
+------+----+-----+
+------+----+-----+

-------------------------------------------
Batch: 4
-------------------------------------------

+------------------------------------------+------+-----+
|window                                    |word  |count|
+------------------------------------------+------+-----+
|{2020-02-07 18:00:00, 2020-02-07 18:00:10}|melon |2   |
|{2020-02-07 18:00:00, 2020-02-07 18:00:10}|pear  |2    |
|{2020-02-07 18:00:00, 2020-02-07 18:00:10}|orange|2    |
|{2020-02-07 18:00:00, 2020-02-07 18:00:10}|apple |2    |
+------------------------------------------+------+-----+
````

The watermark at this point is at 18:00:10 (1581098415 - 5 sec = 1581098410), so windows till 18:00:10 are considered. So the records 1581098406(18:00:06),1581098409(18:00:09) and 1581098412(18:00:07) are now factored in


* Next, adding the below records will have no impact

```sql
INSERT INTO lines (ts, content) VALUES (1581098417,'apple orange');
INSERT INTO lines (ts, content) VALUES (1581098419,'melon pear');
```

- watermark will be at     : 18:00:14 (1581098419 - 5 = 1581098414 )
- and the is new window is : 18:00:05 - 18:00:15

* Let's try emulating late data. The current watermark is at 18:00:10 (1581098415 - 5 sec = 1581098410). Let's add a data with timestamp earlier (18:00:09) than the watermark. 

```sql
INSERT INTO lines (ts, content) VALUES (1581098409,'melon pear');
```

As you'll notice in the output,the addition of this record will have no impact on the word count of window 18:00:00 - 18:00:10 (as it is before the watermark)

This record will however be factored into the the window of 18:00:05 - 18:00:15 (once that window is finalized)

* Now, let's finalize the window of 18:00:05 - 18:00:15 by adding the following record - 

```sql
INSERT INTO lines (ts, content) VALUES (1581098420,'apple orange');
```

**Final Output**

```bash
-------------------------------------------
Batch: 7
-------------------------------------------
+------+----+-----+
|window|word|count|
+------+----+-----+
+------+----+-----+

-------------------------------------------
Batch: 8
-------------------------------------------
+------------------------------------------+------+-----+
|window                                    |word  |count|
+------------------------------------------+------+-----+
|{2020-02-07 18:00:05, 2020-02-07 18:00:15}|apple |2    |
|{2020-02-07 18:00:05, 2020-02-07 18:00:15}|melon |2    |
|{2020-02-07 18:00:05, 2020-02-07 18:00:15}|pear  |2    |
|{2020-02-07 18:00:05, 2020-02-07 18:00:15}|orange|2    |
+------------------------------------------+------+-----+
```

## See all the data produced through ksqlDB

```sql
-- See data in tabular form 
SET 'auto.offset.reset' = 'earliest';
SELECT * FROM lines EMIT CHANGES;

-- See raw data in kafka topic
PRINT lines FROM BEGINNING LIMIT 20;
```

## Tear down

* Delete kafka topic as a part of cleanup activity
 
```bash
-- Cleanup
DROP STREAM lines DELETE TOPIC;

-- Verify if the topic was dropped
SHOW TOPICS;
```

References
==========
https://www.youtube.com/watch?v=_2hrM6woMes&list=PLHJp-gMPHvp_YDqkmQqPF2M1M7C1-otdC&index=9

https://drive.google.com/drive/folders/1c_YKl7qBs3c9eviKE6xcrr_ILar1qvol

https://colab.research.google.com/drive/1WLAo5cDSsEKcr1MzgGktgWWfzVAh7SvB#scrollTo=R4DCA79nmx95