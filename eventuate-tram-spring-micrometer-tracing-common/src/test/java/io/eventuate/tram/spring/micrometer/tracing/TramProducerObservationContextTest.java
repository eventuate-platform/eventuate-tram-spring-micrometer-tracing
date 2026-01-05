package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TramProducerObservationContextTest {

    @Test
    void shouldExtendObservationContext() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        assertThat(context).isInstanceOf(Observation.Context.class);
    }

    @Test
    void shouldStoreDestination() {
        TramProducerObservationContext context = new TramProducerObservationContext("order-events");
        assertThat(context.getDestination()).isEqualTo("order-events");
    }

    @Test
    void shouldStoreMessageId() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        context.setMessageId("msg-123");
        assertThat(context.getMessageId()).isEqualTo("msg-123");
    }

    @Test
    void shouldStoreMessageHeaders() {
        TramProducerObservationContext context = new TramProducerObservationContext("test-destination");
        Map<String, String> headers = Map.of("key1", "value1", "key2", "value2");
        context.setMessageHeaders(headers);
        assertThat(context.getMessageHeaders()).isEqualTo(headers);
    }
}
