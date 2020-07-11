package stackoflw_kafka_connector.client;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WSSListener extends WebSocketListener{
	
	private QuestionsQueue queue;
	private String request = "155-questions-active";
	private String heartbeat = "pong";
	
	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		queue = QuestionsQueue.getInstance();
		webSocket.send(request);
	}
	
	@Override
	public void onMessage(WebSocket webSocket, String text) {
		JSONObject response = new JSONObject(text);
		if(response.getString("action").equals("hb"))
			webSocket.send(heartbeat);
		else
			filterResponse(response);
	}
	
	private void addQuestionToQueue(String data) {
		try {
			queue.addQuestion(data);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private void filterResponse(JSONObject response) {
		String data = response.getString("data");
		JSONObject dataJson = new JSONObject(data);
		String questionSite = dataJson.getString("apiSiteParameter");
		if(questionSite.contains("stackoverflow"))
			addQuestionToQueue(data);
	}

}
