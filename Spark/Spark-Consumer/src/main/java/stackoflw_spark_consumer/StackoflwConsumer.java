package stackoflw_spark_consumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
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
		
		SparkConf sparkConf = new SparkConf().setAppName("StackoflwSparkConsumer").setMaster("local[2]");
		JavaStreamingContext streamingSparkConf = new JavaStreamingContext(sparkConf, Durations.seconds(2));
		
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
		
		JavaDStream<String> lines = directStream.map(ConsumerRecord::value);
		lines.foreachRDD(rdd -> {log.info(rdd);});
		
		streamingSparkConf.start();
		streamingSparkConf.awaitTermination();

	}

}
