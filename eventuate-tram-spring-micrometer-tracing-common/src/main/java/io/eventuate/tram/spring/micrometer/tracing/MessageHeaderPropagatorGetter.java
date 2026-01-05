package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.tracing.propagation.Propagator;

import java.util.Map;

public class MessageHeaderPropagatorGetter implements Propagator.Getter<Map<String, String>> {

    @Override
    public String get(Map<String, String> carrier, String key) {
        return carrier.get(key);
    }
}
