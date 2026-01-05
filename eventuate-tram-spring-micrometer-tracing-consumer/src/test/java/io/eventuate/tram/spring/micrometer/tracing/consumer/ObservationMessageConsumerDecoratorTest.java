package io.eventuate.tram.spring.micrometer.tracing.consumer;

import io.eventuate.tram.consumer.common.MessageHandlerDecoratorChain;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.common.MessageImpl;
import io.eventuate.tram.messaging.common.SubscriberIdAndMessage;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ObservationMessageConsumerDecoratorTest {

    private TestObservationRegistry observationRegistry;
    private ObservationHelper observationHelper;
    private ObservationMessageConsumerDecorator decorator;
    private MessageHandlerDecoratorChain chain;

    @BeforeEach
    void setUp() {
        observationRegistry = TestObservationRegistry.create();
        Tracer tracer = mock(Tracer.class);
        Propagator propagator = mock(Propagator.class);
        observationHelper = new ObservationHelper(observationRegistry, tracer, propagator);
        decorator = new ObservationMessageConsumerDecorator(observationHelper);
        chain = mock(MessageHandlerDecoratorChain.class);
    }

    @Test
    void shouldCreateObservationWhenConsumingMessage() {
        String destination = "test-channel";
        String subscriberId = "test-subscriber";
        Message message = createTestMessage(destination);
        SubscriberIdAndMessage subscriberIdAndMessage = new SubscriberIdAndMessage(subscriberId, message);

        decorator.accept(subscriberIdAndMessage, chain);

        verify(chain).invokeNext(subscriberIdAndMessage);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.CONSUMER.getName())
                .that()
                .hasBeenStarted()
                .hasBeenStopped();
    }

    @Test
    void shouldIncludeDestinationAndSubscriberIdInObservation() {
        String destination = "test-channel";
        String subscriberId = "test-subscriber";
        Message message = createTestMessage(destination);
        SubscriberIdAndMessage subscriberIdAndMessage = new SubscriberIdAndMessage(subscriberId, message);

        decorator.accept(subscriberIdAndMessage, chain);

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.CONSUMER.getName())
                .that()
                .hasLowCardinalityKeyValue("messaging.destination", destination)
                .hasLowCardinalityKeyValue("messaging.subscriber.id", subscriberId);
    }

    @Test
    void shouldOpenScopeDuringHandlerExecution() {
        String destination = "test-channel";
        String subscriberId = "test-subscriber";
        Message message = createTestMessage(destination);
        SubscriberIdAndMessage subscriberIdAndMessage = new SubscriberIdAndMessage(subscriberId, message);

        AtomicBoolean scopeWasOpen = new AtomicBoolean(false);
        doAnswer(invocation -> {
            Observation current = observationRegistry.getCurrentObservation();
            scopeWasOpen.set(current != null);
            return null;
        }).when(chain).invokeNext(subscriberIdAndMessage);

        decorator.accept(subscriberIdAndMessage, chain);

        assertTrue(scopeWasOpen.get(), "Observation scope should be open during handler execution");
    }

    @Test
    void shouldRecordErrorWhenHandlerThrows() {
        String destination = "test-channel";
        String subscriberId = "test-subscriber";
        Message message = createTestMessage(destination);
        SubscriberIdAndMessage subscriberIdAndMessage = new SubscriberIdAndMessage(subscriberId, message);

        RuntimeException expectedException = new RuntimeException("Handler failed");
        doThrow(expectedException).when(chain).invokeNext(subscriberIdAndMessage);

        assertThrows(RuntimeException.class, () -> decorator.accept(subscriberIdAndMessage, chain));

        TestObservationRegistryAssert.assertThat(observationRegistry)
                .hasObservationWithNameEqualTo(TramObservationDocumentation.CONSUMER.getName())
                .that()
                .hasBeenStopped()
                .hasError();
    }

    @Test
    void shouldReturnCorrectOrder() {
        assertEquals(0, decorator.getOrder());
    }

    private Message createTestMessage(String destination) {
        Map<String, String> headers = new HashMap<>();
        headers.put(Message.DESTINATION, destination);
        headers.put(Message.ID, "test-message-id");
        return new MessageImpl("{}", headers);
    }
}
