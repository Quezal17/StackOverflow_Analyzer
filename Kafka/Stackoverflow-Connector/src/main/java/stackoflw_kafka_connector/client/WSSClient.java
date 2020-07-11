package stackoflw_kafka_connector.client;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;

public class WSSClient {

	private OkHttpClient httpClient;
	private WebSocket webSocket;
	private String wssURL;
	
	public WSSClient(String url) {
		httpClient = new OkHttpClient();
		wssURL = url;
	}
	
	public void start() {
		Request request = new Request.Builder().url(wssURL).build();
		webSocket = httpClient.newWebSocket(request, new WSSListener());
	}
	
	public void stop() {
		webSocket.close(0, "Client Stop");
	}
	
}
