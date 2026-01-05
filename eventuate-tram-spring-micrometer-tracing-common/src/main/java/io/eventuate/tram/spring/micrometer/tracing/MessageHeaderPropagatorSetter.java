package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MessageHeaderPropagatorSetter implements Propagator.Setter<Map<String, String>> {

    private static final Logger logger = LoggerFactory.getLogger(MessageHeaderPropagatorSetter.class);

    @Override
    public void set(Map<String, String> carrier, String key, String value) {
        logger.debug("Setting header: {} = {}", key, value);
        carrier.put(key, value);
    }
}
