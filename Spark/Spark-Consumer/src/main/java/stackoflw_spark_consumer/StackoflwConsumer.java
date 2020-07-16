package stackoflw_spark_consumer;

import java.nio.file.Paths;
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
import org.apache.spark.ml.feature.StringIndexer;
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
import org.json.JSONArray;
import org.json.JSONObject;




import scala.Tuple2;
import shapeless.newtype;

public class StackoflwConsumer {

	private static SparkSession sparkSession;
	public static int count;
	public static void main(String[] args) throws InterruptedException {
		
		String brokers = "10.0.100.25:9092";
		String groupId = "connect-cluster";
		String kafkaTopic = "stackoverflow";
		Logger log = Logger.getLogger(StackoflwConsumer.class);
		
		SparkConf sparkConf = new SparkConf().setAppName("StackoflwSparkConsumer").setMaster("local[2]");
		sparkConf.set("es.index.auto.create", "true");
		sparkConf.set("es.nodes", "10.0.100.51");
		sparkConf.set("es.resource", "es/stackoflw");
		sparkConf.set("es.input.json", "yes");
		
		sparkSession = SparkSession.builder().config(sparkConf).getOrCreate();
		sparkSession.sparkContext().setLogLevel("ERROR");
		JavaStreamingContext streamingSparkContext = new JavaStreamingContext(
				JavaSparkContext.fromSparkContext(sparkSession.sparkContext()), Durations.seconds(5));
		
		Set<String> topicSet = Collections.singleton(kafkaTopic);
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
		kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
		
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
		
		Dataset<Row> trainingDataset = sparkSession.read().schema(schema).json(Paths.get("trainingDataset.txt").toString());
		
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
				.setVectorSize(50)
				.setMinCount(1);
		
		Word2Vec body2vec = new Word2Vec()
				.setInputCol("bodyCleaned")
				.setOutputCol("body2vec")
				.setVectorSize(50)
				.setMinCount(1);
		
		Word2Vec tags2vec = new Word2Vec()
				.setInputCol("tags")
				.setOutputCol("tags2vec")
				.setVectorSize(50)
				.setMinCount(1);
		
		PipelineStage[] stages = {titleTokenizer, bodyTokenizer, titleStopWordsRemover, bodyStopWordsRemover, title2vec, body2vec, tags2vec};  
		Pipeline pipeline = new Pipeline()
				.setStages(stages);
		PipelineModel pipelineModel = pipeline.fit(trainingDataset);
		Dataset<Row> newTrainingDataset = pipelineModel.transform(trainingDataset);
		
		//trainingDataset = new StringIndexer().setInputCol("tag").setOutputCol("tagIndex").fit(trainingDataset).transform(trainingDataset);
		//trainingDataset = new StringIndexer().setInputCol("siteParameter").setOutputCol("siteIndex").fit(trainingDataset).transform(trainingDataset);
		
		VectorAssembler assembler = new VectorAssembler()
		        .setInputCols(new String[] {"tags2vec", "title2vec", "body2vec"})
		        .setOutputCol("features");
		Dataset<Row> finalDS = assembler.transform(newTrainingDataset);
		
		KMeans kmeans = new KMeans().setK(10).setSeed(1L);
		KMeansModel kmeansModel = kmeans.fit(finalDS);
		
		Dataset<Row> predictions = kmeansModel.transform(finalDS);
		predictions.select("tags", "tags2vec", "prediction").show(false);
		
		ClusteringEvaluator evaluator = new ClusteringEvaluator();
		
		double sil = evaluator.evaluate(predictions);
		log.error("sil: " + sil);
		
		kmeansModel.clusterCenters();
		
		//createStructField("questionId", DataTypes.createArrayType(StringType), false);
		
		/*StructType schema = createStructType(new StructField[]{
                createStructField("questionId", IntegerType, false),
                createStructField("tag", StringType, false),
                createStructField("siteParameter", StringType, false)
              });
		
		Dataset<Row> trainingDataset = sparkSession.read().format("csv")
				.option("header", true)
				.option("delimiter", ",")
				.option("mode", "DROPMALFORMED")
				.schema(schema)
				.load(Paths.get("trainingDataset.csv").toString())
				.cache();
		
		trainingDataset = new StringIndexer().setInputCol("tag").setOutputCol("tagIndex").fit(trainingDataset).transform(trainingDataset);
		trainingDataset = new StringIndexer().setInputCol("siteParameter").setOutputCol("siteIndex").fit(trainingDataset).transform(trainingDataset);
		
		VectorAssembler assembler = new VectorAssembler()
		        .setInputCols(new String[] {"tagIndex", "siteIndex"})
		        .setOutputCol("features");
		Dataset<Row> finalDS = assembler.transform(trainingDataset);
		finalDS.show();
		
		KMeans kmeans = new KMeans()
				.setK(5)
				.setFeaturesCol("features")
				.setPredictionCol("prediction");
		KMeansModel kmeansModel = kmeans.fit(finalDS);
		kmeansModel.clusterCenters();
		
		List<Row> testList = Arrays.asList(
                RowFactory.create(13, "javascript", "ru.stackoverflow"),
                RowFactory.create(13, "html", "ru.stackoverflow"),
                RowFactory.create(18, "css", "stackoverflow"),
                RowFactory.create(20, "mysql", "ru.stackoverflow"),
                RowFactory.create(22, "mysql-server", "es.stackoverflow"),
                RowFactory.create(30, "java", "ru.stackoverflow"),
                RowFactory.create(35, "python", "stackoverflow")
          );
		
		Dataset<Row> testDataset = sparkSession.createDataFrame(testList, schema);
		testDataset = new StringIndexer().setInputCol("tag").setOutputCol("tagIndex").fit(testDataset).transform(testDataset);
		testDataset = new StringIndexer().setInputCol("siteParameter").setOutputCol("siteIndex").fit(testDataset).transform(testDataset);
		
		VectorAssembler assembler2 = new VectorAssembler()
		        .setInputCols(new String[] {"tagIndex", "siteIndex"})
		        .setOutputCol("features");
		Dataset<Row> finalTest = assembler2.transform(testDataset);
		Dataset<Row> predictionDataset = kmeansModel.transform(finalTest);
		predictionDataset.show();
		*/
		//count=0;
		/*JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
				streamingSparkContext, 
				LocationStrategies.PreferConsistent(), 
				ConsumerStrategies.Subscribe(topicSet, kafkaParams));
		
		directStream.mapToPair(record -> new Tuple2<>(record.key(), record.value())).map(tuple2 -> tuple2._2)
					.foreachRDD(rdd -> addToTrainingDatasetCSV(rdd));
					*/
		//.foreachRDD(rdd -> sendToEs(rdd));
		
		//.foreachRDD(rdd -> clusteringData(rdd, schema, kmeansModel));
		
		
		//streamingSparkContext.start();
		//streamingSparkContext.awaitTermination();

	}
	
