package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TramConsumerObservationContextTest {

    @Test
    void shouldExtendObservationContext() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        assertThat(context).isInstanceOf(Observation.Context.class);
    }

    @Test
    void shouldStoreDestination() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("order-events", "order-service");
        assertThat(context.getDestination()).isEqualTo("order-events");
    }

    @Test
    void shouldStoreSubscriberId() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("order-events", "order-service");
        assertThat(context.getSubscriberId()).isEqualTo("order-service");
    }

    @Test
    void shouldStoreMessageId() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        context.setMessageId("msg-456");
        assertThat(context.getMessageId()).isEqualTo("msg-456");
    }

    @Test
    void shouldStoreMessageHeaders() {
        TramConsumerObservationContext context = new TramConsumerObservationContext("test-destination", "test-subscriber");
        Map<String, String> headers = Map.of("traceId", "abc123");
        context.setMessageHeaders(headers);
        assertThat(context.getMessageHeaders()).isEqualTo(headers);
    }
}
