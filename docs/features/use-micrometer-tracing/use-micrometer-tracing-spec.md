# Eventuate Tram Spring Micrometer Tracing - Platform Capability Specification

## 1. Purpose and Context

### 1.1 Background

The Eventuate Framework provides messaging and event-driven architecture capabilities for microservices. Distributed tracing is essential for understanding message flows across service boundaries, debugging production issues, and monitoring system health.

The existing `eventuate-tram-spring-cloud-sleuth` project provides distributed tracing integration, but Spring Cloud Sleuth has been deprecated and replaced by Micrometer Tracing. This capability provides a modern replacement using Micrometer Tracing, which is the recommended approach for Spring Boot 3.x applications.

### 1.2 Purpose

Provide distributed tracing infrastructure for Eventuate Tram messaging that:

1. Integrates seamlessly with Spring Boot 3.x and Micrometer Tracing
2. Supports both Brave (OpenZipkin) and OpenTelemetry tracer bridges
3. Provides feature parity with the existing Spring Cloud Sleuth integration
4. Uses the modern Micrometer Observation API for combined metrics and tracing

### 1.3 Scope

**In Scope:**
- Trace propagation for synchronous message producers and consumers
- Trace propagation for reactive message producers and consumers
- Instrumentation of SQL-based duplicate message detection
- Spring Boot auto-configuration
- Support for Brave and OpenTelemetry bridges

**Out of Scope:**
- Saga instrumentation (future enhancement)
- Domain event aggregate instrumentation (future enhancement)
- Command handler instrumentation (future enhancement)
- Custom tracing backend implementations

---

## 2. Consumers

### 2.1 Primary Consumers

| Consumer | Description | Integration Method |
|----------|-------------|-------------------|
| Eventuate-based microservices | Applications using Eventuate Tram for messaging | Maven/Gradle dependency on starter module |
| DevOps/SRE teams | Teams monitoring distributed systems | View traces in Zipkin, Jaeger, or other compatible backends |
| Developers | Engineers debugging message flows | Correlation of logs with trace IDs |

### 2.2 Consumer Requirements

- **Zero-code integration**: Adding the starter dependency should enable tracing automatically
- **Bridge flexibility**: Consumers choose Brave or OpenTelemetry based on their infrastructure
- **Backend agnostic**: Works with any tracing backend supported by the chosen bridge

---

## 3. Capabilities and Behaviors

### 3.1 Core Capabilities

#### 3.1.1 Message Producer Tracing (Synchronous)

| Aspect | Specification |
|--------|---------------|
| Trigger | When `MessageProducer.send()` is invoked |
| Observation Name | `eventuate.tram.producer` |
| Context Keys | `destination`, `messageId`, `messageHeaders` |
| Propagation | Inject trace context into message headers |
| Lifecycle | Start observation before send, stop after send completes |

#### 3.1.2 Message Consumer Tracing (Synchronous)

| Aspect | Specification |
|--------|---------------|
| Trigger | When a message handler processes a received message |
| Observation Name | `eventuate.tram.consumer` |
| Context Keys | `destination`, `subscriberId`, `messageId`, `messageHeaders` |
| Propagation | Extract trace context from message headers (creates child span) |
| Lifecycle | Start observation before handler, stop after handler completes (success or error) |

#### 3.1.3 Message Producer Tracing (Reactive)

| Aspect | Specification |
|--------|---------------|
| Trigger | When reactive `MessageProducer.send()` returns a Mono |
| Observation Name | `eventuate.tram.reactive.producer` |
| Context Keys | `destination`, `messageId`, `messageHeaders` |
| Propagation | Inject trace context into message headers |
| Lifecycle | Wrap Mono with observation operators; complete on terminal signal |

#### 3.1.4 Message Consumer Tracing (Reactive)

| Aspect | Specification |
|--------|---------------|
| Trigger | When reactive message handler processes a message |
| Observation Name | `eventuate.tram.reactive.consumer` |
| Context Keys | `destination`, `subscriberId`, `messageId`, `messageHeaders` |
| Propagation | Extract trace context from message headers |
| Lifecycle | Wrap handler Mono with observation operators |

#### 3.1.5 Duplicate Message Detection Tracing

| Aspect | Specification |
|--------|---------------|
| Trigger | When `SqlTableBasedDuplicateMessageDetector` checks for duplicates |
| Observation Name | `eventuate.tram.deduplication` |
| Context Keys | `consumerId`, `messageId` |
| Lifecycle | Wrap deduplication check with observation |

### 3.2 Trace Context Propagation

| Bridge | Default Propagation Format | Header Names |
|--------|---------------------------|--------------|
| Brave | B3 | `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-ParentSpanId`, `X-B3-Sampled` |
| OpenTelemetry | W3C Trace Context | `traceparent`, `tracestate` |

Users can configure alternative propagation formats through standard Spring Boot/Micrometer properties.

### 3.3 Observation Context