	private static void addToTrainingDatasetCSV (JavaRDD<String> rdd) {
		if(rdd.isEmpty()) return;
		Dataset<Row> dataset = sparkSession.read().json(rdd);
		Logger.getLogger(StackoflwConsumer.class).info("question number: " + count);
		dataset.write().format("csv")
		.mode(SaveMode.Append)
		.save(Paths.get("trainingDataset.csv").toString());
	}
	
	private static void sendToEs(JavaRDD<String> rdd) {
		if(rdd.isEmpty()) return;
		Dataset<Row> dataset = sparkSession.read().json(rdd);
		dataset.show();
		JavaEsSpark.saveJsonToEs(dataset.toJSON().toJavaRDD(), "es/stackoflw");
	}

	private static void clusteringData(JavaRDD<String> rdd, StructType schema, KMeansModel kmeansModel) {
		if(rdd.count() <= 0) return;
		List<String> dataStringList = rdd.collect();
		List<Row> dataRow = new ArrayList<>();
		for(int i=0; i<dataStringList.size(); i++) {
			JSONObject questionJson = new JSONObject(dataStringList.get(i));
			JSONArray tagsArray = questionJson.getJSONArray("tags");
			for(int j=0; j<tagsArray.length(); j++) {
				dataRow.add(RowFactory.create(questionJson.getLong("id"), tagsArray.get(j), questionJson.getString("apiSiteParameter")));
			}
		}
		Dataset<Row> testDataset = sparkSession.createDataFrame(dataRow, schema);
		Dataset<Row> predictionDataset = kmeansModel.transform(testDataset);
		predictionDataset.show();
		
		predictionDataset.groupBy("questionId").count().show();
		
	}

}
