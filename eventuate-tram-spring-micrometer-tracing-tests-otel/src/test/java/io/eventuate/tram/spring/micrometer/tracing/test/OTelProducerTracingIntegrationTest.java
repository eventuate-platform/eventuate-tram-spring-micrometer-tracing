package io.eventuate.tram.spring.micrometer.tracing.test;

import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.micrometer.tracing.TramObservationDocumentation;
import io.eventuate.util.test.async.Eventually;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = OTelProducerTracingIntegrationTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class OTelProducerTracingIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Configuration
    @SpringBootApplication
    @Import({TramInMemoryConfiguration.class, TestConsumer.class})
    static class TestConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Autowired
    private TestConsumer testConsumer;

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    // Jaeger with OTLP endpoint
    @Container
    static final GenericContainer<?> jaeger = new GenericContainer<>(DockerImageName.parse("jaegertracing/all-in-one:1.51"))
            .withExposedPorts(16686, 4318)  // Query API and OTLP HTTP
            .withEnv("COLLECTOR_OTLP_ENABLED", "true");

    @DynamicPropertySource
    static void jaegerProperties(DynamicPropertyRegistry registry) {
        // Configure OTLP HTTP exporter endpoint
        registry.add("management.otlp.tracing.endpoint",
                () -> String.format("http://%s:%s/v1/traces", jaeger.getHost(), jaeger.getMappedPort(4318)));
        // Override Spring Boot test defaults that disable tracing
        registry.add("management.tracing.enabled", () -> "true");
        // Use W3C trace context propagation (default for OTel)
        registry.add("management.tracing.propagation.type", () -> "W3C");
    }

    @BeforeEach
    void setUp() {
        testConsumer.clearMessages();
        testConsumer.subscribe();
    }

    @Test
    public void shouldCreateProducerSpanInJaeger() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();

        Eventually.eventually(() -> assertProducerSpanInJaeger());
    }

    @Test
    public void shouldInjectW3CTraceHeadersIntoMessage() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Wait for message to be consumed and verify trace headers
        Eventually.eventually(() -> {
            assertThat(testConsumer.getReceivedMessages()).as("Should have received a message").isNotEmpty();

            var message = testConsumer.getReceivedMessages().get(0);
            var headers = message.getHeaders();

            logger.info("Message headers: {}", headers);

            // Verify W3C trace context headers are present
            assertThat(headers.keySet())
                    .as("Message should contain W3C trace context headers")
                    .anyMatch(key -> key.equalsIgnoreCase("traceparent"));
        });
    }

    private void assertProducerSpanInJaeger() {
        String jaegerQueryUrl = String.format("http://%s:%s", jaeger.getHost(), jaeger.getMappedPort(16686));
        JaegerSpanVerifier verifier = new JaegerSpanVerifier(jaegerQueryUrl, "tracing-test", restTemplate);

        List<List<JaegerSpan>> traces = verifier.getTraces();
        logger.debug("Found {} traces", traces.size());

        assertThat(traces).as("Expected at least one trace in Jaeger").isNotEmpty();

        List<JaegerSpan> trace = traces.get(0);
        logger.debug("Trace has {} spans: {}", trace.size(), trace);

        String expectedSpanName = TramObservationDocumentation.PRODUCER.getName();
        JaegerSpan producerSpan = verifier.findSpanByName(trace, expectedSpanName);

        assertThat(producerSpan).as("Producer span should exist").isNotNull();
        assertThat(producerSpan.getOperationName()).isEqualTo(expectedSpanName);
        assertThat(producerSpan.hasTag("messaging.destination", TestController.TEST_CHANNEL))
                .as("Producer span should have destination tag").isTrue();
    }
}
