package io.eventuate.tram.spring.micrometer.tracing.test;

import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.eventuate.tram.spring.micrometer.tracing.TramObservationDocumentation;
import io.eventuate.tram.spring.micrometer.tracing.consumer.ObservationMessageConsumerDecorator;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ConsumerTracingIntegrationTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "management.tracing.enabled=true",
                "management.tracing.propagation.type=B3"
        })
@Testcontainers
public class ConsumerTracingIntegrationTest {

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
    private ApplicationContext applicationContext;

    @Autowired
    private TestConsumer testConsumer;

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
    public void shouldCreateConsumerSpanAsChildOfProducerSpan() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());

        // Wait for message to be consumed
        Eventually.eventually(() -> {
            assertFalse(testConsumer.getReceivedMessages().isEmpty(), "Should have received at least one message");
        });

        // Verify spans in Zipkin
        Eventually.eventually(() -> assertProducerAndConsumerSpansInZipkin());
    }

    private void assertProducerAndConsumerSpansInZipkin() {
        String zipkinBaseUrl = zipkinEndpoint.replace("/api/v2/spans", "/");
        ZipkinSpanVerifier verifier = new ZipkinSpanVerifier(zipkinBaseUrl, restTemplate);

        List<List<ZipkinSpan>> traces = verifier.getTraces();
        logger.debug("Found {} traces", traces.size());

        assertFalse(traces.isEmpty(), "Expected at least one trace in Zipkin");

        List<ZipkinSpan> trace = traces.get(0);
        logger.debug("Trace has {} spans: {}", trace.size(), trace);

        // Find producer span
        String producerSpanName = TramObservationDocumentation.PRODUCER.getName();
        ZipkinSpan producerSpan = verifier.findSpanByName(trace, producerSpanName);
        assertNotNull(producerSpan, "Producer span should exist");

        // Find consumer span
        String consumerSpanName = TramObservationDocumentation.CONSUMER.getName();
        ZipkinSpan consumerSpan = verifier.findSpanByName(trace, consumerSpanName);
        assertNotNull(consumerSpan, "Consumer span should exist");

        // Verify consumer span is child of producer span (same traceId)
        assertEquals(producerSpan.getTraceId(), consumerSpan.getTraceId(),
                "Consumer and producer should be in the same trace");

        // Verify consumer span has correct tags
        assertTrue(consumerSpan.hasTag("messaging.destination", TestController.TEST_CHANNEL),
                "Consumer span should have destination tag");
        assertTrue(consumerSpan.hasTag("messaging.subscriber.id", TestConsumer.SUBSCRIBER_ID),
                "Consumer span should have subscriber.id tag");
    }
}
