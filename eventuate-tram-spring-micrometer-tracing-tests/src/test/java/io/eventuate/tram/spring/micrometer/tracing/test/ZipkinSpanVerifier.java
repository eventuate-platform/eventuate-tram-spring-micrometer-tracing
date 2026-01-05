package io.eventuate.tram.spring.micrometer.tracing.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public class ZipkinSpanVerifier {

    private final String zipkinBaseUrl;
    private final RestTemplate restTemplate;

    public ZipkinSpanVerifier(String zipkinBaseUrl, RestTemplate restTemplate) {
        this.zipkinBaseUrl = zipkinBaseUrl;
        this.restTemplate = restTemplate;
    }

    public List<List<ZipkinSpan>> getTraces() {
        String url = zipkinBaseUrl + "api/v2/traces";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ZipkinTraceDeserializer.deserializeTraces(response.getBody());
    }

    public List<List<ZipkinSpan>> getTracesByAnnotation(String annotationQuery) {
        String url = String.format("%sapi/v2/traces?annotationQuery=%s", zipkinBaseUrl, annotationQuery);
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return ZipkinTraceDeserializer.deserializeTraces(response.getBody());
    }

    public ZipkinSpan findSpanByName(List<ZipkinSpan> trace, String name) {
        return findSpan(trace, span -> span.hasName(name))
                .orElseThrow(() -> new AssertionError("Span with name '" + name + "' not found in trace: " + trace));
    }

    public Optional<ZipkinSpan> findOptionalSpanByName(List<ZipkinSpan> trace, String name) {
        return findSpan(trace, span -> span.hasName(name));
    }

    public ZipkinSpan findSpanByTag(List<ZipkinSpan> trace, String tagKey, String tagValue) {
        return findSpan(trace, span -> span.hasTag(tagKey, tagValue))
                .orElseThrow(() -> new AssertionError(
                        "Span with tag '" + tagKey + "=" + tagValue + "' not found in trace: " + trace));
    }

    public Optional<ZipkinSpan> findSpan(List<ZipkinSpan> trace, Predicate<ZipkinSpan> predicate) {
        return trace.stream().filter(predicate).findFirst();
    }

    public void assertChildOf(ZipkinSpan parent, ZipkinSpan child) {
        assertTrue(child.isChildOf(parent),
                String.format("Expected %s to be child of %s, but parentId was %s",
                        child, parent.getId(), child.getParentId()));
    }

    public void assertSameTrace(ZipkinSpan span1, ZipkinSpan span2) {
        assertEquals(span1.getTraceId(), span2.getTraceId(),
                String.format("Expected spans to have same traceId: %s vs %s", span1, span2));
    }
}
