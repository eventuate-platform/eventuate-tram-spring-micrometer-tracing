package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.Map;

public class ObservationHelper {

    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final Propagator propagator;
    private final MessageHeaderPropagatorSetter setter = new MessageHeaderPropagatorSetter();
    private final MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

    public ObservationHelper(ObservationRegistry observationRegistry, Tracer tracer, Propagator propagator) {
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    public Observation startProducerObservation(String destination, Map<String, String> headers) {
        TramProducerObservationContext context = new TramProducerObservationContext(destination);
        context.setMessageHeaders(headers);

        Observation observation = TramObservationDocumentation.PRODUCER
                .observation(null, TramObservationDocumentation.DefaultConvention.INSTANCE, () -> context, observationRegistry)
                .start();

        if (tracer != null && tracer.currentSpan() != null) {
            propagator.inject(tracer.currentSpan().context(), headers, setter);
        }

        return observation;
    }

    public Observation startConsumerObservation(String destination, String subscriberId, Map<String, String> headers) {
        TramConsumerObservationContext context = new TramConsumerObservationContext(destination, subscriberId);
        context.setMessageHeaders(headers);

        Observation.Context parentContext = null;
        if (tracer != null) {
            var extractedContext = propagator.extract(headers, getter);
            // The parent context would be set via the tracer
        }

        Observation observation = TramObservationDocumentation.CONSUMER
                .observation(null, TramObservationDocumentation.DefaultConvention.INSTANCE, () -> context, observationRegistry)
                .start();

        return observation;
    }
}
