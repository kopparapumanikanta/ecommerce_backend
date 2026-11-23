package ecommerce_backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class ProductEventConsumerTest {

    private static final String VALID = "{\"eventId\":\"e-1\",\"type\":\"PRODUCT_CREATED\",\"entityId\":\"1\",\"detail\":\"Phone\",\"occurredAt\":\"2026-01-01T00:00:00Z\"}";

    private final EventStore store = mock(EventStore.class);
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final ProductEventConsumer consumer = new ProductEventConsumer(store, new ObjectMapper(), registry);

    @Test
    void newEvent_isStoredAndCounted() {
        when(store.saveIfAbsent(any(EventMessage.class))).thenReturn(true);

        consumer.onEvent(VALID);

        assertEquals(1.0, registry.counter("events.processed").count());
        assertEquals(0.0, registry.counter("events.duplicate").count());
    }

    @Test
    void duplicateEvent_isSkippedIdempotently() {
        when(store.saveIfAbsent(any(EventMessage.class))).thenReturn(false);

        consumer.onEvent(VALID);

        assertEquals(0.0, registry.counter("events.processed").count());
        assertEquals(1.0, registry.counter("events.duplicate").count());
    }

    @Test
    void malformedPayload_throwsNonRetryableException() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onEvent("not json"));
        verify(store, never()).saveIfAbsent(any());
    }

    @Test
    void deadLetter_incrementsCounter() {
        consumer.onDeadLetter("bad");

        assertEquals(1.0, registry.counter("events.dead_lettered").count());
    }
}
