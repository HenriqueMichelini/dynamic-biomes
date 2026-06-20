---
id: CROP-PERF-0005
type: application
bounded_context: crops
capability: performance
layer: application
status: Ready
expected_commit: "feat(crops): add crop performance application service"
---

# Goal Card: Add Crop Performance Application Service

## Goal

Create orchestration that resolves environment, loads crop profile, and returns crop performance.

## Read First

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/implementation/0002-define-crop-owned-crop-environmental-state-composition.goal.md`
- `docs/implementation/0003-add-pure-crop-performance-domain-model.goal.md`
- `docs/implementation/0004-add-crop-performance-profile-provider-and-yaml-adapter.goal.md`
- Existing application services under `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops`
- Matching application tests under `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops`

## Current-State Problem

Growth and yield services independently resolve biome and season and have no shared crop performance result.

## Target-State Outcome

`CropPerformanceService` composes v1 `CropEnvironmentalState` from published upstream contracts and delegates performance calculation to domain.

Required domain decisions:

- V1 uses biome humidity and temperature, optionally modified by season multiplicatively.
- V1 soil fertility comes only from `BiomeProfile.Fertility`.
- Do not apply season or weather factors to soil fertility in v1.
- Wind speed, rain strength, and solar incidence default neutral until upstream contracts exist.
- Use `finalVariable = clamp01(biomeVariable * seasonVariableFactor)` only for season-modified variables.
- `CropPerformanceService` converts missing crop profile / unsupported crop-performance profile into neutral `CropPerformanceResult`.
- `CropPerformanceService` must not convert `UnsupportedBiomeException` into neutral performance.
- Unsupported biome remains an unsupported upstream/environment condition so growth/yield consumers preserve vanilla behavior.
- Malformed config, invalid normalized values, duplicate keys, I/O failures, and programming errors still propagate.

## Files Or Areas Likely Affected

- `java/app/src/main/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`
- `java/app/src/test/java/io/github/henriquemichelini/dynamicbiomes/crops/performance/application`
- Existing published upstream domain contracts under `biome` and `seasons` for read-only consumption.

## OOP / DDD / TDD Guardrails

- Keep `CropPerformanceService` as application orchestration: resolve upstream context, compose `CropEnvironmentalState`, load the crop profile, and delegate scoring to domain.
- Do not put crop scoring rules, profile matching rules, YAML parsing, Bukkit/Paper translation, or persistence in the application service.
- Consume only published upstream domain contracts from `biome` and `seasons`; never import upstream infrastructure.
- Use explicit port dependencies and in-memory fakes/stubs in tests rather than framework mocks or file-backed configuration.
- Write the smallest application tests first for each orchestration path, confirm expected failure when practical, then implement.

## Implementation Boundaries

- Application orchestration only.
- No Bukkit listeners.
- No YAML parsing or file I/O.
- No command output or `/dynamicbiomes inspect` quality output.
- No runtime registration.
- Do not catch or neutralize `UnsupportedBiomeException`.
- Do not swallow malformed config, invalid values, duplicate keys, I/O failures, or programming errors.

## Test/Verification Expectations

- Follow TDD with application tests using in-memory fakes/stubs.
- Use stubs for biome resolver, current season query, season profile provider, and crop performance profile provider.
- Cover multiplicative humidity and temperature season factors.
- Cover biome-only soil fertility.
- Cover neutral unavailable variables.
- Cover missing crop profile converted to neutral result.
- Cover unsupported biome propagation or unsupported-environment result separately from missing crop profile neutralization.
- Cover real failure propagation.
- Run focused application tests, then `cd java && ./gradlew test build --no-daemon`.
- Run `git diff --check`.

## Dependencies

- Card 2: Define Crop-Owned CropEnvironmentalState Composition.
- Card 3: Add Pure Crop Performance Domain Model.
- Card 4: Add Crop Performance Profile Provider And YAML Adapter.

## Risks Or Migration Notes

- Missing crop profile / unsupported crop-performance profile preserves existing DynamicBiomes behavior through neutral factors, not vanilla fallback.
- `UnsupportedBiomeException` remains an upstream/environment unsupported condition so growth/yield fallback semantics can preserve vanilla behavior.

## Acceptance Behavior

1. Given supported biome, season, and crop profile data, when performance is requested, then `CropPerformanceService` returns the domain-calculated performance result.
2. Given a missing crop profile or unsupported crop-performance profile, when performance is requested, then the service returns neutral `CropPerformanceResult`.
3. Given an `UnsupportedBiomeException`, when performance is requested, then the service does not convert it to neutral performance.
4. Given malformed config, invalid normalized values, duplicate keys, I/O failures, or programming errors, when they occur, then they propagate.

## Stop Conditions

Stop and report if service implementation requires Bukkit/Paper APIs, YAML parsing, command changes, runtime wiring, or changes to growth/yield consumers in the same card.
