import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class Recv {

    private final static String QUEUE_NAME = "delayed-retry";
    final static int RETRIES = 6;

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        channel.basicConsume(QUEUE_NAME, 
            false, 
            DelayedRetry.createRetryDeliveryCallback(channel, 6, delivery -> false), // always simulate failure:
            consumerTag -> { });
    }
}
