package stackoflw_kafka_connector;

import stackoflw_kafka_connector.client.QuestionsQueue;
import stackoflw_kafka_connector.client.WSSClient;

public class App 
{
    public static void main( String[] args )
    {
        String wssURL = "wss://qa.sockets.stackexchange.com/";
        new WSSClient(wssURL).start();
        QuestionsQueue queue = QuestionsQueue.getInstance();
        while(true) {
        	String question = queue.readQuestion();
        	System.out.println("Read from queue: "+ question);
        }
    }
}
