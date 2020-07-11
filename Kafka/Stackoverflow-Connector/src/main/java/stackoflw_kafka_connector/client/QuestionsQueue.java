package stackoflw_kafka_connector.client;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class QuestionsQueue {

	private BlockingQueue<String> queue;
	private static QuestionsQueue instance;
	
	private QuestionsQueue() {
		queue = new LinkedBlockingDeque<>();
	}
	
	public static QuestionsQueue getInstance() {
		if(instance == null)
			instance = new QuestionsQueue();
		return instance;
	}
	
	public void addQuestion(String question) {
		try {
			queue.put(question);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public String readQuestion() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean isEmpty() {
		return queue.isEmpty();
	}
}
