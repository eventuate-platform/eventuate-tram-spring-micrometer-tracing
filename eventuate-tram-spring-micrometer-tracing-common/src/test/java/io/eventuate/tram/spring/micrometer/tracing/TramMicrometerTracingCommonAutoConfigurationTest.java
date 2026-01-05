package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class TramMicrometerTracingCommonAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TramMicrometerTracingCommonAutoConfiguration.class));

    @Test
    void shouldCreateObservationHelperBeanWhenDependenciesPresent() {
        contextRunner
                .withUserConfiguration(TracingConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(ObservationHelper.class);
                });
    }

    @Test
    void shouldNotCreateObservationHelperBeanWithoutTracer() {
        contextRunner
                .withUserConfiguration(NoTracerConfiguration.class)
                .run(context -> {
                    assertThat(context).doesNotHaveBean(ObservationHelper.class);
                });
    }

    @Configuration
    static class TracingConfiguration {
        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }

        @Bean
        Tracer tracer() {
            return mock(Tracer.class);
        }

        @Bean
        Propagator propagator() {
            return mock(Propagator.class);
        }
    }

    @Configuration
    static class NoTracerConfiguration {
        @Bean
        ObservationRegistry observationRegistry() {
            return ObservationRegistry.create();
        }
    }
}
