package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.docs.ObservationDocumentation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TramObservationDocumentationTest {

    @Test
    void shouldImplementObservationDocumentation() {
        assertTrue(ObservationDocumentation.class.isAssignableFrom(TramObservationDocumentation.class));
    }

    @Test
    void shouldHaveProducerObservation() {
        assertEquals("eventuate.tram.producer", TramObservationDocumentation.PRODUCER.getName());
    }

    @Test
    void shouldHaveConsumerObservation() {
        assertEquals("eventuate.tram.consumer", TramObservationDocumentation.CONSUMER.getName());
    }

    @Test
    void shouldHaveDeduplicationObservation() {
        assertEquals("eventuate.tram.deduplication", TramObservationDocumentation.DEDUPLICATION.getName());
    }

    @Test
    void producerShouldHaveLowCardinalityKeys() {
        var keys = TramObservationDocumentation.PRODUCER.getLowCardinalityKeyNames();
        assertTrue(keys.length > 0);
    }

    @Test
    void consumerShouldHaveLowCardinalityKeys() {
        var keys = TramObservationDocumentation.CONSUMER.getLowCardinalityKeyNames();
        assertTrue(keys.length > 0);
    }
}
