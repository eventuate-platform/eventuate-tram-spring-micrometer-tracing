package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(ObservationRegistry.class)
public class TramMicrometerTracingCommonAutoConfiguration {

    @Bean
    @ConditionalOnBean({Tracer.class, Propagator.class})
    public ObservationHelper observationHelper(ObservationRegistry observationRegistry,
                                               Tracer tracer,
                                               Propagator propagator) {
        return new ObservationHelper(observationRegistry, tracer, propagator);
    }
}
