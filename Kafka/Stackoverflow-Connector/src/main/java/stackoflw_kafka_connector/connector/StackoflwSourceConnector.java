package stackoflw_kafka_connector.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;

import stackoflw_kafka_connector.client.WSSClient;

public class StackoflwSourceConnector extends SourceConnector{

	private String wssURI;
	private String kafkaTopicName;
	private WSSClient wssClient;
	
	@Override
	public String version() {
		return "1";
	}

	@Override
	public void start(Map<String, String> props) {
		wssURI = props.get("wss");
		kafkaTopicName = props.get("topic");
		wssClient = new WSSClient(wssURI);
		wssClient.start();
	}

	@Override
	public Class<? extends Task> taskClass() {
		return StackoflwSourceTask.class;
	}

	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		List<Map<String, String>> configs = new ArrayList<>();
		Map<String, String> config = new HashMap<>();
		config.put("topic", kafkaTopicName);
		configs.add(config);
		return configs;
	}

	@Override
	public void stop() {
		wssClient.stop();
	}

	@Override
	public ConfigDef config() {
		return new ConfigDef()
				.define("wss", Type.STRING, Importance.HIGH, "Stackoverflow WSS URL")
				.define("topic", Type.STRING, Importance.HIGH, "Kafka Topic Name");
	}

}
