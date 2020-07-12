sleep 15s
/opt/spark/bin/spark-submit --class stackoflw_spark_consumer.StackoflwConsumer --master local[2] /opt/code/Spark-Consumer-0.0.1-SNAPSHOT.jar
