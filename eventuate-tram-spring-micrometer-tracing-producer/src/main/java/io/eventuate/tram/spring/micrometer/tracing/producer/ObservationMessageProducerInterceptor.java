package io.eventuate.tram.spring.micrometer.tracing.producer;

import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.common.MessageInterceptor;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.micrometer.observation.Observation;

public class ObservationMessageProducerInterceptor implements MessageInterceptor {

    private final ObservationHelper observationHelper;
    private final ThreadLocal<Observation> currentObservation = new ThreadLocal<>();

    public ObservationMessageProducerInterceptor(ObservationHelper observationHelper) {
        this.observationHelper = observationHelper;
    }

    @Override
    public void preSend(Message message) {
        String destination = message.getRequiredHeader(Message.DESTINATION);
        Observation observation = observationHelper.startProducerObservation(destination, message.getHeaders());
        currentObservation.set(observation);
    }

    @Override
    public void postSend(Message message, Exception e) {
        Observation observation = currentObservation.get();
        if (observation != null) {
            try {
                if (e != null) {
                    observation.error(e);
                }
            } finally {
                observation.stop();
                currentObservation.remove();
            }
        }
    }
}