Each observation provides the following low-cardinality keys for metrics:

| Key | Description | Example |
|-----|-------------|---------|
| `messaging.operation` | Type of operation | `publish`, `receive`, `deduplicate` |
| `messaging.destination` | Message destination/channel | `order-events` |
| `messaging.system` | Messaging system identifier | `eventuate-tram` |

High-cardinality keys (for tracing only):

| Key | Description |
|-----|-------------|
| `messaging.message_id` | Unique message identifier |
| `messaging.subscriber_id` | Consumer subscriber ID |

---

## 4. High-Level APIs and Integration Points

### 4.1 Module Structure

```
eventuate-tram-spring-micrometer-tracing/
├── eventuate-tram-spring-micrometer-tracing-common/
│   └── Core observation conventions and utilities
├── eventuate-tram-spring-micrometer-tracing-producer/
│   └── Synchronous producer instrumentation
├── eventuate-tram-spring-micrometer-tracing-consumer/
│   └── Synchronous consumer instrumentation + deduplication
├── eventuate-tram-spring-micrometer-tracing-reactive-common/
│   └── Reactive observation operators
├── eventuate-tram-spring-micrometer-tracing-reactive-producer/
│   └── Reactive producer instrumentation
├── eventuate-tram-spring-micrometer-tracing-reactive-consumer/
│   └── Reactive consumer instrumentation
├── eventuate-tram-spring-micrometer-tracing-starter/
│   └── Spring Boot starter (aggregates all modules)
├── eventuate-tram-spring-micrometer-tracing-tests/
│   └── Integration tests (sync, both bridges)
└── eventuate-tram-spring-micrometer-tracing-reactive-tests/
    └── Integration tests (reactive, both bridges)
```

### 4.2 Extension Points

#### 4.2.1 MessageInterceptor (Synchronous Producer)

```java
public interface MessageInterceptor {
    void preSend(Message message);
    void postSend(Message message, Exception e);
}
```

The tracing module provides `ObservationMessageProducerInterceptor` implementing this interface.

#### 4.2.2 MessageHandlerDecorator (Synchronous Consumer)

```java
public interface MessageHandlerDecorator {
    void accept(SubscriberIdAndMessage subscriberIdAndMessage,
                MessageHandlerDecoratorChain chain);
}
```

The tracing module provides `ObservationMessageConsumerDecorator` implementing this interface.

#### 4.2.3 ReactiveMessageHandlerDecorator (Reactive Consumer)

```java
public interface ReactiveMessageHandlerDecorator {
    Mono<Void> apply(SubscriberIdAndMessage subscriberIdAndMessage,
                     ReactiveMessageHandler handler);
}
```

### 4.3 Spring Boot Auto-Configuration

| Configuration Class | Condition | Provides |
|--------------------|-----------|----------|
| `TramMicrometerTracingCommonAutoConfiguration` | `ObservationRegistry` on classpath | Common beans, observation conventions |
| `TramMicrometerTracingProducerAutoConfiguration` | Producer classes on classpath | Producer interceptor |
| `TramMicrometerTracingConsumerAutoConfiguration` | Consumer classes on classpath | Consumer decorator, deduplication aspect |
| `TramMicrometerTracingReactiveProducerAutoConfiguration` | Reactive producer on classpath | Reactive producer aspect |
| `TramMicrometerTracingReactiveConsumerAutoConfiguration` | Reactive consumer on classpath | Reactive consumer decorator |

### 4.4 Dependency Coordinates

**Maven:**
```xml
<dependency>
    <groupId>io.eventuate.tram.core</groupId>
    <artifactId>eventuate-tram-spring-micrometer-tracing-starter</artifactId>
    <version>${eventuate.version}</version>
</dependency>

<!-- Choose one bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<!-- OR -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
```

---

## 5. Non-Functional Requirements

### 5.1 Performance

| Requirement | Target |
|-------------|--------|
| Overhead per message (tracing enabled) | < 1ms latency added |
| Memory overhead | < 1KB per active span |
| Sampling support | Honor configured sampling rate |

### 5.2 Reliability

| Requirement | Specification |
|-------------|---------------|
| Failure isolation | Tracing failures must not affect message processing |
| Graceful degradation | If ObservationRegistry unavailable, proceed without tracing |
| Error handling | Span errors must be recorded but not re-thrown |

### 5.3 Compatibility

| Requirement | Specification |
|-------------|---------------|
| Spring Boot | 3.4.7+ |
| Eventuate Tram | 0.37.0.BUILD-SNAPSHOT |
| Java | 17+ |
| Micrometer Tracing | Latest compatible with Spring Boot 3.4.x |

### 5.4 Observability

| Requirement | Specification |
|-------------|---------------|
| Metrics | Observation API provides automatic metrics (timer, counter) |
| Trace correlation | Trace IDs available in MDC for log correlation |
| Span naming | Consistent, descriptive span names per operation type |

---

## 6. Scenarios and Workflows

