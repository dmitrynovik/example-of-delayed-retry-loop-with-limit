import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

public class DelayedRetry {

    private static int DELAY = 5000;

    public static void declareTopology(Channel channel) throws IOException {
        channel.queueDeclare(Send.QUEUE_NAME, true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", "quorum");
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", Send.QUEUE_NAME + "-dlx");
        }});
    
        
        channel.queueDeclare(Send.QUEUE_NAME + "-dlx", true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", "quorum");
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", Send.QUEUE_NAME);
            put("x-message-ttl", DELAY);
        }});
    }

    public static DeliverCallback createRetryDeliverCallback(Channel channel) {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            if (delivery.getProperties().getHeaders() != null) {
                ArrayList<Object> death =  (ArrayList<Object>) delivery.getProperties().getHeaders().get("x-death");
                if (death != null) {
                    HashMap<String,Object> first = (HashMap<String,Object>) death.get(0);
    
                    Long count = (Long) first.get("count");
                    System.out.println("Retry number: " + count);
                    if (count >= Recv.RETRIES) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        return;
                    }
                }
            }
    
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };
        return deliverCallback;
    }
    
}
