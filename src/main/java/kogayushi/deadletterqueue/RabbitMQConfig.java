package kogayushi.deadletterqueue;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RabbitMQConfig {
    public static final String DLX_MESSAGES_EXCHANGE = "DLX.MESSAGES.EXCHANGE";

    public static final String DLQ_MESSAGES_QUEUE = "DLQ.MESSAGES.QUEUE";

    public static final String MESSAGES_QUEUE = "MESSAGES.QUEUE";

    public static final String MESSAGES_EXCHANGE = "MESSAGES.EXCHANGE";

    public static final String ROUTING_KEY_MESSAGES_QUEUE = "ROUTING_KEY_MESSAGES_QUEUE";

    @Bean
    Queue messagesQueue() {
        return QueueBuilder.durable(MESSAGES_QUEUE)
                           .withArgument("x-dead-letter-exchange", DLX_MESSAGES_EXCHANGE)
                           .build();
    }

    @Bean
    DirectExchange messagesExchange() {
        return new DirectExchange(MESSAGES_EXCHANGE);
    }

    @Bean
    Binding bindingMessages() {
        return BindingBuilder.bind(messagesQueue()).to(messagesExchange()).with(ROUTING_KEY_MESSAGES_QUEUE);
    }

    @Bean
    FanoutExchange deadLetterExchange() {
        return new FanoutExchange(DLX_MESSAGES_EXCHANGE);
    }

    @Bean
    Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ_MESSAGES_QUEUE).build();
    }

    @Bean
    Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    @RabbitListener(queues = MESSAGES_QUEUE)
    public Mono<String> receiveMessage(Message message) { // 返り値をMonoにするとdead letter queueに転送されない
        System.out.println("Received failed message, re-queueing: " + message.toString());
        System.out.println(
                "Received failed message, re-queueing: " + message.getMessageProperties().getReceivedRoutingKey());
        throw new RuntimeException("fail");
    }

    @Bean
    public ApplicationRunner runner(
            RabbitTemplate template) {
        return args -> {
            template.convertAndSend(MESSAGES_EXCHANGE,
                                    ROUTING_KEY_MESSAGES_QUEUE, "foo");
        };
    }

}
