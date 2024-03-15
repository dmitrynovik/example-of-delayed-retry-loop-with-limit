import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class Recv {

    private final static String QUEUE_NAME = "delayed-retry";
    private final static int RETRIES = 6;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println(" [x] Received '" + message + "'");
            if (delivery.getProperties().getHeaders() != null) {
                ArrayList<Object> death =  (ArrayList<Object>) delivery.getProperties().getHeaders().get("x-death");
                if (death != null) {
                    HashMap<String,Object> first = (HashMap<String,Object>) death.get(0);

                    Long count = (Long) first.get("count");
                    System.out.println("Retry number: " + count);
                    if (count >= RETRIES) {
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                        return;
                    }
                }
            }

            channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    }
}
