package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.tracing.propagation.Propagator;

import java.util.Map;

public class MessageHeaderPropagatorSetter implements Propagator.Setter<Map<String, String>> {

    @Override
    public void set(Map<String, String> carrier, String key, String value) {
        carrier.put(key, value);
    }
}
