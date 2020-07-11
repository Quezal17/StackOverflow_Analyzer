package stackoflw_kafka_connector.connector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;

import stackoflw_kafka_connector.client.QuestionsQueue;

public class StackoflwSourceTask extends SourceTask{

	private final String OFFSET_KEY = "Stackoverflow WSS";
	private String kafkaTopicName;
	private QuestionsQueue queue;
	private Long count;
	
	@Override
	public String version() {
		return "1";
	}

	@Override
	public void start(Map<String, String> props) {
		kafkaTopicName = props.get("topic");
		queue = QuestionsQueue.getInstance();
		count = 0L;
	}

	@Override
	public List<SourceRecord> poll() throws InterruptedException {
		List<SourceRecord> recordsList = new ArrayList<>();
		while(!queue.isEmpty()) {
			String question = queue.readQuestion();
			SourceRecord record = new SourceRecord(offsetKey(OFFSET_KEY), offsetValue(count++), kafkaTopicName, Schema.STRING_SCHEMA, question);
			recordsList.add(record);
		}
		return recordsList;
	}

	@Override
	public void stop() {
		
	}
	
	private Map<String, String> offsetKey(String wss) {
		return Collections.singletonMap("wss", wss);
	}
	
	private Map<String, Long> offsetValue(Long position) {
		return Collections.singletonMap("position", position);
	}

}
