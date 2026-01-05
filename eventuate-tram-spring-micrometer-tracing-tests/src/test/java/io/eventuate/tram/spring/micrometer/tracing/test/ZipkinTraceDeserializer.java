package io.eventuate.tram.spring.micrometer.tracing.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class ZipkinTraceDeserializer {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<List<ZipkinSpan>> deserializeTraces(String jsonString) {
        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<List<ZipkinSpan>>>() {});
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize Zipkin traces: " + jsonString, e);
        }
    }
}
