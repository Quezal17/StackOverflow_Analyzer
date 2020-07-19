package stackoflw_spark_consumer;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.evaluation.ClusteringEvaluator;
import org.apache.spark.ml.feature.RegexTokenizer;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.Word2Vec;
import org.apache.spark.ml.feature.Word2VecModel;
import org.apache.spark.ml.linalg.Vector;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.types.DataTypes.*;

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
import org.elasticsearch.spark.rdd.api.java.JavaEsSpark;

import scala.Tuple2;

public class StackoflwConsumer {

//	private static SparkSession sparkSession;
//	public static int count;
	public static void main(String[] args) throws InterruptedException {
		
		Logger log = Logger.getLogger(StackoflwConsumer.class);
		
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
		
		Dataset<Row> predictTraining = KmeansInstance.getInstance().transform(featuresTrainingDataset);
		predictTraining.select("prediction").groupBy("prediction").count().show(false);
		ClusteringEvaluator evaluator = new ClusteringEvaluator();
		double sil = evaluator.evaluate(predictTraining);
		log.error("sil: " + sil);
		
		JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
				jsc, 
				LocationStrategies.PreferConsistent(), 
				ConsumerStrategies.Subscribe(getTopicSet(), getKafkaParams()));
		
		directStream.mapToPair(record -> new Tuple2<>(record.key(), record.value())).map(tuple2 -> tuple2._2)
					.foreachRDD(rdd -> predictDataset(rdd));
		
		jsc.start();
		jsc.awaitTermination();
				
//		String brokers = "10.0.100.25:9092";
//		String groupId = "connect-cluster";
//		String kafkaTopic = "stackoverflow";
//		//Logger log = Logger.getLogger(StackoflwConsumer.class);
//		
//		SparkConf sparkConf = new SparkConf().setAppName("StackoflwSparkConsumer").setMaster("local[2]");
//		sparkConf.set("es.index.auto.create", "true");
//		sparkConf.set("es.nodes", "10.0.100.51");
//		sparkConf.set("es.resource", "es/stackoflw");
//		sparkConf.set("es.input.json", "yes");
//		
//		sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
//		sparkSession.sparkContext().setLogLevel("ERROR");
//		JavaStreamingContext streamingSparkContext = new JavaStreamingContext(
//				JavaSparkContext.fromSparkContext(sparkSession.sparkContext()), Durations.seconds(5));
//		
//		Set<String> topicSet = Collections.singleton(kafkaTopic);
//		Map<String, Object> kafkaParams = new HashMap<>();
//		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
//		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
//		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
//		kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
//		
//		StructType schema = createStructType(new StructField[]{
//                createStructField("siteBaseHostAddress", StringType, false),
//                createStructField("id", LongType, false),
//                createStructField("titleEncodedFancy", StringType, false),
//                createStructField("bodySummary", StringType, false),
//                createStructField("tags", DataTypes.createArrayType(StringType), false),
//                createStructField("lastActivityDate", TimestampType, false),
//                createStructField("url", StringType, false),
//                createStructField("ownerUrl", StringType, false),
//                createStructField("ownerDisplayName", StringType, false),
//                createStructField("apiSiteParameter", StringType, false)
//              });
//		
//		Dataset<Row> trainingDataset = sparkSession.read().schema(schema).json(Paths.get("trainingDataset.txt").toString());
//		Dataset<Row> featureTrainingDataset = getFeatureDataset(trainingDataset,0,0);
//		featureTrainingDataset.select("features").show(5, false);
//		KMeans kmeans = new KMeans().setK(9).setSeed(10);
//		KMeansModel kmeansModel = kmeans.fit(featureTrainingDataset);
//		
//		/*Calculate silhouette score
//		Dataset<Row> predictions = kmeansModel.transform(featureTrainingDataset);
//		ClusteringEvaluator evaluator = new ClusteringEvaluator();
//		double sil = evaluator.evaluate(predictions);
//		log.error("sil: " + sil);
//		 */
//		
//		JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
//				streamingSparkContext, 
//				LocationStrategies.PreferConsistent(), 
//				ConsumerStrategies.Subscribe(topicSet, kafkaParams));
//		
//		directStream.mapToPair(record -> new Tuple2<>(record.key(), record.value())).map(tuple2 -> tuple2._2)
//					.foreachRDD(rdd -> predictDataset(rdd, kmeansModel));
//		
//		streamingSparkContext.start();
//		streamingSparkContext.awaitTermination();

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
	
	/*private static Dataset<Row> getVectorizedDataset(Dataset<Row> trainingDataset, int titleMinCount, int bodyMinCount) {
		RegexTokenizer titleTokenizer = new RegexTokenizer()
				.setInputCol("titleEncodedFancy")
				.setOutputCol("titleTokenized")
				.setPattern("\\W");
		
		RegexTokenizer bodyTokenizer = new RegexTokenizer()
				.setInputCol("bodySummary")
				.setOutputCol("bodyTokenized")
				.setPattern("\\W");
		
		StopWordsRemover titleStopWordsRemover = new StopWordsRemover()
				.setInputCol("titleTokenized")
				.setOutputCol("titleCleaned");
		
		StopWordsRemover bodyStopWordsRemover = new StopWordsRemover()
				.setInputCol("bodyTokenized")
				.setOutputCol("bodyCleaned");
		
		Word2Vec title2vec = new Word2Vec()
				.setInputCol("titleCleaned")
				.setOutputCol("title2vec")
				.setVectorSize(100)
				.setMinCount(titleMinCount);
		
		Word2Vec body2vec = new Word2Vec()
				.setInputCol("bodyCleaned")
				.setOutputCol("body2vec")
				.setVectorSize(100)
				.setMinCount(bodyMinCount);
		
		Word2Vec tags2vec = new Word2Vec()
				.setInputCol("tags")
				.setOutputCol("tags2vec")
				.setVectorSize(100)
				.setMinCount(0);
		
		PipelineStage[] stages = {titleTokenizer, bodyTokenizer, titleStopWordsRemover, bodyStopWordsRemover, title2vec, body2vec, tags2vec};  
		Pipeline pipeline = new Pipeline().setStages(stages);
		PipelineModel pipelineModel = pipeline.fit(trainingDataset);
		Dataset<Row> vectorizedDataset = pipelineModel.transform(trainingDataset);
		return vectorizedDataset;
	}
	
	private static Dataset<Row> getFeatureDataset(Dataset<Row> trainingDataset, int titleMinCount, int bodyMinCount) {
		Dataset<Row> vectorizedDataset = getVectorizedDataset(trainingDataset, titleMinCount, bodyMinCount);
		VectorAssembler assembler = new VectorAssembler()
		        .setInputCols(new String[] {"tags2vec", "title2vec", "body2vec"})
		        .setOutputCol("features");
		Dataset<Row> featureDataset = assembler.transform(vectorizedDataset);
		return featureDataset;
	}*/
	
//	private static void sendToEs(Dataset<Row> dataset) {
//		JavaEsSpark.saveJsonToEs(dataset.toJSON().toJavaRDD(), "es/stackoflw");
//	}
	
	/*private static void predictDataset(JavaRDD<String> rdd, KMeansModel kmeansModel) {
		if(rdd.isEmpty()) return;
		Dataset<Row> dataset = sparkSession.read().json(rdd);
		Dataset<Row> featureDataset = getFeatureDataset(dataset,0,0);
		Dataset<Row> predictionDataset = kmeansModel.transform(featureDataset);
		sendToEs(predictionDataset);
	}*/


}
