package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageHeaderPropagatorTest {

    @Test
    void setterShouldImplementPropagatorSetter() {
        assertThat(Propagator.Setter.class.isAssignableFrom(MessageHeaderPropagatorSetter.class)).isTrue();
    }

    @Test
    void setterShouldSetHeaderValue() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderPropagatorSetter setter = new MessageHeaderPropagatorSetter();

        setter.set(headers, "X-B3-TraceId", "abc123");

        assertThat(headers.get("X-B3-TraceId")).isEqualTo("abc123");
    }

    @Test
    void getterShouldImplementPropagatorGetter() {
        assertThat(Propagator.Getter.class.isAssignableFrom(MessageHeaderPropagatorGetter.class)).isTrue();
    }

    @Test
    void getterShouldGetHeaderValue() {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-B3-TraceId", "abc123");
        MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

        assertThat(getter.get(headers, "X-B3-TraceId")).isEqualTo("abc123");
    }

    @Test
    void getterShouldReturnNullForMissingHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderPropagatorGetter getter = new MessageHeaderPropagatorGetter();

        assertThat(getter.get(headers, "missing")).isNull();
    }
}
