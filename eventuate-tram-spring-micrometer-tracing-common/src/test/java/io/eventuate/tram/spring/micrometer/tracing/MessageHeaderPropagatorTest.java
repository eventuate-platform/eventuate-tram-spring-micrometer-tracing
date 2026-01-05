package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageHeaderPropagatorTest {

    @Test
    void setterShouldImplementPropagatorSetter() {
        assertTrue(Propagator.Setter.class.isAssignableFrom(MessageHeaderPropagatorSetter.class));
    }

    @Test
    void setterShouldSetHeaderValue() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderPropagatorSetter setter = new MessageHeaderPropagatorSetter();

        setter.set(headers, "X-B3-TraceId", "abc123");

        assertEquals("abc123", headers.get("X-B3-TraceId"));
    }

    @Test
    void getterShouldImplementPropagatorGetter() {
        assertTrue(Propagator.Getter.class.isAssignableFrom(MessageHeaderPropagatorGetter.class));
    }

    @Test
    void getterShouldGetHeaderValue() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-B3-TraceId", "abc123");
        MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

        assertEquals("abc123", getter.get(headers, "X-B3-TraceId"));
    }

    @Test
    void getterShouldReturnNullForMissingHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

        assertNull(getter.get(headers, "missing"));
    }
}
