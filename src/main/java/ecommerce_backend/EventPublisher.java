package ecommerce_backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public EventMessage publish(String topic, String type, String entityId, String detail) {
        EventMessage event = new EventMessage(UUID.randomUUID().toString(), type, entityId, detail, Instant.now().toString());
        try {
            String json = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, entityId, json).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Failed to publish event {} to {}", event.eventId(), topic, error);
                } else {
                    log.info("Published event {} type={} topic={}", event.eventId(), type, topic);
                }
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize event", e);
        }
        return event;
    }
}
