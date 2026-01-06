package io.eventuate.tram.spring.micrometer.tracing;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration(afterName = {
        "org.springframework.boot.actuate.autoconfigure.tracing.BraveAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration"
})
@ConditionalOnClass(ObservationRegistry.class)
public class TramMicrometerTracingCommonAutoConfiguration {

    @Bean
    @Lazy
    @ConditionalOnBean({Tracer.class, Propagator.class})
    public ObservationHelper observationHelper(@Lazy ObservationRegistry observationRegistry,
                                               Tracer tracer,
                                               Propagator propagator) {
        return new ObservationHelper(observationRegistry, tracer, propagator);
    }
}
