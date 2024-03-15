import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

public class DelayedRetry {

    public static void declareTopology(Channel channel, String queueName, int delay) throws IOException {
        declareTopology(channel, queueName, delay, QueueType.Quorum);
    }

    /**
     * @param channel    
     * @param queueName The name of the queue. The DLX queue name is queueName-dlx.
     * @param delay     The message retry DELAY (set as TTL on the DLX queue)
     * @param queueType
     * @throws IOException
     */
    public static void declareTopology(Channel channel, String queueName, int delay, QueueType queueType) throws IOException {

        final String qType = getQueueType(queueType);

        // NOTE: this may throw a PRECONDITION_FAILED if queues with same names but different arguments (e.g. type or TTL) exist:
        channel.queueDeclare(queueName, true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", qType);
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", queueName + "-dlx");
        }});
        
        channel.queueDeclare(queueName + "-dlx", true, false, false, new HashMap<String,Object>() {{ 
            put("x-queue-type", qType);
            put("x-dead-letter-exchange", "");
            put("x-dead-letter-routing-key", queueName);
            put("x-message-ttl", delay);
        }});
    }

    /**
     * @param channel
     * @param retries The number of message retries
     * @return
     */
    public static DeliverCallback createRetryDeliveryCallback(Channel channel, int retries) {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            if (delivery.getProperties().getHeaders() != null) {
                @SuppressWarnings("unchecked")
                ArrayList<Object> death =  (ArrayList<Object>) delivery.getProperties().getHeaders().get("x-death");
                if (death != null) {
                    @SuppressWarnings("unchecked")
                    HashMap<String,Object> first = (HashMap<String,Object>) death.get(0);
    
                    Long count = (Long) first.get("count");
                    System.out.println("Retry number: " + count);
                    if (count >= retries) {
                        // We exceeded number of retries, give up on the message processing:
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        return;
                    }
                }
            }
    
            // Requeue by sending NACK:
            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };
        return deliverCallback;
    }
    
    private static String getQueueType(QueueType queueType) {
        String qType;
        switch (queueType) {
            case Quorum:
                qType = "quorum";
                break;
            case Stream:
                qType = "stream";
            default:
                qType = "classic";
                break;
        }
        return qType;
    }
}
