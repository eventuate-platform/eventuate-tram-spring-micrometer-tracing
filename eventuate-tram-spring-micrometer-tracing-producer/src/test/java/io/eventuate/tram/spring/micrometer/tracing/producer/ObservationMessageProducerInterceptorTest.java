package io.eventuate.tram.spring.micrometer.tracing.producer;

import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.eventuate.tram.spring.micrometer.tracing.TramObservationDocumentation;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservationMessageProducerInterceptorTest {

    private TestObservationRegistry observationRegistry;
    private ObservationHelper observationHelper;
    private ObservationMessageProducerInterceptor interceptor;
    private Tracer tracer;
    private Propagator propagator;

    @BeforeEach
    void setUp() {
        observationRegistry = TestObservationRegistry.create();
        tracer = mock(Tracer.class);
        propagator = mock(Propagator.class);
        observationHelper = new ObservationHelper(observationRegistry, tracer, propagator);
        interceptor = new ObservationMessageProducerInterceptor(observationHelper);
    }

    @Test
    void preSendShouldStartObservation() {
        Message message = createMockMessage("test-destination", "msg-123");

        interceptor.preSend(message);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.PRODUCER.getName())
                .that()
                .hasBeenStarted()
                .isNotStopped();
    }

    @Test
    void postSendShouldStopObservation() {
        Message message = createMockMessage("test-destination", "msg-123");

        interceptor.preSend(message);
        interceptor.postSend(message, null);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.PRODUCER.getName())
                .that()
                .hasBeenStarted()
                .hasBeenStopped();
    }

    @Test
    void postSendShouldRecordErrorWhenExceptionOccurs() {
        Message message = createMockMessage("test-destination", "msg-123");
        RuntimeException exception = new RuntimeException("Send failed");

        interceptor.preSend(message);
        interceptor.postSend(message, exception);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.PRODUCER.getName())
                .that()
                .hasBeenStarted()
                .hasBeenStopped()
                .hasError();
    }

    @Test
    void observationShouldHaveCorrectLowCardinalityKey() {
        Message message = createMockMessage("order-events", "msg-456");

        interceptor.preSend(message);
        interceptor.postSend(message, null);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.PRODUCER.getName())
                .that()
                .hasLowCardinalityKeyValue("messaging.destination", "order-events");
    }

    private Message createMockMessage(String destination, String messageId) {
        Message message = mock(Message.class);
        Map<String, String> headers = new HashMap<>();
        headers.put(Message.DESTINATION, destination);

        when(message.getId()).thenReturn(messageId);
        when(message.getHeaders()).thenReturn(headers);
        when(message.getRequiredHeader(Message.DESTINATION)).thenReturn(destination);
        when(message.getHeader(Message.DESTINATION)).thenReturn(Optional.of(destination));

        return message;
    }
}
