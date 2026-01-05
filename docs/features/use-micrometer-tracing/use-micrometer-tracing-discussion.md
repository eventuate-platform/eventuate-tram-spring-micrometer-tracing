# Use Micrometer Tracing - Discussion

## Overview

This document captures the discussion and decisions for implementing distributed tracing for the Eventuate Framework using Micrometer Tracing, as a replacement for the deprecated Spring Cloud Sleuth integration.

## Reference Project Analysis

The existing `eventuate-tram-spring-cloud-sleuth` project provides:
- **9 modules** with separation between synchronous and reactive implementations
- **Instrumentation for**: Message producers, message consumers, duplicate detection
- **Patterns used**: Interceptors (sync), AspectJ aspects, Reactor operators (reactive)
- **Trace propagation**: Brave/B3 headers via message headers
- **Auto-configuration**: Conditional beans based on classpath detection

## Questions and Answers

### Q1: Tracer Bridge Selection

**Question:** Micrometer Tracing supports two underlying tracer implementations:
- A. Brave (OpenZipkin) - B3 propagation, direct migration from Sleuth
- B. OpenTelemetry - W3C Trace Context, CNCF standard
- C. Support both - Design abstractions for either bridge

**Answer:** C - Support both tracer bridges. Design abstractions that work with either Brave or OpenTelemetry, letting users choose based on their infrastructure.

---

### Q2: Instrumentation Scope

**Question:** The existing Sleuth integration instruments:
1. Synchronous message producers
2. Synchronous message consumers
3. Reactive message producers
4. Reactive message consumers
5. SQL-based duplicate message detection

Options:
- A. Match the existing scope (all 5 points)
- B. Synchronous only first (items 1, 2, 5)
- C. Expand scope (add Sagas, domain events, etc.)

**Answer:** A - Match the existing scope. Implement all 5 instrumentation points to provide feature parity with the Sleuth integration.

---

### Q3: Module Structure

**Question:** The existing Sleuth project has 9 separate modules. Options:
- A. Mirror the existing structure (9 modules)
- B. Consolidate into fewer modules
- C. Separate by tracer bridge (brave-*, otel-*)

**Answer:** A - Mirror the existing structure for consistency with other Eventuate projects.

**Derived conclusion on tracer separation:** Separate tracer-specific modules are NOT required. Micrometer Tracing is a facade (like SLF4J for logging) - our code uses the Micrometer Tracing APIs, and users choose the underlying implementation (Brave or OpenTelemetry) by adding the appropriate bridge dependency at runtime. This is the standard Micrometer Tracing pattern.

---

### Q4: Propagation Format

**Question:** For trace context propagation in messages:
- A. Use Micrometer defaults (Brave=B3, OTel=W3C). Users configure if needed.
- B. Enforce B3 for backwards compatibility
- C. Provide explicit configuration with B3 as default

**Answer:** A - Use Micrometer defaults. Let each bridge use its native propagation format. Users can configure propagation through standard Micrometer/Spring Boot properties if compatibility is needed.

---

### Q5: Eventuate Tram Version

**Question:** Which version of Eventuate Tram should this project target?
- A. Latest stable release compatible with Spring Boot 3.x
- B. Specific version
- C. Track development (SNAPSHOT)

**Answer:** Eventuate Tram version `0.37.0.BUILD-SNAPSHOT` (development version compatible with Spring Boot 3.x).

---

### Q6: Testing Strategy

**Question:** Testing approach for both tracer bridges:
- A. Mirror existing (Zipkin + Brave only)
- B. Test both bridges (Brave+Zipkin and OpenTelemetry+Jaeger)
- C. Minimal testing (unit tests with mocks)

**Answer:** B - Test both bridges. Create separate test modules or configurations to verify both Brave+Zipkin and OpenTelemetry+Jaeger work correctly.

---

### Q7: Micrometer API Approach

**Question:** Micrometer provides two API levels:
- A. Direct Tracing API (`io.micrometer.tracing.Tracer`) - closest to existing Sleuth approach
- B. Observation API (`io.micrometer.observation.Observation`) - higher-level, provides metrics + tracing
- C. Hybrid - Observation where it fits, direct Tracing for complex scenarios

**Answer:** B - Use the Observation API. This is the modern, recommended approach by Micrometer that provides both metrics and tracing from a single instrumentation point.

---

### Q8: Artifact Naming

**Question:** Naming convention for artifacts:
- A. `eventuate-tram-spring-micrometer-tracing-*` (mirrors existing pattern)
- B. `eventuate-tram-micrometer-*` (shorter)
- C. `eventuate-tram-tracing-*` (generic/future-proof)

**Answer:** A - Use `eventuate-tram-spring-micrometer-tracing-*` to mirror the existing Sleuth project naming convention.

Resulting module names:
- `eventuate-tram-spring-micrometer-tracing-common`
- `eventuate-tram-spring-micrometer-tracing-producer`
- `eventuate-tram-spring-micrometer-tracing-consumer`
- `eventuate-tram-spring-micrometer-tracing-reactive-common`
- `eventuate-tram-spring-micrometer-tracing-reactive-producer`
- `eventuate-tram-spring-micrometer-tracing-reactive-consumer`
- `eventuate-tram-spring-micrometer-tracing-starter`
- `eventuate-tram-spring-micrometer-tracing-tests`
- `eventuate-tram-spring-micrometer-tracing-reactive-tests`

---

### Q9: Project Classification

**Question:** How should this project be classified?
- A. User-facing feature
- B. Architecture POC
- C. Platform/infrastructure capability
- D. Educational/example repository

**Answer:** C - Platform/infrastructure capability

**Rationale:** This provides cross-cutting tracing infrastructure for the Eventuate platform. It's a library consumed by other Eventuate-based applications, follows proven patterns from the existing Sleuth integration, and is not a user-facing feature, POC, or example application.

---

## Summary of Decisions

| Decision | Choice |
|----------|--------|
| Tracer bridges | Support both Brave and OpenTelemetry |
| Instrumentation scope | Match existing (5 points: sync/reactive producers, sync/reactive consumers, duplicate detection) |
| Module structure | Mirror existing 9-module layout |
| Propagation format | Use Micrometer defaults (configurable by users) |
| Eventuate Tram version | 0.37.0.BUILD-SNAPSHOT |
| Testing strategy | Test both bridges (Brave+Zipkin, OTel+Jaeger) |
| API approach | Observation API |
| Artifact naming | `eventuate-tram-spring-micrometer-tracing-*` |
| Classification | Platform/infrastructure capability |

