import java.io.IOException;
import java.util.HashMap;

import com.rabbitmq.client.Channel;

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
    
}
