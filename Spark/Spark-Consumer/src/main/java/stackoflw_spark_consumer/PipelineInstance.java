package stackoflw_spark_consumer;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.RegexTokenizer;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.feature.Word2Vec;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

public class PipelineInstance {
	private static PipelineInstance instance;
	private static Pipeline pipeline;
	private static PipelineModel pipelineModel;
	
	static {
		instance = new PipelineInstance();
		pipeline = new Pipeline().setStages(getStages());
	}
	
	private static PipelineStage[] getStages() {
		RegexTokenizer titleTokenizer = new RegexTokenizer()
				.setInputCol("titleEncodedFancy")
				.setOutputCol("titleTokenized")
				.setPattern("\\W");
		
		RegexTokenizer bodyTokenizer = new RegexTokenizer()
				.setInputCol("bodySummary")
				.setOutputCol("bodyTokenized")
				.setPattern("\\W");
		
		StopWordsRemover titleStopWordsRemover = new StopWordsRemover()
				.setInputCol(titleTokenizer.getOutputCol())
				.setOutputCol("titleCleaned");
		
		StopWordsRemover bodyStopWordsRemover = new StopWordsRemover()
				.setInputCol(bodyTokenizer.getOutputCol())
				.setOutputCol("bodyCleaned");
		
		Word2Vec title2vec = new Word2Vec()
				.setInputCol(titleStopWordsRemover.getOutputCol())
				.setOutputCol("title2vec")
				.setVectorSize(100)
				.setMinCount(2)
				.setWindowSize(1);
		
		Word2Vec body2vec = new Word2Vec()
				.setInputCol(bodyStopWordsRemover.getOutputCol())
				.setOutputCol("body2vec")
				.setVectorSize(100)
				.setMinCount(2)
				.setWindowSize(1);
		
		Word2Vec tags2vec = new Word2Vec()
				.setInputCol("tags")
				.setOutputCol("tags2vec")
				.setVectorSize(100)
				.setMinCount(0)
				.setWindowSize(1);
		
		PipelineStage[] stages = {titleTokenizer, bodyTokenizer, titleStopWordsRemover, bodyStopWordsRemover, title2vec, body2vec, tags2vec};
		return stages;
	}
	
	private Dataset<Row> getFeaturesDataset(Dataset<Row> dataset) {
		VectorAssembler assembler = new VectorAssembler()
		        .setInputCols(new String[] {"tags2vec", "title2vec", "body2vec"})
		        .setOutputCol("features");
		Dataset<Row> featureDataset = assembler.transform(dataset);
		return featureDataset;
	}
	
	public void fit(Dataset<Row> dataset) {
		pipelineModel = pipeline.fit(dataset);
	}
	
	public Dataset<Row> transform(Dataset<Row> dataset) {
		Dataset<Row> vectorizedDataset = pipelineModel.transform(dataset);
		return getFeaturesDataset(vectorizedDataset);
	}
	
	public static PipelineInstance getInstance() {
		return instance;
	}

}