package io.eventuate.tram.spring.micrometer.tracing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageHeaderMapAccessorTest {

    @Test
    void shouldPutHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        accessor.put("key1", "value1");

        assertThat(headers.get("key1")).isEqualTo("value1");
    }

    @Test
    void shouldGetHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        assertThat(accessor.get("key1")).isEqualTo("value1");
    }

    @Test
    void shouldReturnNullForMissingHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        assertThat(accessor.get("missing")).isNull();
    }

    @Test
    void shouldRemoveHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        accessor.remove("key1");

        assertThat(headers).doesNotContainKey("key1");
    }
}
