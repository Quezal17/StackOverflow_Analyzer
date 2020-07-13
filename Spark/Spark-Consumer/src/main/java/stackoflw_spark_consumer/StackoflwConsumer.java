package stackoflw_spark_consumer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

public class StackoflwConsumer {

	public static void main(String[] args) throws InterruptedException {
		
		String brokers = "10.0.100.25:9092";
		String groupId = "connect-cluster";
		String kafkaTopic = "stackoverflow";
		Logger log = Logger.getLogger(StackoflwConsumer.class);
		
		//SparkConf sparkConf = new SparkConf().setAppName("StackoflwSparkConsumer").setMaster("local[2]");
		SparkSession sparkSession = SparkSession.builder().appName("StackoflwSparkConsumer").master("local[2]").getOrCreate();
		JavaStreamingContext streamingSparkConf = new JavaStreamingContext(
				JavaSparkContext.fromSparkContext(sparkSession.sparkContext()), Durations.seconds(5));
		//Set<String> topicSet = new HashSet<>(Arrays.asList(kafkaTopics.split(",")));
		Set<String> topicSet = Collections.singleton(kafkaTopic);
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		
		JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
				streamingSparkConf, 
				LocationStrategies.PreferConsistent(), 
				ConsumerStrategies.Subscribe(topicSet, kafkaParams));
		
		JavaDStream<String> lines = directStream.map(x -> x.value());
		/*List<Row> data = new ArrayList<>();
		
		lines.foreachRDD(rdd -> {data.add(RowFactory.create(rdd));});
		StructType schema = DataTypes.createStructType(new StructField[] {DataTypes.createStructField("question", DataTypes.StringType, false)});
		Dataset<Row> dataset = sparkSession.createDataFrame(data, schema);
		//dataset.show();
		dataset.write().format("com.databricks.spark.csv").save("dataset.csv");
		*/
		lines.foreachRDD(x -> {
			x.collect().stream().forEach(n -> log.info("Read from Topic: " + n));
		});
		
		streamingSparkConf.start();
		streamingSparkConf.awaitTermination();

	}

}
