package io.eventuate.tram.spring.micrometer.tracing.test;

import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.micrometer.tracing.TramObservationDocumentation;
import io.eventuate.util.test.async.Eventually;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
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
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ProducerTracingIntegrationTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class ProducerTracingIntegrationTest {

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

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${management.zipkin.tracing.endpoint}")
    private String zipkinEndpoint;

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Container
    static final GenericContainer<?> zipkin = new GenericContainer<>(DockerImageName.parse("openzipkin/zipkin:2.23"))
            .withExposedPorts(9411);

    @DynamicPropertySource
    static void zipkinProperties(DynamicPropertyRegistry registry) {
        registry.add("management.zipkin.tracing.endpoint",
                () -> String.format("http://%s:%s/api/v2/spans", zipkin.getHost(), zipkin.getFirstMappedPort()));
        // Override Spring Boot test defaults that disable tracing
        registry.add("management.tracing.enabled", () -> "true");
        registry.add("management.tracing.propagation.type", () -> "B3");
    }

    @BeforeEach
    void setUp() {
        testConsumer.clearMessages();
        testConsumer.subscribe();
    }

    @Test
    public void shouldCreateProducerSpanInZipkin() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());

        Eventually.eventually(() -> assertProducerSpanInZipkin());
    }

    @Test
    public void shouldInjectTraceHeadersIntoMessage() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        // Wait for message to be consumed and verify trace headers
        Eventually.eventually(() -> {
            assertFalse(testConsumer.getReceivedMessages().isEmpty(), "Should have received a message");

            var message = testConsumer.getReceivedMessages().get(0);
            var headers = message.getHeaders();

            logger.info("Message headers: {}", headers);

            // Verify B3 trace headers are present (either single header or multi-header format)
            assertThat(headers.keySet())
                    .as("Message should contain B3 trace headers")
                    .anyMatch(key -> key.equals("b3") || key.equals("X-B3-TraceId"));
        });
    }

    private void assertProducerSpanInZipkin() {
        String zipkinBaseUrl = zipkinEndpoint.replace("/api/v2/spans", "/");
        ZipkinSpanVerifier verifier = new ZipkinSpanVerifier(zipkinBaseUrl, restTemplate);

        List<List<ZipkinSpan>> traces = verifier.getTraces();
        logger.debug("Found {} traces", traces.size());

        assertFalse(traces.isEmpty(), "Expected at least one trace in Zipkin");

        List<ZipkinSpan> trace = traces.get(0);
        logger.debug("Trace has {} spans: {}", trace.size(), trace);

        String expectedSpanName = TramObservationDocumentation.PRODUCER.getName();
        ZipkinSpan producerSpan = verifier.findSpanByName(trace, expectedSpanName);

        assertNotNull(producerSpan, "Producer span should exist");
        assertEquals(expectedSpanName, producerSpan.getName());
        assertTrue(producerSpan.hasTag("messaging.destination", TestController.TEST_CHANNEL),
                "Producer span should have destination tag");
    }
}
