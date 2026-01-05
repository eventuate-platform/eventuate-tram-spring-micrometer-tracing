package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TramProducerObservationContextTest {

    @Test
    void shouldExtendObservationContext() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        assertInstanceOf(Observation.Context.class, context);
    }

    @Test
    void shouldStoreDestination() {
        TramProducerObservationContext context = new TramProducerObservationContext("order-events");
        assertEquals("order-events", context.getDestination());
    }

    @Test
    void shouldStoreMessageId() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        context.setMessageId("msg-123");
        assertEquals("msg-123", context.getMessageId());
    }

    @Test
    void shouldStoreMessageHeaders() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        Map<String, String> headers = Map.of("key1", "value1", "key2", "value2");
        context.setMessageHeaders(headers);
        assertEquals(headers, context.getMessageHeaders());
    }
}
