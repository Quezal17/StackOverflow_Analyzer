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
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import static org.apache.spark.sql.types.DataTypes.*;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.feature.StringIndexer;
import org.apache.spark.ml.feature.VectorAssembler;


import scala.Tuple2;

public class StackoflwConsumer {

	private static SparkSession sparkSession;
	public static void main(String[] args) throws InterruptedException {
		
		String brokers = "10.0.100.25:9092";
		String groupId = "connect-cluster";
		String kafkaTopic = "stackoverflow";
		Logger log = Logger.getLogger(StackoflwConsumer.class);
		
		sparkSession = SparkSession.builder().appName("StackoflwSparkConsumer").master("local[2]").getOrCreate();
		JavaStreamingContext streamingSparkContext = new JavaStreamingContext(
				JavaSparkContext.fromSparkContext(sparkSession.sparkContext()), Durations.seconds(5));
		sparkSession.sparkContext().setLogLevel("ERROR");
		
		Set<String> topicSet = Collections.singleton(kafkaTopic);
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		
		StructType schema = createStructType(new StructField[]{
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
		
		predictionDataset.groupBy("questionId").count().show();
		/*JavaInputDStream<ConsumerRecord<String, String>> directStream = KafkaUtils.createDirectStream(
				streamingSparkContext, 
				LocationStrategies.PreferConsistent(), 
				ConsumerStrategies.Subscribe(topicSet, kafkaParams));
		
		directStream.mapToPair(record -> new Tuple2<>(record.key(), record.value())).map(tuple2 -> tuple2._2)
		.foreachRDD(rdd -> clusteringData(rdd, schema, kmeansModel));
		*/
		
		//streamingSparkContext.start();
		//streamingSparkContext.awaitTermination();

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