### 6.1 Primary End-to-End Scenario: Cross-Service Message Trace

**Scenario:** A message is produced by Service A, consumed by Service B, and the complete trace is visible in the tracing backend.

**Flow:**
1. Service A produces a message to channel `order-events`
2. Producer interceptor creates observation, injects trace headers
3. Message is persisted and published via Eventuate CDC
4. Service B receives the message
5. Consumer decorator extracts trace context, creates child observation
6. Message handler processes the message
7. Observation completes (success or error recorded)
8. Both spans are exported to tracing backend (Zipkin/Jaeger)
9. Complete trace is queryable showing Service A → Service B flow

**Verification:**
- Query tracing backend for trace by ID
- Verify parent-child relationship between producer and consumer spans
- Verify span tags contain expected metadata (destination, message ID, etc.)

### 6.2 Scenario: Reactive Message Flow

**Flow:**
1. Service A uses reactive `MessageProducer.send()` returning `Mono<Message>`
2. Reactive producer aspect wraps Mono with observation
3. On subscription, trace context is injected into message headers
4. Service B's reactive consumer receives message
5. Reactive consumer decorator wraps handler Mono with observation
6. Trace propagates through reactive chain
7. On terminal signal, observations complete

### 6.3 Scenario: Duplicate Message Detection Trace

**Flow:**
1. Consumer receives a message
2. Consumer decorator starts consumer observation
3. `SqlTableBasedDuplicateMessageDetector` checks for duplicates
4. Deduplication aspect creates child observation
5. Database query executes
6. Deduplication observation completes
7. Handler executes (if not duplicate)
8. Consumer observation completes

### 6.4 Scenario: Multi-Bridge Compatibility

**Flow (Brave):**
1. Application includes `micrometer-tracing-bridge-brave`
2. Brave's tracer is used automatically
3. B3 headers propagated in messages
4. Traces export to Zipkin

**Flow (OpenTelemetry):**
1. Application includes `micrometer-tracing-bridge-otel`
2. OpenTelemetry tracer is used automatically
3. W3C Trace Context headers propagated in messages
4. Traces export to Jaeger (or any OTLP-compatible backend)

### 6.5 Scenario: Error Handling

**Flow:**
1. Consumer receives message
2. Consumer observation starts
3. Handler throws exception
4. Observation records error (exception attached to span)
5. Observation completes with error status
6. Span exported with error flag and exception details

---

## 7. Constraints and Assumptions

### 7.1 Constraints

1. **Spring Boot 3.x only**: This library requires Spring Boot 3.x; Spring Boot 2.x users should use the existing Sleuth integration
2. **Bridge required**: Users must include exactly one tracing bridge (Brave or OpenTelemetry)
3. **Observation API**: Implementation uses Micrometer Observation API; direct Tracer API not exposed
4. **No backwards compatibility with Sleuth headers**: If migrating from Sleuth, users must coordinate header format or configure compatible propagation

### 7.2 Assumptions

1. Users have a functioning Eventuate Tram setup
2. Users have a tracing backend (Zipkin, Jaeger, etc.) or will configure one
3. The Eventuate Tram interceptor/decorator extension points are stable
4. Micrometer Tracing's Observation API is stable for the target Spring Boot version

---

## 8. Acceptance Criteria

### 8.1 Functional Acceptance Criteria

| ID | Criterion | Verification |
|----|-----------|--------------|
| F1 | Synchronous message producer creates observation with correct context | Integration test |
| F2 | Synchronous message consumer extracts trace and creates child observation | Integration test |
| F3 | Reactive message producer creates observation with correct context | Integration test |
| F4 | Reactive message consumer extracts trace and creates child observation | Integration test |
| F5 | Duplicate detection creates observation as child of consumer span | Integration test |
| F6 | Trace context propagates in message headers (Brave/B3) | Integration test with Zipkin |
| F7 | Trace context propagates in message headers (OTel/W3C) | Integration test with Jaeger |
| F8 | Complete trace visible in backend showing producer → consumer flow | Manual verification |

### 8.2 Non-Functional Acceptance Criteria

| ID | Criterion | Verification |
|----|-----------|--------------|
| NF1 | Tracing overhead < 1ms per message | Performance test |
| NF2 | Tracing failure does not prevent message processing | Fault injection test |
| NF3 | Auto-configuration activates only when tracing classes present | Unit test |
| NF4 | Works with Spring Boot 3.4.7 | CI build verification |

### 8.3 Documentation Acceptance Criteria

| ID | Criterion |
|----|-----------|
| D1 | README with quick start guide |
| D2 | Migration guide from Spring Cloud Sleuth integration |
| D3 | Configuration reference for common properties |

---

## Change History

### 2026-01-05: Initial specification

- Created Platform Capability Specification based on discussion outcomes
- Defined 9-module structure mirroring existing Sleuth integration
- Specified Observation API approach for instrumentation
- Documented support for both Brave and OpenTelemetry bridges
