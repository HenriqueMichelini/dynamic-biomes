---
id: CROP-PERF-0005B
type: application
bounded_context: crops
capability: performance
layer: application
status: Ready
expected_commit: "feat(crops): add crop performance application service"
---

# Goal Card: Add Crop Performance Application Service

## Goal

Implement `CropPerformanceService` orchestration for crop profile lookup and domain performance calculation.

## Short Description

Add an application service that composes environmental state through the Card 5A collaborator, loads the crop performance profile, delegates scoring to domain, converts missing/unsupported crop profiles to neutral performance, and propagates real upstream/configuration failures.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0003c-add-crop-performance-scoring-domain.goal.md`
- `docs/implementation/0004-add-crop-performance-profile-provider-and-yaml-adapter.goal.md`
- `docs/implementation/0005a-compose-crop-environmental-state-application.goal.md`
- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`
- Matching application tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`

## Affected Layer(s)

- Application: `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`
- Matching application tests only.

## Acceptance Criteria

1. Given supported environmental state and crop profile data, when performance is requested, then `CropPerformanceService` returns the domain-calculated `CropPerformanceResult`.
2. Given a missing crop profile or unsupported crop-performance profile, when performance is requested, then the service returns neutral `CropPerformanceResult`.
3. Given `UnsupportedBiomeException`, malformed config, invalid normalized values, duplicate keys, I/O failures, or programming errors, when they occur, then the service does not swallow them.

## Expected Tests To Write First

- Application test using stubs for the environmental-state composer and crop performance profile provider for the supported happy path.
- Application test for missing/unsupported crop profile returning neutral performance.
- Application test for `UnsupportedBiomeException` propagation.
- Application test for real failure propagation from collaborators.

## OOP / DDD / TDD Guardrails

- Keep `CropPerformanceService` as orchestration only: ask for environmental state, load the crop profile, and delegate scoring to domain.
- Do not put scoring rules, environmental composition formulas, YAML parsing, Bukkit/Paper translation, or persistence in this service.
- Use explicit collaborator dependencies and in-memory fakes/stubs in tests.
- Preserve unsupported biome semantics so growth/yield consumers can preserve vanilla behavior.
- Write the smallest failing service test first, then implement only the orchestration/fallback behavior required by that test.

## Dependencies

- Card 3C: Add Crop Performance Scoring Domain.
- Card 4: Add Crop Performance Profile Provider And YAML Adapter.
- Card 5A: Compose Crop Environmental State.

## Out of Scope

- Environmental state composition formulas.
- YAML adapter implementation.
- Runtime registration.
- Growth/yield consumers.
- Command output or quality concepts.

## Stop Conditions

Stop and report if service implementation requires Bukkit/Paper APIs, YAML parsing, command changes, runtime wiring, or changes to growth/yield consumers in the same card.
