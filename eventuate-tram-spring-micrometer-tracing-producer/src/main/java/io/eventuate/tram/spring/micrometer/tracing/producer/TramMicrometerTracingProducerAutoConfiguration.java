package io.eventuate.tram.spring.micrometer.tracing.producer;

import io.eventuate.tram.messaging.common.MessageInterceptor;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

@AutoConfiguration
@ConditionalOnClass({ObservationRegistry.class, MessageInterceptor.class})
public class TramMicrometerTracingProducerAutoConfiguration {

    @Bean
    @Lazy
    @ConditionalOnBean(ObservationHelper.class)
    public ObservationMessageProducerInterceptor observationMessageProducerInterceptor(
            @Lazy ObservationHelper observationHelper) {
        return new ObservationMessageProducerInterceptor(observationHelper);
    }
}
