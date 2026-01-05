package io.eventuate.tram.spring.micrometer.tracing;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MessageHeaderMapAccessorTest {

    @Test
    void shouldPutHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        accessor.put("key1", "value1");

        assertEquals("value1", headers.get("key1"));
    }

    @Test
    void shouldGetHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        assertEquals("value1", accessor.get("key1"));
    }

    @Test
    void shouldReturnNullForMissingHeader() {
        Map<String, String> headers = new HashMap<>();
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        assertNull(accessor.get("missing"));
    }

    @Test
    void shouldRemoveHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("key1", "value1");
        MessageHeaderMapAccessor accessor = new MessageHeaderMapAccessor(headers);

        accessor.remove("key1");

        assertFalse(headers.containsKey("key1"));
    }
}
