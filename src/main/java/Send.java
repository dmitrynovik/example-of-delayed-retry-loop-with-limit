import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Send {

    private final static String QUEUE_NAME = "delayed-retry";
    //private final static String DLX_NAME = "delayed-retry-dlx";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, true, false, false, new HashMap<String,Object>() {{ 
                put("x-queue-type", "quorum");
                put("x-dead-letter-exchange", "");
                put("x-dead-letter-routing-key", QUEUE_NAME + "-dlx");
            }});

            
            channel.queueDeclare(QUEUE_NAME + "-dlx", true, false, false, new HashMap<String,Object>() {{ 
                put("x-queue-type", "quorum");
                put("x-dead-letter-exchange", "");
                put("x-dead-letter-routing-key", QUEUE_NAME);
                put("x-message-ttl", 5000);
            }});

            String message = "Hello World!";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println(" [x] Sent '" + message + "'");
        }
    }
}
