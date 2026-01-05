package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.docs.ObservationDocumentation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TramObservationDocumentationTest {

    @Test
    void shouldImplementObservationDocumentation() {
        assertThat(ObservationDocumentation.class.isAssignableFrom(TramObservationDocumentation.class)).isTrue();
    }

    @Test
    void shouldHaveProducerObservation() {
        assertThat(TramObservationDocumentation.PRODUCER.getName()).isEqualTo("eventuate.tram.producer");
    }

    @Test
    void shouldHaveConsumerObservation() {
        assertThat(TramObservationDocumentation.CONSUMER.getName()).isEqualTo("eventuate.tram.consumer");
    }

    @Test
    void shouldHaveDeduplicationObservation() {
        assertThat(TramObservationDocumentation.DEDUPLICATION.getName()).isEqualTo("eventuate.tram.deduplication");
    }

    @Test
    void producerShouldHaveLowCardinalityKeys() {
        var keys = TramObservationDocumentation.PRODUCER.getLowCardinalityKeyNames();
        assertThat(keys).hasSizeGreaterThan(0);
    }

    @Test
    void consumerShouldHaveLowCardinalityKeys() {
        var keys = TramObservationDocumentation.CONSUMER.getLowCardinalityKeyNames();
        assertThat(keys).hasSizeGreaterThan(0);
    }
}
