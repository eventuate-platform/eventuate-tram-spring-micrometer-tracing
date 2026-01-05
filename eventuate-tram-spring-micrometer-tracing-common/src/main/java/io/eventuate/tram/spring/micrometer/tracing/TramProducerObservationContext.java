package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.Observation;

import java.util.Map;

public class TramProducerObservationContext extends Observation.Context {

    private final String destination;
    private String messageId;
    private Map<String, String> messageHeaders;

    public TramProducerObservationContext(String destination) {
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
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
