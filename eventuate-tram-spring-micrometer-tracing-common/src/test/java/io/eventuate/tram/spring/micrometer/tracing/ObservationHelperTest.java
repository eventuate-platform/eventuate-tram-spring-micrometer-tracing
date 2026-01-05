package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ObservationHelperTest {

    private TestObservationRegistry observationRegistry;
    private Tracer tracer;
    private Propagator propagator;
    private ObservationHelper observationHelper;

    @BeforeEach
    void setUp() {
        observationRegistry = TestObservationRegistry.create();
        tracer = mock(Tracer.class);
        propagator = mock(Propagator.class);
        observationHelper = new ObservationHelper(observationRegistry, tracer, propagator);
    }

    @Test
    void shouldStartProducerObservation() {
        Map<String, String> headers = new HashMap<>();

        Observation observation = observationHelper.startProducerObservation("test-destination", headers);

        assertThat(observation).isNotNull();
        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("eventuate.tram.producer");
    }

    @Test
    void shouldStartConsumerObservation() {
        Map<String, String> headers = new HashMap<>();

        ObservationHelper.ConsumerObservationResult result = observationHelper.startConsumerObservation("test-destination", "test-subscriber", headers);

        assertThat(result).isNotNull();
        assertThat(result.getObservation()).isNotNull();
        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo("eventuate.tram.consumer");
    }

    @Test
    void shouldHandleNoopRegistry() {
        ObservationHelper noopHelper = new ObservationHelper(ObservationRegistry.NOOP, tracer, propagator);
        Map<String, String> headers = new HashMap<>();

        Observation observation = noopHelper.startProducerObservation("test-destination", headers);

        assertThat(observation).isNotNull();
    }
}
