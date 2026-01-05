package io.eventuate.tram.spring.micrometer.tracing.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JaegerSpanVerifier {

    private final String jaegerQueryUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    public JaegerSpanVerifier(String jaegerQueryUrl, String serviceName, RestTemplate restTemplate) {
        this.jaegerQueryUrl = jaegerQueryUrl;
        this.serviceName = serviceName;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<List<JaegerSpan>> getTraces() {
        String url = String.format("%s/api/traces?service=%s&limit=20", jaegerQueryUrl, serviceName);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return parseTraces(response.getBody());
    }

    private List<List<JaegerSpan>> parseTraces(String json) {
        List<List<JaegerSpan>> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode data = root.get("data");
            if (data != null && data.isArray()) {
                for (JsonNode trace : data) {
                    List<JaegerSpan> spans = new ArrayList<>();
                    JsonNode spansNode = trace.get("spans");
                    if (spansNode != null && spansNode.isArray()) {
                        for (JsonNode spanNode : spansNode) {
                            JaegerSpan span = objectMapper.treeToValue(spanNode, JaegerSpan.class);
                            spans.add(span);
                        }
                    }
                    result.add(spans);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Jaeger traces: " + json, e);
        }
        return result;
    }

    public JaegerSpan findSpanByName(List<JaegerSpan> trace, String name) {
        return findSpan(trace, span -> span.hasName(name))
                .orElseThrow(() -> new AssertionError("Span with name '" + name + "' not found in trace: " + trace));
    }

    public Optional<JaegerSpan> findOptionalSpanByName(List<JaegerSpan> trace, String name) {
        return findSpan(trace, span -> span.hasName(name));
    }

    public Optional<JaegerSpan> findSpan(List<JaegerSpan> trace, Predicate<JaegerSpan> predicate) {
        return trace.stream().filter(predicate).findFirst();
    }

    public void assertSameTrace(JaegerSpan span1, JaegerSpan span2) {
        assertEquals(span1.getTraceID(), span2.getTraceID(),
                String.format("Expected spans to have same traceId: %s vs %s", span1, span2));
    }
}
