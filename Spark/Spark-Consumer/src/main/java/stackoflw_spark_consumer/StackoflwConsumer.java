package stackoflw_spark_consumer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.evaluation.ClusteringEvaluator;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.types.DataTypes.*;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import scala.Tuple2;

public class StackoflwConsumer {

	public static void main(String[] args) throws InterruptedException {
		
		//Logger log = Logger.getLogger(StackoflwConsumer.class);
		
		SparkInstance sparkInstance = SparkInstance.getInstance();
		SparkSession spark = sparkInstance.getSparkSession();
		JavaStreamingContext jsc = new JavaStreamingContext(
				JavaSparkContext.fromSparkContext(spark.sparkContext()), Durations.seconds(5));
		
		StructType schema = createStructType(new StructField[]{
              createStructField("siteBaseHostAddress", StringType, false),
              createStructField("id", LongType, false),
              createStructField("titleEncodedFancy", StringType, false),
              createStructField("bodySummary", StringType, false),
              createStructField("tags", DataTypes.createArrayType(StringType), false),
              createStructField("lastActivityDate", TimestampType, false),
              createStructField("url", StringType, false),
              createStructField("ownerUrl", StringType, false),
              createStructField("ownerDisplayName", StringType, false),
              createStructField("apiSiteParameter", StringType, false)
            });
		
		Dataset<Row> trainingDataset = sparkInstance.getTrainingDataset("trainingDataset.txt", schema);
		PipelineInstance.getInstance().fit(trainingDataset);
		Dataset<Row> featuresTrainingDataset = PipelineInstance.getInstance().transform(trainingDataset);
		KmeansInstance.getInstance().fit(featuresTrainingDataset);
		
		//Dataset<Row> predictTraining = KmeansInstance.getInstance().transform(featuresTrainingDataset);
		
		/* Silhouette score
		predictTraining.select("prediction").groupBy("prediction").count().show(false);
		ClusteringEvaluator evaluator = new ClusteringEvaluator();
		double sil = evaluator.evaluate(predictTraining);
		log.error("sil: " + sil);*/
		
		JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
				jsc, 
				LocationStrategies.PreferConsistent(), 
				ConsumerStrategies.Subscribe(getTopicSet(), getKafkaParams()));
		
		directStream.mapToPair(record -> new Tuple2<>(record.key(), record.value())).map(tuple2 -> tuple2._2)
					.foreachRDD(rdd -> predictDataset(rdd));
		
		jsc.start();
		jsc.awaitTermination();
	}
	
	private static Map<String, Object> getKafkaParams() {
		String brokers = "10.0.100.25:9092";
		String groupId = "connect-cluster";
		
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		return kafkaParams;
	}
	
	private static Set<String> getTopicSet() {
		String kafkaTopic = "stackoverflow";
		
		Set<String> topicSet = Collections.singleton(kafkaTopic);
		return topicSet;
	}
	
	private static void predictDataset(JavaRDD<String> rdd) {
		if(rdd.isEmpty()) return;
		Dataset<Row> dataset = SparkInstance.getInstance().convertRddToDataset(rdd);
		Dataset<Row> featureDataset = PipelineInstance.getInstance().transform(dataset);
		Dataset<Row> predictionDataset = KmeansInstance.getInstance().transform(featureDataset);
		sendToEs(predictionDataset);
	}
	
	private static void sendToEs(Dataset<Row> dataset) {
		JavaEsSpark.saveJsonToEs(dataset.toJSON().toJavaRDD(), "es/stackoflw");
	}
}
