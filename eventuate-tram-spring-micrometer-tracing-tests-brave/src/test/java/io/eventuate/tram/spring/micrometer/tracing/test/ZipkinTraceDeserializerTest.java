package io.eventuate.tram.spring.micrometer.tracing.test;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ZipkinTraceDeserializerTest {

    @Test
    void shouldDeserializeEmptyTraces() {
        List<List<ZipkinSpan>> traces = ZipkinTraceDeserializer.deserializeTraces("[]");
        assertThat(traces).isEmpty();
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
        assertThat(traces).hasSize(1);

        List<ZipkinSpan> trace = traces.get(0);
        assertThat(trace).hasSize(1);

        ZipkinSpan span = trace.get(0);
        assertThat(span.getTraceId()).isEqualTo("abc123");
        assertThat(span.getId()).isEqualTo("span1");
        assertThat(span.getName()).isEqualTo("test-span");
        assertThat(span.hasTag("key", "value")).isTrue();
        assertThat(span.hasName("test-span")).isTrue();
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
        assertThat(traces).hasSize(1);

        List<ZipkinSpan> trace = traces.get(0);
        assertThat(trace).hasSize(2);

        ZipkinSpan parent = trace.stream().filter(s -> s.hasName("parent-span")).findFirst().orElseThrow();
        ZipkinSpan child = trace.stream().filter(s -> s.hasName("child-span")).findFirst().orElseThrow();

        assertThat(child.isChildOf(parent)).isTrue();
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
        assertThat(traces).hasSize(1);
        assertThat(traces.get(0).get(0).getName()).isEqualTo("test-span");
    }
}
