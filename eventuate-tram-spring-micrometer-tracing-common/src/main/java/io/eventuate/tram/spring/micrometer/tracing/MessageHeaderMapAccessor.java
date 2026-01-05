package io.eventuate.tram.spring.micrometer.tracing;

import java.util.Map;

public class MessageHeaderMapAccessor {

    private final Map<String, String> headers;

    public MessageHeaderMapAccessor(Map<String, String> headers) {
        this.headers = headers;
    }

    public void put(String key, String value) {
        headers.put(key, value);
    }

    public String get(String key) {
        return headers.get(key);
    }

    public void remove(String key) {
        headers.remove(key);
    }
}
