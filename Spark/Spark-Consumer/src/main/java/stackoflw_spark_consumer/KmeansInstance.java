package stackoflw_spark_consumer;

import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class KmeansInstance {
	private static KmeansInstance instance;
	private static KMeans kmeans;
	private static KMeansModel kmeansModel;
	
	static {
		kmeans = new KMeans().setK(10).setSeed(10);
		instance = new KmeansInstance();
	}
	
	public static KmeansInstance getInstance() {
		return instance;
	}
	
	public void fit(Dataset<Row> dataset) {
		kmeansModel = kmeans.fit(dataset);
	}
	
	public Dataset<Row> transform(Dataset<Row> dataset) {
		return kmeansModel.transform(dataset);
	}
}