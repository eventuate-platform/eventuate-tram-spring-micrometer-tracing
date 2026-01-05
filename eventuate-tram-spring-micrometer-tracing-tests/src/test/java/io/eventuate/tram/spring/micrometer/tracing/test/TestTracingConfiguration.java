package io.eventuate.tram.spring.micrometer.tracing.test;

import brave.handler.SpanHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.urlconnection.URLConnectionSender;

@Configuration
public class TestTracingConfiguration {

    @Bean
    public SpanHandler zipkinSpanHandler(@Value("${management.zipkin.tracing.endpoint}") String endpoint) {
        return AsyncZipkinSpanHandler.create(URLConnectionSender.create(endpoint));
    }
}
