package ecommerce_backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final EventStore eventStore;
    private final ObjectMapper objectMapper;
    private final Counter processed;
    private final Counter duplicates;
    private final Counter deadLettered;

    public ProductEventConsumer(EventStore eventStore, ObjectMapper objectMapper, MeterRegistry registry) {
        this.eventStore = eventStore;
        this.objectMapper = objectMapper;
        this.processed = registry.counter("events.processed");
        this.duplicates = registry.counter("events.duplicate");
        this.deadLettered = registry.counter("events.dead_lettered");
    }

    @KafkaListener(topics = {KafkaConfig.PRODUCT_CREATED, KafkaConfig.ORDER_CREATED}, groupId = "event-group")
    public void onEvent(String payload) {
        EventMessage event = parse(payload);
        if (eventStore.saveIfAbsent(event)) {
            processed.increment();
            log.info("Processed event {} type={} entity={}", event.eventId(), event.type(), event.entityId());
        } else {
            duplicates.increment();
            log.warn("Skipped duplicate event {}", event.eventId());
        }
    }

    @KafkaListener(topics = {KafkaConfig.PRODUCT_CREATED + ".DLT", KafkaConfig.ORDER_CREATED + ".DLT"}, groupId = "dlt-group")
    public void onDeadLetter(String payload) {
        deadLettered.increment();
        log.error("Event moved to dead-letter topic, needs manual review: {}", payload);
    }

    private EventMessage parse(String payload) {
        try {
            EventMessage event = objectMapper.readValue(payload, EventMessage.class);
            if (event.eventId() == null || event.eventId().isBlank()) {
                throw new IllegalArgumentException("Event is missing eventId");
            }
            return event;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Event payload is not valid JSON", e);
        }
    }
}
