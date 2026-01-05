package io.eventuate.tram.spring.micrometer.tracing.test;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ZipkinTraceDeserializerTest {

    @Test
    void shouldDeserializeEmptyTraces() {
        List<List<ZipkinSpan>> traces = ZipkinTraceDeserializer.deserializeTraces("[]");
        assertTrue(traces.isEmpty());
    }

    @Test
    void shouldDeserializeTraceWithSingleSpan() {
        String json = """
            [[{
                "traceId": "abc123",
                "id": "span1",
                "name": "test-span",
                "tags": {"key": "value"}
            }]]
            """;

        List<List<ZipkinSpan>> traces = ZipkinTraceDeserializer.deserializeTraces(json);
        assertEquals(1, traces.size());

        List<ZipkinSpan> trace = traces.get(0);
        assertEquals(1, trace.size());

        ZipkinSpan span = trace.get(0);
        assertEquals("abc123", span.getTraceId());
        assertEquals("span1", span.getId());
        assertEquals("test-span", span.getName());
        assertTrue(span.hasTag("key", "value"));
        assertTrue(span.hasName("test-span"));
    }

    @Test
    void shouldDeserializeTraceWithParentChild() {
        String json = """
            [[{
                "traceId": "abc123",
                "id": "parent1",
                "name": "parent-span"
            }, {
                "traceId": "abc123",
                "id": "child1",
                "parentId": "parent1",
                "name": "child-span"
            }]]
            """;

        List<List<ZipkinSpan>> traces = ZipkinTraceDeserializer.deserializeTraces(json);
        assertEquals(1, traces.size());

        List<ZipkinSpan> trace = traces.get(0);
        assertEquals(2, trace.size());

        ZipkinSpan parent = trace.stream().filter(s -> s.hasName("parent-span")).findFirst().orElseThrow();
        ZipkinSpan child = trace.stream().filter(s -> s.hasName("child-span")).findFirst().orElseThrow();

        assertTrue(child.isChildOf(parent));
    }

    @Test
    void shouldIgnoreUnknownProperties() {
        String json = """
            [[{
                "traceId": "abc123",
                "id": "span1",
                "name": "test-span",
                "unknownField": "someValue",
                "anotherUnknown": 123
            }]]
            """;

        List<List<ZipkinSpan>> traces = ZipkinTraceDeserializer.deserializeTraces(json);
        assertEquals(1, traces.size());
        assertEquals("test-span", traces.get(0).get(0).getName());
    }
}
