import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

public class DelayedRetry {

    //private static int DELAY = 5000;

    public static void declareTopology(Channel channel, String queueName, int delay) throws IOException {
        channel.queueDeclare(queueName, true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", "quorum");
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", queueName + "-dlx");
        }});
    
        
        channel.queueDeclare(queueName + "-dlx", true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", "quorum");
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", queueName);
            put("x-message-ttl", delay);
        }});
    }

    public static DeliverCallback createRetryDeliverCallback(Channel channel, int retries) {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            if (delivery.getProperties().getHeaders() != null) {
                ArrayList<Object> death =  (ArrayList<Object>) delivery.getProperties().getHeaders().get("x-death");
                if (death != null) {
                    HashMap<String,Object> first = (HashMap<String,Object>) death.get(0);
    
                    Long count = (Long) first.get("count");
                    System.out.println("Retry number: " + count);
                    if (count >= retries) {
                        // We exceeded number of retries, give up on message:
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        return;
                    }
                }
            }
    
            // Simulate message processing error by sending NACK:
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };
        return deliverCallback;
    }
    
}
