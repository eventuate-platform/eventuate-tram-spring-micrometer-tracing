package io.eventuate.tram.spring.micrometer.tracing.consumer;

import io.eventuate.tram.consumer.common.MessageHandlerDecorator;
import io.eventuate.tram.consumer.common.MessageHandlerDecoratorChain;
import io.eventuate.tram.messaging.common.Message;
import io.eventuate.tram.messaging.common.SubscriberIdAndMessage;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.micrometer.observation.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObservationMessageConsumerDecorator implements MessageHandlerDecorator {

    private static final Logger logger = LoggerFactory.getLogger(ObservationMessageConsumerDecorator.class);

    private final ObservationHelper observationHelper;

    public ObservationMessageConsumerDecorator(ObservationHelper observationHelper) {
        this.observationHelper = observationHelper;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void accept(SubscriberIdAndMessage subscriberIdAndMessage, MessageHandlerDecoratorChain chain) {
        String subscriberId = subscriberIdAndMessage.getSubscriberId();
        Message message = subscriberIdAndMessage.getMessage();
        String destination = message.getRequiredHeader(Message.DESTINATION);

        logger.info("Starting consumer observation for destination: {}, subscriberId: {}", destination, subscriberId);

        ObservationHelper.ConsumerObservationResult result =
                observationHelper.startConsumerObservation(destination, subscriberId, message.getHeaders());

        Observation observation = result.getObservation();
        Observation.Scope scope = observation.openScope();

        try {
            chain.invokeNext(subscriberIdAndMessage);
        } catch (Exception e) {
            logger.debug("Recording error in consumer observation: {}", e.getMessage());
            observation.error(e);
            if (result.getParentSpan() != null) {
                result.getParentSpan().error(e);
            }
            throw e;
        } finally {
            logger.debug("Stopping consumer observation for message: {}", message.getId());
            scope.close();
            observation.stop();

            // Close the parent span scope (don't end the extracted span)
            if (result.getSpanInScope() != null) {
                result.getSpanInScope().close();
            }
        }
    }
}
