package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TramConsumerObservationContextTest {

    @Test
    void shouldExtendObservationContext() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        assertInstanceOf(Observation.Context.class, context);
    }

    @Test
    void shouldStoreDestination() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("order-events", "order-service");
        assertEquals("order-events", context.getDestination());
    }

    @Test
    void shouldStoreSubscriberId() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("order-events", "order-service");
        assertEquals("order-service", context.getSubscriberId());
    }

    @Test
    void shouldStoreMessageId() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        context.setMessageId("msg-456");
        assertEquals("msg-456", context.getMessageId());
    }

    @Test
    void shouldStoreMessageHeaders() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        Map<String, String> headers = Map.of("traceId", "abc123");
        context.setMessageHeaders(headers);
        assertEquals(headers, context.getMessageHeaders());
    }
}
