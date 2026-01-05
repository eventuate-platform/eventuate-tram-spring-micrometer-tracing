package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ObservationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ObservationHelper.class);

    private final ObservationRegistry observationRegistry;
    private final Tracer tracer;
    private final Propagator propagator;
    private final MessageHeaderPropagatorSetter setter = new MessageHeaderPropagatorSetter();
    private final MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

    public ObservationHelper(ObservationRegistry observationRegistry, Tracer tracer, Propagator propagator) {
        this.observationRegistry = observationRegistry;
        this.tracer = tracer;
        this.propagator = propagator;
        logger.info("ObservationHelper initialized with propagator: {}, propagator.fields: {}",
                propagator != null ? propagator.getClass().getName() : "null",
                propagator != null ? propagator.fields() : "null");
    }

    public Observation startProducerObservation(String destination, Map<String, String> headers) {
        TramProducerObservationContext context = new TramProducerObservationContext(destination);
        context.setMessageHeaders(headers);

        Observation observation = TramObservationDocumentation.PRODUCER
                .observation(null, TramObservationDocumentation.DefaultConvention.INSTANCE, () -> context, observationRegistry)
                .start();

        // Note: Trace context injection happens in injectTraceContext() after scope is opened
        return observation;
    }

    /**
     * Injects the current trace context into the message headers.
     * This should be called AFTER the observation scope is opened so that
     * the observation's span is current.
     */
    public void injectTraceContext(Map<String, String> headers) {
        logger.debug("injectTraceContext: tracer={}, currentSpan={}", tracer, tracer != null ? tracer.currentSpan() : null);
        if (tracer != null && tracer.currentSpan() != null) {
            var context = tracer.currentSpan().context();
            logger.debug("Context type: {}, traceId={}, spanId={}, propagator.fields: {}",
                    context.getClass().getName(), context.traceId(), context.spanId(), propagator.fields());
            propagator.inject(context, headers, setter);
            logger.debug("After propagator.inject, headers: {}", headers);
        } else {
            logger.warn("Unable to inject trace context: tracer={}, currentSpan={}", tracer, tracer != null ? tracer.currentSpan() : null);
        }
    }

    public ConsumerObservationResult startConsumerObservation(String destination, String subscriberId, Map<String, String> headers) {
        TramConsumerObservationContext context = new TramConsumerObservationContext(destination, subscriberId);
        context.setMessageHeaders(headers);

        Span extractedSpan = null;
        Tracer.SpanInScope spanInScope = null;

        // Extract parent trace context from message headers and make it current
        if (tracer != null && propagator != null) {
            try {
                extractedSpan = propagator.extract(headers, getter).start();
                if (extractedSpan != null) {
                    // Make the extracted span current so the observation becomes its child
                    spanInScope = tracer.withSpan(extractedSpan);
                }
            } catch (Exception e) {
                // Ignore extraction errors - continue without parent context
            }
        }

        Observation observation = TramObservationDocumentation.CONSUMER
                .observation(null, TramObservationDocumentation.DefaultConvention.INSTANCE, () -> context, observationRegistry)
                .start();

        return new ConsumerObservationResult(observation, extractedSpan, spanInScope);
    }

    public static class ConsumerObservationResult {
        private final Observation observation;
        private final Span parentSpan;
        private final Tracer.SpanInScope spanInScope;

        public ConsumerObservationResult(Observation observation, Span parentSpan, Tracer.SpanInScope spanInScope) {
            this.observation = observation;
            this.parentSpan = parentSpan;
            this.spanInScope = spanInScope;
        }

        public Observation getObservation() {
            return observation;
        }

        public Span getParentSpan() {
            return parentSpan;
        }

        public Tracer.SpanInScope getSpanInScope() {
            return spanInScope;
        }
    }
}
