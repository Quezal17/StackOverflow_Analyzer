package stackoflw_spark_consumer;

import java.nio.file.Paths;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.StructType;

public class SparkInstance {
	private static SparkSession spark;
	private static SparkInstance instance;
	
	static {
		spark = SparkSession.builder().config(getSparkConf()).getOrCreate();
		spark.sparkContext().setLogLevel("ERROR");
		instance = new SparkInstance();
	}
	
	private static SparkConf getSparkConf() {
		SparkConf sparkConf = new SparkConf().setAppName("StackoflwSparkConsumer").setMaster("local[2]");
		sparkConf.set("es.index.auto.create", "true");
		sparkConf.set("es.nodes", "10.0.100.51");
		sparkConf.set("es.resource", "es/stackoflw");
		sparkConf.set("es.input.json", "yes");
		return sparkConf;
	}
	
	public static SparkInstance getInstance() {
		return instance;
	}
	
	public SparkSession getSparkSession() {
		return spark;
	}
	
	public Dataset<Row> getTrainingDataset(String fileName, StructType schema) {
		return spark.read().schema(schema).json(Paths.get(fileName).toString());
	}
	
	public Dataset<Row> convertRddToDataset(JavaRDD<String> rdd) {
		return spark.read().json(rdd);
	}
}