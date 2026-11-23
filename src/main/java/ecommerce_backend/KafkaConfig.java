package ecommerce_backend;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaConfig {

    public static final String PRODUCT_CREATED = "product-created";
    public static final String ORDER_CREATED = "order-created";

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(PRODUCT_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name(ORDER_CREATED).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic productCreatedDlt() {
        return TopicBuilder.name(PRODUCT_CREATED + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic orderCreatedDlt() {
        return TopicBuilder.name(ORDER_CREATED + ".DLT").partitions(1).replicas(1).build();
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        handler.addNotRetryableExceptions(IllegalArgumentException.class);
        return handler;
    }
}
