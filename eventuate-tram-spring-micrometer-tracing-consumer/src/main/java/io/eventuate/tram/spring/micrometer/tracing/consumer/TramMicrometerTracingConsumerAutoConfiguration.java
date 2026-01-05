package io.eventuate.tram.spring.micrometer.tracing.consumer;

import io.eventuate.tram.consumer.common.MessageHandlerDecorator;
import io.eventuate.tram.spring.micrometer.tracing.ObservationHelper;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass({ObservationRegistry.class, MessageHandlerDecorator.class})
public class TramMicrometerTracingConsumerAutoConfiguration {

    @Bean
    @ConditionalOnBean(ObservationHelper.class)
    public ObservationMessageConsumerDecorator observationMessageConsumerDecorator(
            ObservationHelper observationHelper) {
        return new ObservationMessageConsumerDecorator(observationHelper);
    }
}
