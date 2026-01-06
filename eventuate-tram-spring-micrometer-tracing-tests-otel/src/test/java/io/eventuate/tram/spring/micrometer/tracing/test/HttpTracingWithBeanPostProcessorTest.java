package io.eventuate.tram.spring.micrometer.tracing.test;

import io.eventuate.tram.consumer.common.MessageHandlerDecorator;
import io.eventuate.tram.spring.inmemory.TramInMemoryConfiguration;
import io.eventuate.util.test.async.Eventually;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
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

/**
 * Test that verifies HTTP tracing continues to work when a BeanPostProcessor
 * depends on beans from the tracing library.
 *
 * This reproduces a bug where the ObservationRegistry is created too early
 * (before tracing handlers are attached) due to the dependency chain:
 * BeanPostProcessor -> MessageHandlerDecorator -> ObservationHelper -> ObservationRegistry
 */
@SpringBootTest(classes = HttpTracingWithBeanPostProcessorTest.TestConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class HttpTracingWithBeanPostProcessorTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Configuration
    @SpringBootApplication
    @Import({TramInMemoryConfiguration.class, TestConsumer.class})
    static class TestConfiguration {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }

        /**
         * A BeanPostProcessor that depends on MessageHandlerDecorator beans.
         * This simulates the real-world scenario where eventuateCommandHandlerBeanPostProcessor
         * depends on decorators, causing early initialization of the ObservationRegistry.
         */
        @Bean
        public static BeanPostProcessor decoratorDependentBeanPostProcessor(
                List<MessageHandlerDecorator> decorators) {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                    // Just having this dependency is enough to reproduce the issue
                    return bean;
                }
            };
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private RestTemplate restTemplate;

    @Container
    static final GenericContainer<?> jaeger = new GenericContainer<>(DockerImageName.parse("jaegertracing/all-in-one:1.51"))
            .withExposedPorts(16686, 4318)
            .withEnv("COLLECTOR_OTLP_ENABLED", "true");

    @DynamicPropertySource
    static void jaegerProperties(DynamicPropertyRegistry registry) {
        registry.add("management.otlp.tracing.endpoint",
                () -> String.format("http://%s:%s/v1/traces", jaeger.getHost(), jaeger.getMappedPort(4318)));
        registry.add("management.tracing.enabled", () -> "true");
        registry.add("management.tracing.propagation.type", () -> "W3C");
        registry.add("spring.application.name", () -> "http-tracing-test");
    }

    @Test
    public void shouldCreateHttpServerSpanInJaeger() {
        String id = Long.toString(System.currentTimeMillis());
        String url = String.format("http://localhost:%s/send/%s", port, id);

        ResponseEntity<String> result = restTemplate.postForEntity(url,
                new TestMessage("test content " + id), String.class);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);

        Eventually.eventually(() -> assertHttpServerSpanInJaeger());
    }

    private void assertHttpServerSpanInJaeger() {
        String jaegerQueryUrl = String.format("http://%s:%s", jaeger.getHost(), jaeger.getMappedPort(16686));
        JaegerSpanVerifier verifier = new JaegerSpanVerifier(jaegerQueryUrl, "http-tracing-test", restTemplate);

        List<List<JaegerSpan>> traces = verifier.getTraces();
        logger.info("Found {} traces", traces.size());

        assertThat(traces).as("Expected at least one trace in Jaeger").isNotEmpty();

        List<JaegerSpan> trace = traces.get(0);
        logger.info("Trace has {} spans: {}", trace.size(), trace);

        // Look for HTTP server span - Spring Boot creates spans with operation name like "POST /send/{id}"
        JaegerSpan httpServerSpan = trace.stream()
                .filter(span -> span.getOperationName().contains("/send/") ||
                               span.getOperationName().startsWith("POST") ||
                               span.getOperationName().startsWith("HTTP"))
                .findFirst()
                .orElse(null);

        assertThat(httpServerSpan)
                .as("HTTP server span should exist - if missing, ObservationRegistry was created before tracing handlers were attached")
                .isNotNull();
    }
}
