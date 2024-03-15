import com.rabbitmq.client.Delivery;

public interface Consumer {
    public boolean consume(Delivery delivery);
} 
