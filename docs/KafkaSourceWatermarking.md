
## Create Topics

sudo docker exec -it broker bash

```bash
kafka-topics \
--create \
--bootstrap-server broker:9092 \
--replication-factor 1 \
--partitions 1 \
--topic lines
```

## Timestamp format

 1581098400 ==> Feb  7, 2020 18:00:00 GMT
 
 1581098420 ==> Feb  7, 2020 18:00:20 GMT
 
 The last three digits are the significant ones
 
# Launch a Producer

```bash
kafka-console-producer \
--broker-list broker:9092 \
--topic lines
```

**Add first round of messages**

```json
{"ts": 1581098400, "content": "apple orange"}
{"ts": 1581098403, "content": "melon pear"}
{"ts": 1581098406, "content": "apple orange"}
{"ts": 1581098409, "content": "melon pear"}
{"ts": 1581098412, "content": "apple orange"}
```

**Output**

```
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

```json
{"ts": 1581098406, "content": "apple orange"}
{"ts": 1581098409, "content": "melon pear"}
{"ts": 1581098412, "content": "apple orange"}
```

If the max ts of message is M and late threshold is L then window till (M - L) is finalized. 

The watermark at this point is at 18:00:07 (1581098412 - 5 sec = 1581098407), so only windows whose end till this point will be considered. 

But, since the slide interval is 5 second the window will only be till 18:00:05 and hence the records 1581098406(18:00:06),1581098409(18:00:09) and 1581098412(18:00:07)
are not considered for computation

* Now if you add 

```json
{"ts": 1581098415, "content": "melon pear"}
```


The output becomes

```
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

```json
{"ts": 1581098417, "content": "apple orange"}
{"ts": 1581098419, "content": "melon pear"}
```

As  
- watermark will be at     : 18:00:14 (1581098419 - 5 = 1581098414 )
- and the is new window is : 18:00:05 - 18:00:15

But this record will cause the finalization

```json
{"ts": 1581098420, "content": "apple orange"}
```

# Delete the kafka topic so as a cleanup activity

```bash
kafka-topics \
--bootstrap-server ec2-52-66-45-236.ap-south-1.compute.amazonaws.com:9092 \
--delete \
--topic lines
```

References
==========
https://www.youtube.com/watch?v=_2hrM6woMes&list=PLHJp-gMPHvp_YDqkmQqPF2M1M7C1-otdC&index=9

https://drive.google.com/drive/folders/1c_YKl7qBs3c9eviKE6xcrr_ILar1qvol

https://colab.research.google.com/drive/1WLAo5cDSsEKcr1MzgGktgWWfzVAh7SvB#scrollTo=R4DCA79nmx95
