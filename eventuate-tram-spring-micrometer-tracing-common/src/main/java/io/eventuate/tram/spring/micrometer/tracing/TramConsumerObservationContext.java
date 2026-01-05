package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;

import java.util.Map;

public class TramConsumerObservationContext extends Observation.Context {

    private final String destination;
    private final String subscriberId;
    private String messageId;
    private Map<String, String> messageHeaders;

    public TramConsumerObservationContext(String destination, String subscriberId) {
        this.destination = destination;
        this.subscriberId = subscriberId;
    }

    public String getDestination() {
        return destination;
    }

    public String getSubscriberId() {
        return subscriberId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Map<String, String> getMessageHeaders() {
        return messageHeaders;
    }

    public void setMessageHeaders(Map<String, String> messageHeaders) {
        this.messageHeaders = messageHeaders;
    }
}
