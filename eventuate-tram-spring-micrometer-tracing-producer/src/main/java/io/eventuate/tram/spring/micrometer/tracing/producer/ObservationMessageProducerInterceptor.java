package io.eventuate.tram.spring.micrometer.tracing.producer;

import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.common.MessageInterceptor;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.micrometer.observation.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservationMessageProducerInterceptor implements MessageInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ObservationMessageProducerInterceptor.class);

    private final ObservationHelper observationHelper;
    private final ThreadLocal<ObservationHolder> currentObservationHolder = new ThreadLocal<>();

    public ObservationMessageProducerInterceptor(ObservationHelper observationHelper) {
        this.observationHelper = observationHelper;
    }

    @Override
    public void preSend(Message message) {
        String destination = message.getRequiredHeader(Message.DESTINATION);
        logger.info("Starting producer observation for destination: {}", destination);
        Observation observation = observationHelper.startProducerObservation(destination, message.getHeaders());
        Observation.Scope scope = observation.openScope();
        // Inject trace context AFTER opening scope so the observation's span is current
        observationHelper.injectTraceContext(message.getHeaders());
        currentObservationHolder.set(new ObservationHolder(observation, scope));
    }

    @Override
    public void postSend(Message message, Exception e) {
        ObservationHolder holder = currentObservationHolder.get();
        if (holder != null) {
            try {
                if (e != null) {
                    logger.debug("Recording error in producer observation: {}", e.getMessage());
                    holder.observation.error(e);
                }
            } finally {
                logger.debug("Stopping producer observation for message: {}", message.getId());
                holder.scope.close();
                holder.observation.stop();
                currentObservationHolder.remove();
            }
        } else {
            logger.warn("No observation found in postSend for message: {}", message.getId());
        }
    }

    private static class ObservationHolder {
        final Observation observation;
        final Observation.Scope scope;

        ObservationHolder(Observation observation, Observation.Scope scope) {
            this.observation = observation;
            this.scope = scope;
        }
    }
}
